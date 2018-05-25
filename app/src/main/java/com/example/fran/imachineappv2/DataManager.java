package com.example.fran.imachineappv2;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Handler;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;

import com.codekidlabs.storagechooser.StorageChooser;

import org.apache.commons.io.FileUtils;
import org.ejml.data.DMatrixRMaj;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by fran on 24/05/18.
 */

public class DataManager implements  DataManagerMvp {

    private MainActivityMvpPresenter presenter;
    String[] imagespath;
    Vector<String> images = new Vector<>();

    private static final String MODEL_PATH = "mobilenet_quant_v1_224.tflite";
    private static final String LABEL_PATH = "labels2.txt";
    private static final String WORDS_PATH = "words.txt";
    private static final String HIERARCHY_PATH = "wordnet.is_a.txt";
    private static final int INPUT_SIZE = 224;

    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();

    imagenet wnid_lookup = new imagenet();
    List<String> label_lookup = new ArrayList<>();

    Vector<String> vImages = new Vector<>();
    Vector<Integer> vClusters = new Vector<>();

    public DataManager(MainActivityPresenter mainActivityPresenter) {
        presenter = mainActivityPresenter;
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
                presenter.showGalleryChosen(path);
            }
        });
    }

    @Override
    public void checkBoxClick(CheckBox chAllImages) {
        if (chAllImages.isChecked()){
            presenter.buttonChooseGalleryEnable(false);
        }else{
            presenter.buttonChooseGalleryEnable(true);
        }
        presenter.showGalleryChosen("");
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
            curDir = new File((String) path_chosen);
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
        presenter.showWorkingText(setearTexto);
    }

    @Override
    public void procesarImagenes(MainActivityView mainActivityView) {
        try {
            wnid_lookup.wnidWordsList = wnid_lookup.loadWnIDWords(mainActivityView.getAssets(),WORDS_PATH);
            label_lookup = TensorFlowImageClassifier.loadLabelList(mainActivityView.getAssets(),LABEL_PATH);
            wnid_lookup.hierarchyLookupList = wnid_lookup.loadHierarchy_lookup(mainActivityView.getAssets(),HIERARCHY_PATH);
        } catch (IOException e) {
            e.printStackTrace();
        }

        initTensorFlowAndLoadModel(mainActivityView);

    }

    private void initTensorFlowAndLoadModel(final MainActivityView mainActivityView) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = TensorFlowImageClassifier.create(
                            mainActivityView.getAssets(),
                            MODEL_PATH,
                            LABEL_PATH,
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
    }
}
