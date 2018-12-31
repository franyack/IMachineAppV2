package com.example.fran.imachineappv2;

import com.example.fran.imachineappv2.CIEngine.imagenet.HierarchyLookup;
import com.example.fran.imachineappv2.CIEngine.imagenet.WNIDWords;
import com.example.fran.imachineappv2.CIEngine.predictor.Classifier;
import com.example.fran.imachineappv2.CIEngine.predictor.Classifier.Recognition;
import com.example.fran.imachineappv2.CIEngine.clustering.MCLDenseEJML;
import com.example.fran.imachineappv2.CIEngine.predictor.TensorFlowImageClassifier;
import com.example.fran.imachineappv2.CIEngine.affinity.SemanticAffinity;
import com.example.fran.imachineappv2.CIEngine.affinity.VectorAffinity;
import com.example.fran.imachineappv2.CIEngine.imagenet.ImageNetUtils;
import com.example.fran.imachineappv2.CIEngine.imagenet.TopPredictions;
import com.example.fran.imachineappv2.CIEngine.imagenet.WNIDPrediction;
import com.example.fran.imachineappv2.CIEngine.util.ImageUtils;

import com.codekidlabs.storagechooser.StorageChooser;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.widget.CheckBox;

import org.apache.commons.io.FileUtils;
import org.ejml.data.DMatrixRMaj;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Created by fran on 24/05/18.
 */

public class MainActivityModel implements MainActivityMvpModel {

    private static final Logger LOGGER = Logger.getLogger(MainActivityView.class.getName());
    private MainActivityMvpPresenter mainActivityPresenter;

    // TODO: declare these variables in a JSON configuration file

    // Error codes for the app
    public static final int ERROR_NO_SELECTION = 0;
    public static final int ERROR_FOLDER_IS_EMPTY = 1;
    public static final int ERROR_INSUFFICIENT_STORAGE = 2;
    public static final int OK_FOLDER_READY = 100;

    // Max number of images supported by the app
    public static final int MAX_SUPPORTED_IMAGES = 400;

    // To measure progress while processing images
    private float partialProgress = 0.0f;
    private final float deltaProgress = 0.05f;


    // TODO: handle this in a better way
    public List<String> supportedImageFiles = Arrays.asList(".bmp",".gif",".jpg",".jpeg",".png",".tif",".tiff","thumbnails");

    // Input images
    private String[] pathToImages;
    private Vector<String> images = new Vector<>();
    private boolean tooManyImages = false;

    // Parameters for MobileNet model
    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();
    private static final String MODEL_PATH = "mobilenet_v1_224.tflite";
    private static final String LABEL_PATH = "labels.txt";
    private static final String WORDS_PATH = "words.txt";
    private static final String HIERARCHY_PATH = "wordnet.is_a.txt";
    private static final int IMG_W = 224;
    private static final int IMG_H = 224;
    private static final int BATCH_SIZE = 1;
    private static final int PIXEL_SIZE = 3;
    private static final float IMAGE_MEAN = 128.f;
    private static final float IMAGE_STD = 128.f;
    private static final double THRESHOLD_PROBABILITY_PREDICTION = 0.65;  // TODO: tune this

    // Parameters for clustering step
    // TODO: clustering parameters depending on N images to process?
    // TODO: static final, or there is a possibility to change them?
    private static final int maxIt = 100;
    private static final int expPow = 2;
    private static final int infPow = 3;
    private static final double epsConvergence = 1e-3;
    private static final double threshPrune = 0.01;

    // Attributes for handling results
    private List<WNIDWords> wnidWordsList = new ArrayList<>();
    private List<HierarchyLookup> hierarchyLookupList = new ArrayList<>();
    private ArrayList<String> vImages = new ArrayList<>();
    private ArrayList<Integer> vClusters = new ArrayList<>();
    private Set<Integer> ClustersResult = new TreeSet<>();  // TreeSet guarantees the order of elements when iterated
    private List<TopPredictions> topPredictions = new ArrayList<>();
    private List<float[]> embeddingsList = new ArrayList<>();
    private DMatrixRMaj affinityMatrix;
    private SemanticAffinity semanticAffinity = new SemanticAffinity(THRESHOLD_PROBABILITY_PREDICTION);
    private VectorAffinity vectorAffinity = new VectorAffinity(THRESHOLD_PROBABILITY_PREDICTION);

    private long startLoop;

    // Constructor
    MainActivityModel(MainActivityPresenter presenter) {
        this.mainActivityPresenter = presenter;
        resetProgress();
    }

