package com.example.fran.imachineappv2;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.CheckBox;
import com.codekidlabs.storagechooser.StorageChooser;
import com.example.fran.imachineappv2.CIEngine.Classifier;
import com.example.fran.imachineappv2.CIEngine.MCLDenseEJML;
import com.example.fran.imachineappv2.CIEngine.TensorFlowImageClassifier;
import com.example.fran.imachineappv2.Utils.Imagenet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    private MainActivityMvpPresenter mainActivityPresenter;

    private String[] imagespath;
    private Vector<String> images = new Vector<>();

    private static final Logger LOGGER = Logger.getLogger(MainActivityView.class.getName());
    private static final String MODEL_PATH = "mobilenet_v1_224.tflite";
    private static final String LABEL_PATH = "labels2.txt";
    private static final String WORDS_PATH = "words.txt";
    private static final String HIERARCHY_PATH = "wordnet.is_a.txt";
    private static final int INPUT_SIZE = 224;
    private static final int BATCH_SIZE = 1;
    private static final int PIXEL_SIZE = 3;
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128.0f;

    //MCL
    private int maxIt = 100;
    private int expPow = 2;
    private int infPow = 3;
    private double epsConvergence = 1e-3;
    private double threshPrune = 0.01;
    int n = 100;
    int seed = 1234;

    /* Preallocated buffers for storing image data in. */
    private int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];


    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();

    private double THRESHOLD_PROBABILITY_PREDICTION = 0.65;

    private Imagenet wnid_lookup = new Imagenet();
//    private List<String> label_lookup = new ArrayList<>();


    private ArrayList<String> vImages = new ArrayList<>();
    private ArrayList<Integer> vClusters = new ArrayList<>();
