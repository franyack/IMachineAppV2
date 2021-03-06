package com.example.fran.imachineappv2.CIEngine.predictor;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

/**
 * Based on: https://github.com/amitshekhariitbhu/Android-TensorFlow-Lite-Example
 */

public class TensorFlowImageClassifier implements Classifier {

    private static final int MAX_RESULTS = 5;
    private static final float THRESHOLD = 0.05f;

    private Interpreter interpreter;
    private int inputSize;
    private List<String> labelList;

    /** An array to hold inference results, to be feed into Tensorflow Lite as outputs. */
    private float[][] labelProbArray = null;
    private float[][][][] embeddingArray = null;

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

        classifier.labelProbArray = new float[1][classifier.labelList.size()];
        classifier.embeddingArray = new float[1][1][1][1024];

        return classifier;
    }

    @Override
    public List<Recognition> recognizeImage(ByteBuffer byteBuffer) {
        float[][] result = new float[1][labelList.size()];
        interpreter.run(byteBuffer, result);
        return getSortedResult(result);
    }

    public void recognize(ByteBuffer byteBuffer, Map<Integer, Object> results) {
        // ----
        Object[] inputs = new Object[]{byteBuffer};
        Map<Integer, Object> outputs = new TreeMap<>();
        outputs.put(0, labelProbArray);
        outputs.put(1, embeddingArray);

        interpreter.runForMultipleInputsOutputs(inputs, outputs);
        // ----

        results.put(0, getSortedResult(labelProbArray));
        results.put(1, embeddingArray);
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

    private List<Recognition> getSortedResult(float[][] labelProbArray) {

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
            float confidence = labelProbArray[0][i];
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

    private List<Recognition> getSortedResult2(byte[][] labelProbArray) {

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