    @Override
    public void deleteClusterResultFolder(String pathFolderResult,final MainActivityView mainActivityView) {
        File folder = new File(pathFolderResult);
        if (folder.exists()){
            try {
                // Delete directory and send updates
                LOGGER.info(String.format("Removing content from %s", pathFolderResult));
                FileUtils.deleteDirectory(folder);
                Intent mediaScannerIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri fileContentUri = Uri.fromFile(folder);
                mediaScannerIntent.setData(fileContentUri);
                mainActivityView.sendBroadcast(mediaScannerIntent);
            } catch (IOException e) {
                LOGGER.warning(String.format("An exception has occurred! '%s'", e.toString()));
                e.printStackTrace();
            }
        }
    }

    @Override
    public void chooseGallery(MainActivityView view) {
        // TODO: create chooser on constructor? or every time this funcion is called?
        StorageChooser chooser = new StorageChooser.Builder()
                .withActivity(view)
                .withFragmentManager(view.getFragmentManager())
                .withMemoryBar(true)
                .allowCustomPath(true)
                .setType(StorageChooser.DIRECTORY_CHOOSER)
                .build();

        // Show dialog whenever you want
        chooser.show();

        // Get the path chosen by the user
        chooser.setOnSelectListener(new StorageChooser.OnSelectListener() {
            @Override
            public void onSelect(String path) {
                mainActivityPresenter.showGalleryChosen(path);
            }
        });
    }

    @Override
    public void checkBoxClick(CheckBox chAllImages) {
        if (chAllImages.isChecked()){
            mainActivityPresenter.buttonChooseGalleryEnable(false);
        }else{
            mainActivityPresenter.buttonChooseGalleryEnable(true);
        }
        mainActivityPresenter.showGalleryChosen("");  // TODO: empty path to not print anything?
    }

    @Override
    public int prepareImages(String path_chosen, CheckBox chAllImages, Context applicationContext) {
        // TODO: use logger

        if (path_chosen.equals("") && !chAllImages.isChecked())
            // No path was selected so there are no images to process
            return ERROR_NO_SELECTION;

        if (chAllImages.isChecked())
            // Then change the path to obtain all the images stored on DCIM
            path_chosen = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM).getAbsolutePath();

        // Open a file from the chosen path
        File curDir = new File(path_chosen);

        // Clear previous selections, and get all the images to process
        // TODO: do this on getAllFiles
        if (images.size()>0)
            images.clear();
        getAllFiles(curDir);

        if (images.size() == 0)
            // It means that there are no valid images on the selected folder(s)
            return ERROR_FOLDER_IS_EMPTY;

        // Obtain the paths to images and the total of bytes involved
        // TODO: why create 'pathToImages' having 'images' available?
        pathToImages = new String[images.size()];
        long totalBytes = 0;
        File file;  // TODO: is it convenient to create a lot of files that are not going to be used?
        for (int i = 0; i<images.size(); i++){
            pathToImages[i] = images.get(i);
            file = new File(pathToImages[i]);
            if(file.exists()) totalBytes += file.length();
        }

        totalBytes*=2; // Because we need to save more storage for the temporary folder to create
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long bytesAvailable = stat.getBlockSizeLong() * stat.getAvailableBlocksLong();

        if(bytesAvailable<totalBytes)
            // Then there is not enough space to process the image on this application
            return ERROR_INSUFFICIENT_STORAGE;

        // Finally, save the list of paths to be processed later
        // TODO: why not load the images into memory just in this method?
        writeToFile(pathToImages, applicationContext);

