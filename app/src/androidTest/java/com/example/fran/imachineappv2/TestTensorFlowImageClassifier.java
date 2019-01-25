package com.example.fran.imachineappv2;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.example.fran.imachineappv2.CIEngine.predictor.Classifier;
import com.example.fran.imachineappv2.CIEngine.predictor.TensorFlowImageClassifier;
import com.example.fran.imachineappv2.CIEngine.util.ImageUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@RunWith(AndroidJUnit4.class)
public class TestTensorFlowImageClassifier {
    // Paths
    private static final String MODEL_FILENAME = "mobilenet_v1_224.tflite";
    private static final String LABELS_FILENAME = "labels.txt";
    private static final String TEST_IMAGE_FILENAME = "test_image.jpg";

    private static final int INPUT_SIZE = 224;
    private static final int IMG_W = 224;
    private static final int IMG_H = 224;
    private static final int BATCH_SIZE = 1;
    private static final int PIXEL_SIZE = 3;
    private static final float IMAGE_MEAN = 128.f;
    private static final float IMAGE_STD = 128.f;

    private static final int EXPECTED_SIZE_EMBEDDING_VECTOR = 1024;

    private TensorFlowImageClassifier classifier;
    private ByteBuffer byteBuffer;

    @Before
    public void setUp() throws IOException {

        // Get target context to retrieve assets from main (not from androidTest)
        Context context = InstrumentationRegistry.getTargetContext();

        // Read files from assets folder
        AssetManager assetManager = context.getAssets();
        BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open(LABELS_FILENAME)));
        AssetFileDescriptor fileDescriptor = assetManager.openFd(MODEL_FILENAME);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        Bitmap image = BitmapFactory.decodeStream(assetManager.open(TEST_IMAGE_FILENAME));
        image = ImageUtils.resize(image, INPUT_SIZE, INPUT_SIZE);  // Rescale as needed for model

        // Create classifier to be tested
        classifier = (TensorFlowImageClassifier) TensorFlowImageClassifier.create(
                reader,
                inputStream,
                startOffset,
                declaredLength,
                INPUT_SIZE);

        // Convert to buffer of bytes
        byteBuffer = ImageUtils.convertBitmapToByteBuffer(
                image,BATCH_SIZE,IMG_W,IMG_H,PIXEL_SIZE,IMAGE_MEAN,IMAGE_STD);
    }

    @Test
    public void recognize(){

        try {
            setUp();

            Map<Integer, Object> outputs = new TreeMap<>();

            // Get output from classifier (expected: recognitions and embeddings)
            classifier.recognize(byteBuffer, outputs);

            // Expected map of two elements
            assertEquals(outputs.size(), 2);

            // Get recognitions
            List<Classifier.Recognition> recognitions = (List<Classifier.Recognition>) outputs.get(0);

            assertNotNull(recognitions);
            assertTrue(recognitions.size() > 0);
            // TODO: assert Samoyed in recognition labels?


            // Get embeddings
            float[][][][] embArr = (float[][][][]) outputs.get(1);

            assertNotNull(embArr);

            List<float[]> embList = Arrays.asList(embArr[0][0][0]);

            assertEquals(embList.size(), 1);  // just one image processed
            assertEquals(embList.get(0).length, EXPECTED_SIZE_EMBEDDING_VECTOR);


        } catch (IOException e) {
            e.printStackTrace();
        }



    }
}
