package com.example.fran.imachineappv2;

import android.app.AlertDialog;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import com.codekidlabs.storagechooser.StorageChooser;
import com.example.fran.imachineappv2.CIEngine.Classifier;
import com.example.fran.imachineappv2.CIEngine.MCLDenseEJML;
import com.example.fran.imachineappv2.CIEngine.TensorFlowImageClassifier;
import com.example.fran.imachineappv2.Utils.Imagenet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.ejml.data.DMatrixRMaj;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
    private static final String MODEL_PATH = "mobilenet_quant_v1_224.tflite";
    private static final String LABEL_PATH = "labels2.txt";
    private static final String WORDS_PATH = "words.txt";
    private static final String HIERARCHY_PATH = "wordnet.is_a.txt";
    private static final int INPUT_SIZE = 224;
    private static final int BATCH_SIZE = 1;
    private static final int PIXEL_SIZE = 3;

    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();

    private Imagenet wnid_lookup = new Imagenet();
//    private List<String> label_lookup = new ArrayList<>();


    ArrayList<String> vImages = new ArrayList<>();
    ArrayList<Integer> vClusters = new ArrayList<>();
    private List<Top_Predictions> top_predictions = new ArrayList<>();

    //Constructor
    MainActivityModel(MainActivityPresenter presenter) {
        this.mainActivityPresenter = presenter;
    }

    @Override
    public void deleteClusterResultFolder() {
        File folder = new File(Environment.getExternalStorageDirectory() +
                File.separator + "clusterResult");
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
    public boolean prepararImagenes(String path_chosen, CheckBox chAllImages) {
        File curDir;
        if (path_chosen == null && !chAllImages.isChecked()){
            return false;
        }
        if (chAllImages.isChecked()){
            curDir = new File("/storage/emulated/0");
        }else{
            assert path_chosen != null;
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
        return true;
    }

    private void getAllFiles(File curDir){
        File[] filesList = curDir.listFiles();
        for(File f : filesList){
            if(f.isDirectory()) {
                getAllFiles(f);
            }else {
                if(f.isFile()){
                    if (images.size()>=100){
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
    public void alertBlackWindow(MainActivityView mainActivityView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivityView);
        builder.setTitle("Atención!");
        builder.setIcon(R.drawable.warning_black);
        builder.setMessage("Este proceso puede ocasionar que la pantalla se ponga en negro durante unos segundos.\n\nAguarde por favor.");
        final AlertDialog dialog = builder.create();
        dialog.show();
        Window window = dialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.BOTTOM;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(wlp);
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
        handler.postDelayed(runnable, 5000);
    }

    @Override
    public void fillWorkingText() {
        String setearTexto = "Procesando " + images.size() +  " imagenes, aguarde por favor…";
        mainActivityPresenter.showWorkingText(setearTexto);
    }

    @Override
    public void startImageProcess(MainActivityView mainActivityView) {
        try {
            wnid_lookup.wnidWordsList = wnid_lookup.loadWnIDWords(mainActivityView.getAssets(),WORDS_PATH);
//            label_lookup = TensorFlowImageClassifier.loadLabelList(mainActivityView.getAssets(),LABEL_PATH);
            wnid_lookup.hierarchyLookupList = wnid_lookup.loadHierarchy_lookup(mainActivityView.getAssets(),HIERARCHY_PATH);
            initTensorFlowAndLoadModel(mainActivityView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //    @Override
    public void processImages() {
        for (int i = 0; i< imagespath.length; i++){
            Bitmap image = lessResolution(imagespath[i],INPUT_SIZE,INPUT_SIZE);
            image = Bitmap.createScaledBitmap(image,INPUT_SIZE,INPUT_SIZE,false);
            ByteBuffer byteBuffer;
            if (image != null){
                byteBuffer = convertBitmapToByteBuffer(image);
                final List<Classifier.Recognition> results = classifier.recognizeImage(byteBuffer);
                byteBuffer.clear();
                List<wnIdPredictions> wnIdPredictionsList = new ArrayList<>();
                if (results.size() == 0){
                    wnIdPredictions entity = new wnIdPredictions("n00001740",1);
                    wnIdPredictionsList.add(entity);
                }else{
                    wnIdPredictionsList = process_top_predictions(results,4,0.05);
                }
                top_predictions.add(new Top_Predictions(imagespath[i], wnIdPredictionsList));

                LOGGER.info(imagespath[i]);
            for (int j=0;j<wnIdPredictionsList.size();j++){
                String word;
                word = wnid_lookup.get_label_from_wnid(wnIdPredictionsList.get(j).getWnId(),wnid_lookup.wnidWordsList);
                LOGGER.info(word+": "+wnIdPredictionsList.get(j).getPrediction());
            }
            LOGGER.info("                                                                                ");
            }
        }
        double[][] g_aff_matrix;
        g_aff_matrix = get_grammatical_affinity(top_predictions);
        DMatrixRMaj cluster_matrix = new DMatrixRMaj(g_aff_matrix);

        int maxIt = 100;
        int expPow = 2;
        int infPow = 2;
        double epsConvergence = 1e-3;
        double threshPrune = 0.01;
        int n = 100;
        int seed = 1234;

        MCLDenseEJML mcl = new MCLDenseEJML(maxIt, expPow, infPow, epsConvergence, threshPrune);


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
        mainActivityPresenter.clustersReady(vImages,vClusters);
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
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(BATCH_SIZE * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < INPUT_SIZE; ++i) {
            for (int j = 0; j < INPUT_SIZE; ++j) {
                final int val = intValues[pixel++];
                byteBuffer.put((byte) ((val >> 16) & 0xFF));
                byteBuffer.put((byte) ((val >> 8) & 0xFF));
                byteBuffer.put((byte) (val & 0xFF));
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

    private double[][] get_grammatical_affinity(List<Top_Predictions> top_predictions) {
        double[][] result = new double[top_predictions.size()][top_predictions.size()];
        List<String> dictionary = new ArrayList<>();
        boolean add;
        int d;
        for (int i=0;i<top_predictions.size();i++){
            for (int j=0; j<top_predictions.get(i).getResult().size();j++){
                add=true;
                if(dictionary.size() == 0){
                    dictionary.add(top_predictions.get(i).getResult().get(j).getWnId());
                }else{
                    d=0;
                    while (d<dictionary.size()){
                        if(dictionary.get(d).equals(top_predictions.get(i).getResult().get(j).getWnId())){
                            add=false;
                            break;
                        }
                        d+=1;
                    }
                    if(add){
                        dictionary.add(top_predictions.get(i).getResult().get(j).getWnId());
                    }
                }
            }
        }



        double[] aff_row;
        double[] v1,v2;
        double v1_s,v2_s;
        double corr;
        for(int i =0;i<top_predictions.size();i++){
            aff_row = new double[top_predictions.size()];
            corr=0;
            v1 = new double[dictionary.size()];
            for(int j=0;j<top_predictions.get(i).getResult().size();j++){
                d=0;
                while(d<dictionary.size()){
                    if(dictionary.get(d).equals(top_predictions.get(i).getResult().get(j).getWnId())){
                        v1[d]=top_predictions.get(i).getResult().get(j).getPrediction();
                    }
                    d+=1;
                }
            }
            v1_s=0;
            for (int r=0;r<v1.length;r++){
                v1_s += v1[r];
            }
            for (int r=0;r<v1.length;r++){
                v1[r] = v1[r]/v1_s;
            }
            for(int k=0;k<top_predictions.size();k++){
                v2= new double[dictionary.size()];
                for(int j=0;j<top_predictions.get(k).getResult().size();j++){
                    d=0;
                    while(d<dictionary.size()){
                        if(dictionary.get(d).equals(top_predictions.get(k).getResult().get(j).getWnId())){
                            v2[d]=top_predictions.get(k).getResult().get(j).getPrediction();
                        }
                        d+=1;
                    }
                }
                v2_s=0;
                for (int r=0;r<v2.length;r++){
                    v2_s += v2[r];
                }
                for (int r=0;r<v2.length;r++){
                    v2[r] = v2[r]/v2_s;
                }
                corr = new PearsonsCorrelation().correlation(v2,v1);
                corr = (corr + 1)/2.0; //Normalize output
                if(corr>0.65){
                    corr = corr;
                }else{
                    corr = 0;
                }
                aff_row[k]=corr;
            }
            for(int j=0;j<top_predictions.size();j++){
                result[i][j]=aff_row[j];
            }
        }
        return result;
    }


}