        return OK_FOLDER_READY;
    }

    private void writeToFile(String[] data, Context context) {
        try {
            File imagesPathFile = context.getFileStreamPath("imagespath.txt");
            if(imagesPathFile.exists()){
                FileUtils.forceDelete(imagesPathFile);
            }
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("imagespath.txt", Context.MODE_PRIVATE));
            for(String d:data){
                outputStreamWriter.write(d);
                outputStreamWriter.write("\n");
            }
            outputStreamWriter.close();
        }
        catch (IOException e) {
            LOGGER.warning("Exception! File write failed: " + e.toString());
        }
    }

    private boolean isValidImageFile(String path){
        boolean isValid = false;

        String p = path.toLowerCase();

        for (String s:supportedImageFiles)
            if(p.endsWith(s)){
                isValid = true;
                break;
            }

        return isValid;
    }

    private void getAllFiles(File curDir){
        File[] filesList = curDir.listFiles();
        String filePath;

        for(File f : filesList)
            if(f.isDirectory())
                // Recursive call to children
                getAllFiles(f);
            else
                if(f.isFile()){
                    // If the max of images has reached, then stop adding
                    if (images.size()>= MAX_SUPPORTED_IMAGES){
                        tooManyImages = true;
                        break;
                    }

                    // Check the file format before adding it
                    filePath = f.getAbsolutePath();
                    if (isValidImageFile(filePath)){
                        images.add(filePath);
                    }
                }
    }

    @Override
    public void fillWorkingText() {
        mainActivityPresenter.showWorkingText(""+images.size());
    }

    @Override
    public void startImageProcess(MainActivityView mainActivityView) {
        // TODO: load these things on the constructor would be the correct way
        try {
            startLoop = System.nanoTime();

            AssetManager assetManager = mainActivityView.getAssets();

            BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open(WORDS_PATH)));
            wnidWordsList = ImageNetUtils.loadWNIDWords(reader);
