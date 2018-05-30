package com.example.fran.imachineappv2.CIEngine;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Based on: https://github.com/amitshekhariitbhu/Android-TensorFlow-Lite-Example
 */

public class TensorFlowImageClassifier implements Classifier {

    private static final int MAX_RESULTS = 5;
    private static final float THRESHOLD = 0.05f;

    private Interpreter interpreter;
    private int inputSize;
    private List<String> labelList;

    private TensorFlowImageClassifier() {

    }

    public static Classifier create(BufferedReader reader,
                                    FileInputStream inputStream,
                                    long startOffset,
                                    long declaredLength,
                                    int inputSize) throws IOException {

        TensorFlowImageClassifier classifier = new TensorFlowImageClassifier();
        classifier.interpreter = new Interpreter(classifier.loadModelFile(inputStream, startOffset,declaredLength));
        classifier.labelList = classifier.loadLabelList(reader);
        classifier.inputSize = inputSize;

        return classifier;
    }

    @Override
    public List<Recognition> recognizeImage(ByteBuffer byteBuffer) {
        byte[][] result = new byte[1][labelList.size()];
        interpreter.run(byteBuffer, result);
        return getSortedResult(result);
    }

    @Override
    public void close() {
        interpreter.close();
        interpreter = null;
    }

    private MappedByteBuffer loadModelFile(FileInputStream inputStream, long startOffset, long declaredLength) throws IOException {
        FileChannel fileChannel = inputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public static List<String> loadLabelList(BufferedReader reader) throws IOException {
        List<String> labelList = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }


//    @SuppressLint("DefaultLocale")
    private List<Recognition> getSortedResult(byte[][] labelProbArray) {

        PriorityQueue<Recognition> pq =
                new PriorityQueue<>(
                        MAX_RESULTS,
                        new Comparator<Recognition>() {
                            @Override
                            public int compare(Recognition lhs, Recognition rhs) {
                                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                            }
                        });

        for (int i = 0; i < labelList.size(); ++i) {
            float confidence = (labelProbArray[0][i] & 0xff) / 255.0f;
            if (confidence > THRESHOLD) {
                pq.add(new Recognition("" + i,
                        labelList.size() > i ? labelList.get(i) : "unknown",
                        confidence));
            }
        }

        final ArrayList<Recognition> recognitions = new ArrayList<>();
        int recognitionsSize = Math.min(pq.size(), MAX_RESULTS);
        for (int i = 0; i < recognitionsSize; ++i) {
            recognitions.add(pq.poll());
        }

        return recognitions;
    }

}
