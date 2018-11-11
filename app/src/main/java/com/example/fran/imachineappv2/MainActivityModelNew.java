package com.example.fran.imachineappv2;

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
import android.util.Log;
import android.widget.CheckBox;

import com.codekidlabs.storagechooser.StorageChooser;
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

import org.apache.commons.io.FileUtils;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
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

public class MainActivityModelNew implements MainActivityMvpModel {

    private MainActivityMvpPresenter mainActivityPresenter;

    private String[] imagespath;
    private Vector<String> images = new Vector<>();
    private boolean tooMuchImages = false;

    private static final Logger LOGGER = Logger.getLogger(MainActivityView.class.getName());
    private static final String MODEL_PATH = "mobilenet_v1_224.tflite";
    private static final String LABEL_PATH = "labels2.txt";
    private static final String WORDS_PATH = "words.txt";
    private static final String HIERARCHY_PATH = "wordnet.is_a.txt";
    //private static final int INPUT_SIZE = 224;
    private static final int IMG_W = 224;
    private static final int IMG_H = 224;
    private static final int BATCH_SIZE = 1;
    private static final int PIXEL_SIZE = 3;
    private static final float IMAGE_MEAN = 128.f;
    private static final float IMAGE_STD = 128.f;

    //MCL
    private int maxIt = 100;
    private int expPow = 2;
    private int infPow = 3;
    private static final double epsConvergence = 1e-3;
    private static final double threshPrune = 0.01;
    int n = 100;
    int seed = 1234;

    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();

    private static final double THRESHOLD_PROBABILITY_PREDICTION = 0.65;

    // private ImageNetUtils imageNetUtils = new ImageNetUtils();
    private List<WNIDWords> wnidWordsList = new ArrayList<>();
    private List<HierarchyLookup> hierarchyLookupList = new ArrayList<>();
    private ArrayList<String> vImages = new ArrayList<>();
    private ArrayList<Integer> vClusters = new ArrayList<>();
//    Vector<Integer> vClustersResult = new Vector<>();
    private Set<Integer> ClustersResult = new TreeSet<>();  // TreeSet guarantees the order of elements when iterated
    private List<TopPredictions> topPredictions = new ArrayList<>();
    private List<float[]> embeddingsList = new ArrayList<>();
    private DMatrixRMaj affinityMatrix;

    private long startLoop;


    //Constructor
    MainActivityModelNew(MainActivityPresenter presenter) {
        this.mainActivityPresenter = presenter;
    }