//            label_lookup = TensorFlowImageClassifier.loadLabelList(mainActivityView.getAssets(),LABEL_PATH);

            reader = new BufferedReader(new InputStreamReader(assetManager.open(HIERARCHY_PATH)));
            hierarchyLookupList = ImageNetUtils.loadHierarchyLookup(reader);
            initTensorFlowAndLoadModel(mainActivityView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void folderGenerator(String pathFolder, final MainActivityView mainActivityView) {
        // TODO: check this method!
        File folder = new File(pathFolder);
        if (!folder.exists()) {
            folder.mkdirs();
        } else {
            try {
                FileUtils.cleanDirectory(folder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String number;
        int numberFolder=1;
        for (Integer cluster:ClustersResult) {
            //TODO: remember change when the size of images processed grow up
            number = String.valueOf(numberFolder);
            if(number.length()==1){
                number = "000"+number;
            }else{
                if(number.length()==2){
                    number = "00"+number;
                }else{
                    if(number.length()==3){
                        number = "0"+number;
                    }
                }
            }
            mainActivityPresenter.growProgress();
            folder = new File(pathFolder + File.separator + mainActivityView.getApplicationContext().getString(R.string.folder) + number);
            try {
                FileUtils.forceMkdir(folder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (int j = 0; j < vClusters.size(); j++) {
                if (Objects.equals(cluster, vClusters.get(j))) {
                    File source = new File(vImages.get(j));
                    try {
                        FileUtils.copyFileToDirectory(source,folder);
                        //You need to tell the media scanner about the new file so that it is immediately available to the user.
                        source = new File(folder.getPath() + File.separator + source.getName());
                        Intent mediaScannerIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        Uri fileContentUri = Uri.fromFile(source);
                        mediaScannerIntent.setData(fileContentUri);
                        mainActivityView.getApplicationContext().sendBroadcast(mediaScannerIntent);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            numberFolder++;
        }

        mainActivityPresenter.showFilesManager(pathFolder);
    }

    @Override
    public boolean folderResultsExist(String pathFoldersResult) {
        File folder = new File(pathFoldersResult);
        return folder.exists();
    }

    @Override
    public List<String> getMclParameters() {
        List<String> mclParameters = new ArrayList<>();
        mclParameters.add(""+maxIt);
        mclParameters.add(""+expPow);
        mclParameters.add(""+infPow);
        mclParameters.add(""+threshPrune);
        return  mclParameters;
    }

    @Override
    public void checkNumberImages(MainActivityView mainActivityView) {
        if(tooManyImages){
            final AlertDialog.Builder builder = new AlertDialog.Builder(mainActivityView);
            builder.setTitle(R.string.attention);
            // TODO: too many images
            builder.setMessage(R.string.tooMuchImages);
            final AlertDialog dialog=builder.create();
            dialog.show();
            // Hide after some seconds
            final Handler handler  = new Handler();
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                    }
                }
            };

            dialog .setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    handler.removeCallbacks(runnable);
                }
            });
            handler.postDelayed(runnable, 4000);
        }
    }
    private void resetProgress(){
        partialProgress = 0.0f;
    }

    private void checkAndReportProgress(int nImgs){
        float totalProgress = (float) nImgs / pathToImages.length;
        if (totalProgress > partialProgress){
            partialProgress += deltaProgress;
            mainActivityPresenter.reportProgress((int) (totalProgress * 100));
        }

    }

    @SuppressLint("DefaultLocale")
    private void processImages() {
        // TODO: review this way to estimate progress

        int nImgs = 0;
        for (int i = 0; i< pathToImages.length; i++){
            checkAndReportProgress(nImgs++);

            Map<Integer, Object> outputs = new TreeMap<>();
            Bitmap image = ImageUtils.lessResolution(pathToImages[i],IMG_W,IMG_H);
            image = Bitmap.createScaledBitmap(image,IMG_W,IMG_H,false);
            ByteBuffer byteBuffer;
            if (image != null){
                byteBuffer = ImageUtils.convertBufferedImageToByteBuffer(
                        image,BATCH_SIZE,IMG_W,IMG_H,PIXEL_SIZE,IMAGE_MEAN,IMAGE_STD);

                classifier.recognize(byteBuffer, outputs);
                byteBuffer.clear();

                List<Recognition> results = (List<Recognition>) outputs.get(0);
                float[][][][] emb = ((float[][][][]) outputs.get(1)).clone();

                List<WNIDPrediction> wnIdPredictionsList = new ArrayList<>();
                // assert results.size() > 0
                if (results.size() == 0){
                    // TODO: this is an error or what?
                    WNIDPrediction entity = new WNIDPrediction("n00001740",1);
                    wnIdPredictionsList.add(entity);
                }else{
                    int depth = 4;
                    double threshProb = 0.05;
                    wnIdPredictionsList = ImageNetUtils.processTopPredictions(results,
                                wnidWordsList,hierarchyLookupList, depth, threshProb);

                }
                topPredictions.add(new TopPredictions(pathToImages[i], wnIdPredictionsList));
                embeddingsList.add(emb[0][0][0].clone());
            }
        }

        double[][] g_aff_matrix = semanticAffinity.getAffinityMatrix(topPredictions);
        double[][] i_aff_matrix = vectorAffinity.getAffinityMatrix(embeddingsList);

        List<DMatrixRMaj> matList = new ArrayList<>();
        matList.add(new DMatrixRMaj(g_aff_matrix));
        matList.add(new DMatrixRMaj(i_aff_matrix));

        affinityMatrix = MCLDenseEJML.averageMatrices(matList);

        MCLDenseEJML mcl = new MCLDenseEJML(maxIt, expPow, infPow, epsConvergence, threshPrune);

        affinityMatrix = mcl.run(affinityMatrix);

        ArrayList<ArrayList<Integer>> clusters = mcl.getClusters(affinityMatrix);

        for (int i = 0; i< pathToImages.length; i++){
            vImages.add(pathToImages[i]);
            for (int j=0;j<clusters.size();j++){
                for (int k=0;k<clusters.get(j).size();k++){
                    if(clusters.get(j).get(k) == i){
                        vClusters.add(j);
                        break;
                    }
                }
            }
        }

        // TODO: check this! why the loop?
        //for(int i=0;i<3;i++){
        //    MCLDenseEJML.postCluster();
        //}

        // Fill the set of clusters with the given results
        fillClustersResult(vClusters);

        // Toc
        double tLoop = (System.nanoTime() - startLoop) / 1e9;
        LOGGER.info(String.format("Total process took %f seconds", tLoop));

//        Metrics a = new Metrics(affinityMatrix,vImages,vClusters);
//        a.Silhouette();

        // Report end of process
        mainActivityPresenter.clustersReady();
    }

    private void fillClustersResult(ArrayList<Integer> vClusters) {
        // First clear previous results
        if (ClustersResult.size()>0)
            ClustersResult.clear();

        // Then add the new ones to the Set of results
        ClustersResult.addAll(vClusters);
    }

    private void initTensorFlowAndLoadModel(final MainActivityView mainActivityView) throws IOException {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    AssetManager assetManager = mainActivityView.getAssets();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open(LABEL_PATH)));
                    AssetFileDescriptor fileDescriptor = assetManager.openFd(MODEL_PATH);
                    FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
                    long startOffset = fileDescriptor.getStartOffset();
                    long declaredLength = fileDescriptor.getDeclaredLength();
                    classifier = TensorFlowImageClassifier.create(
                            reader,
                            inputStream,
                            startOffset,
                            declaredLength,
                            IMG_W);
                    setParameters();
                } catch (final Exception e) {
//                    mainActivityView.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            mainActivityPresenter.callErrorToast(e.toString());
//                        }
//                    });
                    throw new RuntimeException("Error!", e);
                }
            }
        });
    }
    private void setParameters() {
        if (vImages.size()>0){
            vImages.clear();
        }
        if (vClusters.size()>0){
            vClusters.clear();
        }
        processImages();
    }

}