package com.example.fran.imachineappv2;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.example.fran.imachineappv2.CIEngine.clustering.MetricsReporter;
import com.example.fran.imachineappv2.CIEngine.imagenet.HierarchyLookup;
import com.example.fran.imachineappv2.CIEngine.imagenet.ImageNetUtils;
import com.example.fran.imachineappv2.CIEngine.imagenet.WNIDWords;
import com.example.fran.imachineappv2.CIEngine.predictor.Classifier;
import com.example.fran.imachineappv2.CIEngine.predictor.TensorFlowImageClassifier;

import org.ejml.data.DMatrixRMaj;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


@RunWith(AndroidJUnit4.class)
public class ExperimentRunner {

    // Paths
    private static final String MODEL_FILENAME = "mobilenet_v1_224.tflite";
    private static final String LABELS_FILENAME = "labels.txt";
    private static final String WORDS_PATH = "words.txt";
    private static final String HIERARCHY_PATH = "wordnet.is_a.txt";

    private Classifier classifier;
    private List<WNIDWords> wnidWordsList = new ArrayList<>();
    private List<HierarchyLookup> hierarchyLookupList = new ArrayList<>();
    private List<String> vImages = new ArrayList<>();
    private List<Integer> vClusters = new ArrayList<>();
    private static final String imagesDir = "/storage/7B5C-15FF/ImagenesEntrada";  // TODO: set this!
    private static final String PATH_TO_LOGS = "%t/imachineapp-metrics-%g.log";  // TODO: set this!
    private String[] imagesPath;


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

        // Create classifier to be tested
        classifier = TensorFlowImageClassifier.create(
                reader,
                inputStream,
                startOffset,
                declaredLength,
                TensorFlowImageClassifier.IMG_H);

        // Read WNID words list for classifier
        reader = new BufferedReader(new InputStreamReader(assetManager.open(WORDS_PATH)));
        wnidWordsList = ImageNetUtils.loadWNIDWords(reader);
        reader.close();

        // Read the hierarchy list that relate those WNID words as well
        reader = new BufferedReader(new InputStreamReader(assetManager.open(HIERARCHY_PATH)));
        hierarchyLookupList = ImageNetUtils.loadHierarchyLookup(reader);
        reader.close();

        File imagesDirFile = new File(imagesDir);

        imagesPath = new String[imagesDirFile.list().length];

        int i = 0;

        for (File imageFile : imagesDirFile.listFiles()){
            // TODO: allow tree structure of dirs
            imagesPath[i++] = imageFile.getAbsolutePath();

        }
    }

    @Test
    public void run(){

        DMatrixRMaj affinityMatrix = MainActivityModel.runProcessing(imagesPath,null,
                classifier, wnidWordsList, hierarchyLookupList, vImages, vClusters);

        // TODO: WARNING: getMCLParameters may not be static in the future
        MetricsReporter metricsReporter = new MetricsReporter(vImages, vClusters, affinityMatrix,
                MainActivityModel.getMCLParameters(), PATH_TO_LOGS);

        metricsReporter.run();

        // TODO: something else?

    }

}