    @Override
    public void deleteClusterResultFolder(String pathFolderResult,final MainActivityView mainActivityView) {
        File folder = new File(pathFolderResult);
        if (folder.exists()){
            try {
                FileUtils.deleteDirectory(folder);
                Intent mediaScannerIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri fileContentUri = Uri.fromFile(folder);
                mediaScannerIntent.setData(fileContentUri);
                mainActivityView.sendBroadcast(mediaScannerIntent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void chooseGallery(MainActivityView view) {
        StorageChooser chooser = new StorageChooser.Builder()
                .withActivity(view)
                .withFragmentManager(view.getFragmentManager())
                .withMemoryBar(true)
                .allowCustomPath(true)
                .setType(StorageChooser.DIRECTORY_CHOOSER)
                .build();

        // Show dialog whenever you want by
        chooser.show();

        // get path that the user has chosen
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
        mainActivityPresenter.showGalleryChosen("");
    }

    @Override
    public int prepararImagenes(String path_chosen, CheckBox chAllImages, Context applicationContext) {
        File curDir;
        if (path_chosen.equals("") && !chAllImages.isChecked()){
            return 0;
        }
        if (chAllImages.isChecked()){
            String dcimPath = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM).getAbsolutePath();
            curDir = new File(dcimPath);
        }else{
            curDir = new File(path_chosen);
        }
        if (images.size()>0){
            images.clear();
        }
        getAllFiles(curDir);
        imagespath = new String[images.size()];
        long totalBytes = 0;
        File file;
        for (int i = 0; i<images.size(); i++){
            imagespath[i] = images.get(i);
            file = new File(imagespath[i]);
            if(file.exists()){
                totalBytes+=file.length();
            }
        }
        //Because we need to save storage por the temporary folder
        totalBytes*=2;
        if(imagespath.length==0){
            return 1;
        }

        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long bytesAvailable;
        bytesAvailable = stat.getBlockSizeLong() * stat.getAvailableBlocksLong();
        if(bytesAvailable<totalBytes){
            return 2;
        }

        writeToFile(imagespath, applicationContext);

        return 3;
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
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private void getAllFiles(File curDir){
        File[] filesList = curDir.listFiles();
        for(File f : filesList){
            if(f.isDirectory()) {
                getAllFiles(f);
            }else {
                if(f.isFile()){
                    if (images.size()>=400){
                        tooMuchImages = true;
                        break;
                    }
                    //TODO: lower path
                    if ((f.getAbsolutePath().contains(".jpg") || f.getAbsolutePath().contains(".gif") || f.getAbsolutePath().contains(".bmp")
                            || f.getAbsolutePath().contains(".jpeg") || f.getAbsolutePath().contains(".tif") || f.getAbsolutePath().contains(".tiff")
                            || f.getAbsolutePath().contains(".png")) && !f.getAbsolutePath().contains("thumbnails")){
                        images.add(f.getAbsolutePath());
                    }
                }
            }
        }
    }

    @Override
    public void fillWorkingText() {
        mainActivityPresenter.showWorkingText(""+images.size());
    }

    @Override
    public void startImageProcess(MainActivityView mainActivityView) {
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
        if(tooMuchImages){
            final AlertDialog.Builder builder = new AlertDialog.Builder(mainActivityView);
            builder.setTitle(R.string.attention);
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

    //    @Override
    // TODO: private?
    public void processImages() {
        int percent = imagespath.length / 30;
        if(percent < 1){
            percent=1;
        }
        int cantidadImgProcesadas = 0;
        for (int i = 0; i< imagespath.length; i++){
            if(cantidadImgProcesadas==percent){
                mainActivityPresenter.growProgress();
                cantidadImgProcesadas=0;
            }else {
                cantidadImgProcesadas+=1;
            }
            Map<Integer, Object> outputs = new TreeMap<>();
            Bitmap image = ImageUtils.lessResolution(imagespath[i],IMG_W,IMG_H);
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
                topPredictions.add(new TopPredictions(imagespath[i], wnIdPredictionsList));
                embeddingsList.add(emb[0][0][0].clone());
//                LOGGER.info(imagespath[i]);
//            for (int j=0;j<wnIdPredictionsList.size();j++){
//                String word;
//                word = imageNetUtils.get_label_from_wnid(wnIdPredictionsList.get(j).getWnId(),imageNetUtils.wnidWordsList);
//                LOGGER.info(word+": "+wnIdPredictionsList.get(j).getPrediction());
//            }
//            LOGGER.info("                                                                                ");
            }
        }
        double[][] g_aff_matrix, i_aff_matrix;
        // TODO: all variables should be created in Constructor
        SemanticAffinity sa = new SemanticAffinity(THRESHOLD_PROBABILITY_PREDICTION);
        g_aff_matrix = sa.getAffinityMatrix(topPredictions);

        VectorAffinity va = new VectorAffinity(THRESHOLD_PROBABILITY_PREDICTION);
        i_aff_matrix = va.getAffinityMatrix(embeddingsList);
        DMatrixRMaj g_matrix = new DMatrixRMaj(g_aff_matrix);
        DMatrixRMaj i_matrix = new DMatrixRMaj(i_aff_matrix);

        DMatrixRMaj cluster_matrix = new DMatrixRMaj(g_matrix.numRows, g_matrix.numCols);
        CommonOps_DDRM.add(g_matrix, i_matrix, cluster_matrix);
        CommonOps_DDRM.divide(cluster_matrix, 2.0);

        affinityMatrix = cluster_matrix.copy();

        MCLDenseEJML mcl = new MCLDenseEJML(maxIt, expPow, infPow, epsConvergence, threshPrune);

//        mainActivityPresenter.growProgress();
//        LOGGER.info("                                                                           ");
//        LOGGER.info("Rows: "+cluster_matrix.getNumRows());
//        LOGGER.info("Rows: "+cluster_matrix.getNumCols());
        cluster_matrix = mcl.run(cluster_matrix);

        ArrayList<ArrayList<Integer>> clusters = mcl.getClusters(cluster_matrix);

        for (int i=0;i<imagespath.length;i++){
            vImages.add(imagespath[i]);
            for (int j=0;j<clusters.size();j++){
                for (int k=0;k<clusters.get(j).size();k++){
                    if(clusters.get(j).get(k) == i){
                        vClusters.add(j);
                        break;
                    }
                }
            }
        }
        for(int i=0;i<3;i++){
            postCluster();
        }

        fillClustersResult(vClusters);

        double tLoop = (System.nanoTime() - startLoop) / 1e9;

        LOGGER.info(String.format("Total process took %f seconds", tLoop));

//        Metrics a = new Metrics(affinityMatrix,vImages,vClusters);
//        a.Silhouette();

        mainActivityPresenter.clustersReady();
    }

    private void postCluster() {
        List<String> imagesNotClustered = new ArrayList<>();
        for(int i=0;i<vClusters.size();i++){
            int size=0;
            for(int j=0; j<vClusters.size();j++){
                if(Objects.equals(vClusters.get(i), vClusters.get(j))){
                    size++;
                }
            }
            if(size==1){
                imagesNotClustered.add(vImages.get(i));
            }
        }
        int imagePosition, newClusterPosition;
        String imageWithMaxAffinity;
        for(String image:imagesNotClustered){
            imageWithMaxAffinity=getImageWithMaxAffinity(image);
            imagePosition=0;
            newClusterPosition=0;
            while(!vImages.get(imagePosition).equals(image)){
                imagePosition++;
            }
            if(imageWithMaxAffinity.equals("")){
                vClusters.set(imagePosition,999);
            }else{
                while(!vImages.get(newClusterPosition).equals(imageWithMaxAffinity)){
                    newClusterPosition++;
                }
                vClusters.set(imagePosition,vClusters.get(newClusterPosition));
            }
        }
    }

    private String getImageWithMaxAffinity(String image) {
        int i=0;
        while(!vImages.get(i).equals(image)){
            i++;
        }
        double maxAffinity=0;
        String maxAffinityImage="";
        for(int j=0;j<affinityMatrix.numCols;j++){
            if(i!=j){
                if(affinityMatrix.get(i,j)>maxAffinity){
                    maxAffinity=affinityMatrix.get(i,j);
                    maxAffinityImage=vImages.get(j);
                }
            }
        }
        return maxAffinityImage;
    }

    private void postProcess() {
        List<List<String>> groups = new ArrayList<>();
        List<String> groupTemp;
        for (int i = 0; i < ClustersResult.size(); i++) {
            groupTemp = new ArrayList<>();
            for (int j = 0; j < vClusters.size(); j++) {
                if (vClusters.get(j) == i) {
                    groupTemp.add(vImages.get(j));
                }
            }
            groups.add(groupTemp);
        }
        int percentThreshold;
        if(vImages.size()<50){
            percentThreshold=10;
        }else{
            percentThreshold=4;
        }
        List<Float> groupsPercent = new ArrayList<>();
        List<Float> groupsAffinityAverage = new ArrayList<>();
        for(int i = 0;i<groups.size();i++){
            groupsPercent.add((float) (((float) groups.get(i).size()/vImages.size())*100.0));
            groupsAffinityAverage.add((float) getAffinitySum(groups.get(i))/groups.get(i).size());
        }
        List<Integer> smallGroups = new ArrayList<>();
        List<Integer> bigGroups = new ArrayList<>();
        for(int i=0; i<groupsPercent.size();i++){
            if(groupsPercent.get(i)<=percentThreshold){
                smallGroups.add(i);
            }else{
                bigGroups.add(i);
            }
        }
        float newAffinity=0,newAffinityIterator=0;
        int r;
        List<String> newCluster, newClusterIterator;
        newCluster=new ArrayList<>();
        while(smallGroups.size()>0) {
            for (int k = 0; k < smallGroups.size(); k++) {
                if (bigGroups.size() == 0) {
                    break;
                }
                r=-1;
                for (int i = 0; i < bigGroups.size(); i++) {
                    newClusterIterator = new ArrayList<>();
                    for(int j=0; j<groups.get(smallGroups.get(k)).size();j++){
                        newClusterIterator.add(groups.get(smallGroups.get(k)).get(j));
                    }
                    for (int j = 0; j < groups.get(bigGroups.get(i)).size(); j++) {
                        newClusterIterator.add(groups.get(bigGroups.get(i)).get(j));
                    }
                    newAffinityIterator = (float) getAffinitySum(newClusterIterator)/newClusterIterator.size();
                    if(Math.abs(newAffinityIterator-groupsAffinityAverage.get(bigGroups.get(i))) > .05 ){
                        if(newAffinityIterator>=newAffinity){
                            newAffinity=newAffinityIterator;
                            newCluster=newClusterIterator;
                            r=i;
                        }
//                        Iterator<List<String>> a = groups.iterator();
//                        while(a.hasNext()){
//                            List<String> o = a.next();
//                            if(groups.get(bigGroups.get(i))==a){
//                                a.remove();
//                            }else{
//                                if(groups.get(smallGroups.get(k))==a){
//                                    a.remove();
//                                }
//                            }
//                        }
                    }
                }
                if (r != -1) {
                    groups.remove(bigGroups.get(r));
                    groups.remove(smallGroups.get(k));
                    groups.add(newCluster);
                    smallGroups.remove(k);
                    groupsAffinityAverage.clear();
                    for(int i = 0;i<groups.size();i++){
                        groupsAffinityAverage.add((float) getAffinitySum(groups.get(i))/groups.get(i).size());
                    }
                }
            }
        }
    }

    private double getAffinitySum(List<String> images) {
        double average = 0;
        for(int i = 0; i< images.size(); i++){
            if(i+1<images.size()){
                average+=getAffinity(images.get(i), images.get(i+1));
//                LOGGER.info("Afinidad entre " + images.get(i) + " y " + images.get(i+1) + " = " + getAffinity(images.get(i), images.get(i+1)));
//                LOGGER.info(" - ");
            }
        }
        return average;
    }

    private double getAffinity(String img1, String img2) {
        int i=0,j=0;
        while(i<vImages.size()){
            if(vImages.get(i).equals(img1)){
                break;
            }
            i++;
        }
        while(j<vImages.size()){
            if(vImages.get(j).equals(img2)){
                break;
            }
            j++;
        }
        return affinityMatrix.get(i,j);
    }

    private void fillClustersResult(ArrayList<Integer> vClusters) {
//        ArrayList<Integer> vClustersCopy = new ArrayList<Integer>(vClusters);
        if (ClustersResult.size()>0){
            ClustersResult.clear();
        }
        for (Integer cluster: vClusters) {
            if (!ClustersResult.contains(cluster)){
                ClustersResult.add(cluster);
            }
        }
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