//    Vector<Integer> vClustersResult = new Vector<>();
    private Set<Integer> ClustersResult = new TreeSet<>();  // TreeSet guarantees the order of elements when iterated
    private List<Top_Predictions> top_predictions = new ArrayList<>();
    private List<float[]> embeddings_list = new ArrayList<>();
    private DMatrixRMaj affinityMatrix;

    private long startLoop;


    //Constructor
    MainActivityModel(MainActivityPresenter presenter) {
        this.mainActivityPresenter = presenter;
    }

    @Override
    public void deleteClusterResultFolder(String pathFolderResult) {
        File folder = new File(pathFolderResult);
        if (folder.exists()){
            try {
                FileUtils.deleteDirectory(folder);
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
            curDir = new File("/storage/emulated/0");
        }else{
            curDir = new File(path_chosen);
        }
        if (images.size()>0){
            images.clear();
        }
        getAllFiles(curDir);
        imagespath = new String[images.size()];
        for (int i = 0; i<images.size(); i++){
            imagespath[i] = images.get(i);
        }

        if(imagespath.length==0){
            return 1;
        }

        writeToFile(imagespath, applicationContext);

        return 2;
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
                    if (images.size()>=300){
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
            wnid_lookup.wnidWordsList = wnid_lookup.loadWnIDWords(mainActivityView.getAssets(),WORDS_PATH);
//            label_lookup = TensorFlowImageClassifier.loadLabelList(mainActivityView.getAssets(),LABEL_PATH);
            wnid_lookup.hierarchyLookupList = wnid_lookup.loadHierarchy_lookup(mainActivityView.getAssets(),HIERARCHY_PATH);
            initTensorFlowAndLoadModel(mainActivityView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void folderGenerator(String pathFolder, Context applicationContext) {
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
                number = "00"+number;
            }else{
                if(number.length()==2){
                    number = "0"+number;
                }
            }
            mainActivityPresenter.growProgress();
            folder = new File(pathFolder + File.separator + applicationContext.getString(R.string.folder) + number);
            folder.mkdirs();
            for (int j = 0; j < vClusters.size(); j++) {
                if (Objects.equals(cluster, vClusters.get(j))) {
                    File source = new File(vImages.get(j));
                    File destination = new File(folder.getAbsolutePath() + File.separator);
                    try {
                        FileUtils.copyFileToDirectory(source,destination);
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
        return  mclParameters;
    }

    //    @Override
    public void processImages() {
        int percent = (int) (imagespath.length / 30);
        if(percent < 1){
            percent=1;
        }
        int cantidadImgProcesadas = 0;
        for (int i = 0; i< imagespath.length; i++){
            if(cantidadImgProcesadas==percent){
                mainActivityPresenter.growProgress();
                cantidadImgProcesadas=0;
//                LOGGER.info("processImages");
            }else {
                cantidadImgProcesadas+=1;
            }
            Map<Integer, Object> outputs = new TreeMap<>();
            Bitmap image = lessResolution(imagespath[i],INPUT_SIZE,INPUT_SIZE);
            image = Bitmap.createScaledBitmap(image,INPUT_SIZE,INPUT_SIZE,false);
            ByteBuffer byteBuffer;
            if (image != null){
                byteBuffer = convertBitmapToByteBuffer(image);

                classifier.recognize(byteBuffer, outputs);
                byteBuffer.clear();

                List<Classifier.Recognition> results = (List<Classifier.Recognition>) outputs.get(0);
                float[][][][] emb = ((float[][][][]) outputs.get(1)).clone();

                List<wnIdPredictions> wnIdPredictionsList = new ArrayList<>();
                if (results.size() == 0){
                    wnIdPredictions entity = new wnIdPredictions("n00001740",1);
                    wnIdPredictionsList.add(entity);
                }else{
                    wnIdPredictionsList = process_top_predictions(results,4,0.05);
                }
                top_predictions.add(new Top_Predictions(imagespath[i], wnIdPredictionsList));
                embeddings_list.add(emb[0][0][0].clone());
//                LOGGER.info(imagespath[i]);
//            for (int j=0;j<wnIdPredictionsList.size();j++){
//                String word;
//                word = wnid_lookup.get_label_from_wnid(wnIdPredictionsList.get(j).getWnId(),wnid_lookup.wnidWordsList);
//                LOGGER.info(word+": "+wnIdPredictionsList.get(j).getPrediction());
//            }
//            LOGGER.info("                                                                                ");
            }
        }
        double[][] g_aff_matrix, i_aff_matrix;
        g_aff_matrix = getGrammaticalAffinity(top_predictions);

        i_aff_matrix = getImageAffinity(embeddings_list);
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
        postCluster();

        fillClustersResult(vClusters);

        //TODO: here
//        postProcess();

        double tLoop = (System.nanoTime() - startLoop) / 1e9;

        LOGGER.info(String.format("Total process took %f seconds", tLoop));

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
                LOGGER.info("Afinidad entre " + images.get(i) + " y " + images.get(i+1) + " = " + getAffinity(images.get(i), images.get(i+1)));
                LOGGER.info(" - ");
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

    public static Bitmap lessResolution (String filePath, int width, int height){
        BitmapFactory.Options options = new BitmapFactory.Options();

        //First decode with inJustDecodeBounds = true to check dimensions
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath,options);

        //Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, width, height);

        //Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(filePath,options);

    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth){
            //Calculate ratios of height and width to requested heigth and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            //Choose the smallest ratio as inSampleSie value, this will guarantee
            //a final image with both dimensions larger than or equal to the requested
            //height and width
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        //4 por el uso de flotantes, 4 bytes -> 1 float de 32 bits
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * BATCH_SIZE * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());
        //byteBuffer.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < INPUT_SIZE; ++i) {
            for (int j = 0; j < INPUT_SIZE; ++j) {
                final int val = intValues[pixel++];
                //byteBuffer.put((byte) ((val >> 16) & 0xFF));
                //byteBuffer.put((byte) ((val >> 8) & 0xFF));
                //byteBuffer.put((byte) (val & 0xFF));
                byteBuffer.putFloat((((val >> 16) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                byteBuffer.putFloat((((val >> 8) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                byteBuffer.putFloat((((val) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
            }
        }
        return byteBuffer;
    }

    private List<wnIdPredictions> process_top_predictions(List<Classifier.Recognition> results, int depth, double thresh_prob) {
        List<wnIdPredictions> predictions = new ArrayList<>();
        String wnid;
        ArrayList<String> full_hiearchy;
        for (int i = 0; i< results.size(); i++){
            if (results.get(i).getConfidence()>thresh_prob){
                wnid = wnid_lookup.get_wnid_from_label(results.get(i).getTitle(),wnid_lookup.wnidWordsList);
                full_hiearchy = wnid_lookup.get_full_hierarchy(wnid,depth,wnid_lookup.hierarchyLookupList);
                for (int j=0;j<full_hiearchy.size();j++){
                    predictions.add(new wnIdPredictions(full_hiearchy.get(j), results.get(i).getConfidence()));
                }
            }
        }
        List<wnIdPredictions> result = new ArrayList<>();

        result.add(new wnIdPredictions(predictions.get(0).getWnId(),predictions.get(0).getPrediction()));

        for (int i=1; i<predictions.size();i++){
            int d = 0;
            boolean add = true;
            while (d<result.size()){
                if(predictions.get(i).getWnId().equals(result.get(d).getWnId())){
                    result.get(d).setPrediction(result.get(d).getPrediction()+predictions.get(i).getPrediction());
                    add = false;
                    break;
                }
                d+=1;
            }
            if(add){
                result.add(new wnIdPredictions(predictions.get(i).getWnId(),predictions.get(i).getPrediction()));
            }
        }
        Collections.sort(result, new sortByPredicction());

        return result;
    }
    public class sortByPredicction implements Comparator<wnIdPredictions> {
        public int compare(wnIdPredictions a, wnIdPredictions b)
        {
            return (int) (a.getPrediction() - b.getPrediction());
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
                            INPUT_SIZE);
                    setParameters();
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
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

    public class Top_Predictions{
        String img_path;
        List<wnIdPredictions> result;

        private Top_Predictions(String s, List<wnIdPredictions> wnIdPredictions) {
            img_path = s;
            result = wnIdPredictions;
        }


//        public String getImg_path() {
//            return img_path;
//        }

        private List<wnIdPredictions> getResult() {
            return result;
        }
    }

    private class wnIdPredictions{
        String wnId;
        float prediction;
        private wnIdPredictions(String wnId, float prediction){
            this.wnId = wnId;
            this.prediction = prediction;
        }

        private String getWnId() {
            return wnId;
        }

        private float getPrediction() {
            return prediction;
        }

        private void setPrediction(float prediction) {
            this.prediction = prediction;
        }
    }

    // TODO: scope: CIEngine
    private double[][] getGrammaticalAffinity(List<Top_Predictions> top_predictions) {
        double[][] result = new double[top_predictions.size()][top_predictions.size()];
        Set<String> dictionary = new TreeSet<>();  // TreeSet guarantees the order of elements when iterated
        List<wnIdPredictions> predResults;
        String wnId;
        Map<String, Float> predDict;

        // Loop for populating dictionary with all the WNIDs that will be handled
        for (Top_Predictions topPrediction : top_predictions) {
            predResults = topPrediction.getResult();

            for (wnIdPredictions wnIdPred: predResults) {
                wnId = wnIdPred.getWnId();

                if (!dictionary.contains(wnId)){
                    dictionary.add(wnId);
                }

            }

        }
        int percent = (int) (imagespath.length/10);
        if(percent < 1){
            percent=1;
        }
        int cantidadImgProcesadas = 0;
        double[] v1, v2;
        double v, v1_s, v2_s, corr;
        int d;


        for (int i=0; i < top_predictions.size(); i++){
            if(cantidadImgProcesadas==percent){
                mainActivityPresenter.growProgress();
                cantidadImgProcesadas=0;
//                LOGGER.info("getGrammaticalAffinity");
            }else {
                cantidadImgProcesadas+=1;
            }
            predResults = top_predictions.get(i).getResult();
            predDict = new TreeMap<>();

            for (wnIdPredictions wnIdPred: predResults)
                predDict.put(wnIdPred.getWnId(), wnIdPred.getPrediction());

            v1 = new double[dictionary.size()];

            // Get a vector v1 with all predictions for each WNID handled
            d = 0;
            for(String wnIdDict : dictionary){
                v = 0.0;
                if(predDict.containsKey(wnIdDict))
                    v = predDict.get(wnIdDict);
                v1[d++] = v;
            }

            // Normalize values in v1 by the sum of total
            v1_s=0;
            for (double v1_r : v1) v1_s += v1_r;
            for (int r=0;r<v1.length;r++) v1[r] /= v1_s;

            for(int j=0;j<top_predictions.size();j++){
                if(i!=j){
                    predResults = top_predictions.get(j).getResult();
                    predDict = new TreeMap<>();

                    for (wnIdPredictions wnIdPred: predResults)
                        predDict.put(wnIdPred.getWnId(), wnIdPred.getPrediction());

                    v2=new double[dictionary.size()];

                    d = 0;
                    for(String wnIdDict : dictionary){
                        v = 0.0;
                        if(predDict.containsKey(wnIdDict))
                            v = predDict.get(wnIdDict);
                        v2[d++] = v;
                    }

                    // Normalize values in v2 by the sum of total
                    v2_s=0;
                    for (double v2_r : v2) v2_s += v2_r;
                    for (int r=0;r<v2.length;r++) v2[r] /= v2_s;

                    corr = new PearsonsCorrelation().correlation(v2,v1);
                    corr = (corr + 1)/2.0; //Normalize output
                    if(corr < THRESHOLD_PROBABILITY_PREDICTION) corr = 0.0;
                }else{
                    corr=1;
                }
                result[i][j] = corr;
            }
        }
        return result;
    }

    // TODO: scope: CIEngine
    private double[][] getImageAffinity(List<float[]> embeddings) {
        double[][] result = new double[embeddings.size()][embeddings.size()];
        float [] embed;
        double[] v1, v2;
        double corr;
        int percent = (int) (imagespath.length/10);
        if(percent < 1){
            percent=1;
        }
        int cantidadImgProcesadas = 0;
        // TODO: loop O(n^2) -> loop O(N*(N-1)/2)
        for (int i=0; i < embeddings.size(); i++){
            if(cantidadImgProcesadas==percent){
                mainActivityPresenter.growProgress();
                cantidadImgProcesadas=0;
//                LOGGER.info("getImageAffinity");
            }else {
                cantidadImgProcesadas+=1;
            }
            embed = embeddings.get(i);
            v1 = new double[embed.length];

            for (int r=0;r<v1.length;r++) v1[r] = (double) embed[r];

            for(int j=0;j<embeddings.size();j++){
                if(i!=j){
                    embed = embeddings.get(j);
                    v2 = new double[embed.length];

                    for (int r=0;r<v2.length;r++) v2[r] = (double) embed[r];

                    corr = new PearsonsCorrelation().correlation(v2,v1);
                    corr = (corr + 1)/2.0; //Normalize output
                    if(corr < THRESHOLD_PROBABILITY_PREDICTION) corr = 0.0;
                }else{
                    corr=1;
                }
                result[i][j] = corr;
            }

        }
        return result;
    }


}