package com.example.fran.imachineappv2;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.numberprogressbar.NumberProgressBar;
import com.example.fran.imachineappv2.FilesManager.FilesMainActivity;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

public class MainActivityView extends AppCompatActivity implements MainActivityMvpView {

    private MainActivityMvpPresenter presenter;

    TextView path_chosen;
    CheckBox checkCameraImages;
    Button btnChooseGallery;
    TextView workingText;
    ProgressBar progressBarWorking;  // TODO: used?
    String pathFoldersResult;
    NumberProgressBar numberProgressBar;
    int progress=0;
    private static final Logger LOGGER = Logger.getLogger(MainActivityView.class.getName()); // TODO: used?

    private static final String[] INITIAL_PERMS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final int INITIAL_REQUEST = 1337;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        presenter = new MainActivityPresenter(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(INITIAL_PERMS, INITIAL_REQUEST);
        }
        pathFoldersResult = Environment.getExternalStorageDirectory() + File.separator + "IMachineAppTemporaryResults";
        path_chosen = (TextView) findViewById(R.id.path_chosen);
        checkCameraImages = (CheckBox) findViewById(R.id.checkCameraImages);
        btnChooseGallery = (Button) findViewById(R.id.btnCarpetaProcesar);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void deleteClusterResultFolder(String pathFoldersResult, MainActivityView mainActivityView) {presenter.deleteClusterResultFolder(pathFoldersResult, mainActivityView);}

    public void chooseGallery(View view) {presenter.chooseGallery(MainActivityView.this);}

    public void showGalleryChosen(String result){path_chosen.setText(result);}

    public void buttonChooseGalleryEnable(boolean enable){btnChooseGallery.setEnabled(enable);}

    public void checkBoxClick(View view) {presenter.checkBoxClick(checkCameraImages);}

    public void showWorkingText(String result){
        String text = getString(R.string.processing) + " " + result + " " + getString(R.string.images);
        workingText.setText(text);
    }

    public void processImages(View view) {

        deleteClusterResultFolder(pathFoldersResult, MainActivityView.this);

        int returnCode = presenter.prepareImages((String) path_chosen.getText(),
                checkCameraImages, getApplicationContext());

        if (returnCode != MainActivityModel.OK_FOLDER_READY) {

            switch (returnCode) {
                case MainActivityModel.ERROR_NO_SELECTION:
                    Toast.makeText(getApplicationContext(), getString(R.string.noneselected), Toast.LENGTH_SHORT).show();
                    break;
                case MainActivityModel.ERROR_FOLDER_IS_EMPTY:
                    Toast.makeText(getApplicationContext(), getString(R.string.thefolderisempty), Toast.LENGTH_SHORT).show();
                    break;
                case MainActivityModel.ERROR_INSUFFICIENT_STORAGE:
                    Toast.makeText(getApplicationContext(), getString(R.string.insufficientsizeprocess), Toast.LENGTH_SHORT).show();
                    break;
            }
            return;
        }

        presenter.checkNumberImages(MainActivityView.this);

        setContentView(R.layout.working);

        workingText = (TextView) findViewById(R.id.workingTexto);

        presenter.fillWorkingText();

        presenter.processImages(MainActivityView.this);
    }

    @Override
    public void clusterReady() {
//        this.imagesPath=new String[imagespath.length];
        presenter.folderGenerator(pathFoldersResult, MainActivityView.this);
    }

    public void showFilesManagerActivity(String pathFolder){
        Intent i = new Intent(this, FilesMainActivity.class);
        i.putExtra("pathFolder",pathFolder);
        startActivity(i);
    }

    @Override
    // TODO: deprecated. use reportProgress instead
    public void growProgress() {
        // TODO: receive delta to add
        // TODO: improve this!
        progress+=2.5;
        if(progress>=100){
            progress=95;  // TODO: why?
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                numberProgressBar = (NumberProgressBar) findViewById(R.id.number_progress_bar);
                numberProgressBar.setProgress(progress);
            }
        });
    }

    @Override
    public void reportProgress(final int p) {
        // TODO: check why the bar is being reset
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                numberProgressBar = findViewById(R.id.number_progress_bar);
                numberProgressBar.setProgress(p);
            }
        });
    }

    @Override
    public void callErrorToast(String s) {
        Toast.makeText(getApplicationContext(),s, Toast.LENGTH_SHORT).show();
        Intent i = getBaseContext().getPackageManager()
                .getLaunchIntentForPackage(getBaseContext().getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }


    public void verResultadosAnteriores(View view) {

        if(!presenter.folderResultsExists(pathFoldersResult)){
            Toast.makeText(getApplicationContext(),getString(R.string.previousresultsnotexists), Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> mclParameters = presenter.getMclParameters();
        String pathFolderChosen = Environment.getExternalStorageDirectory() + File.separator + "Models";
        // TODO: remove this entry point for metrics, and put it into a different class
//        Metrics a = new Metrics(pathFolderChosen, pathFoldersResult, mclParameters);
//        a.getScore();
        Intent i = new Intent(this, FilesMainActivity.class);
        i.putExtra("pathFolder",pathFoldersResult);
        startActivity(i);
    }

}
