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
    CheckBox chAllImages;
    Button btnChooseGallery;
    TextView workingText;
    ProgressBar progressBarWorking;
    String pathFoldersResult;
    NumberProgressBar numberProgressBar;
    int progress=0;
    private static final Logger LOGGER = Logger.getLogger(MainActivityView.class.getName());

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
        chAllImages = (CheckBox) findViewById(R.id.checkTodasLasImagenes);
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

    private void deleteClusterResultFolder(String pathFoldersResult) {presenter.deleteClusterResultFolder(pathFoldersResult);}

    public void chooseGallery(View view) {presenter.chooseGallery(MainActivityView.this);}

    public void showGalleryChosen(String result){path_chosen.setText(result);}

    public void buttonChooseGalleryEnable(boolean enable){btnChooseGallery.setEnabled(enable);}

    public void checkBoxClick(View view) {presenter.checkBoxClick(chAllImages);}

    public void showWorkingText(String result){workingText.setText(result);}

    public void procesarImagenes(View view) {

        deleteClusterResultFolder(pathFoldersResult);

        if (!presenter.prepararImagenes((String) path_chosen.getText(),chAllImages)){
            //TODO: Strings para los toast
            Toast.makeText(getApplicationContext(),"Debe seleccionar un directorio a procesar", Toast.LENGTH_SHORT).show();
            return;
        }

        setContentView(R.layout.working);

        workingText = (TextView) findViewById(R.id.workingTexto);

        presenter.fillWorkingText();

        presenter.procesarImagenes(MainActivityView.this);
    }

    @Override
    public void clusterReady() {
        presenter.folderGenerator(pathFoldersResult);
    }

    public void showFilesManagerActivity(String pathFolder){
        Intent i = new Intent(this, FilesMainActivity.class);
        i.putExtra("pathFolder",pathFolder);
        startActivity(i);

    }

    @Override
    public void growProgress() {
        progress+=2.5;
        if(progress>=100){
            progress=95;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                numberProgressBar = (NumberProgressBar) findViewById(R.id.number_progress_bar);
                numberProgressBar.setProgress(progress);
            }
        });
    }

    public void verResultadosAnteriores(View view) {

        if(!presenter.folderResultsExists(pathFoldersResult)){
            Toast.makeText(getApplicationContext(),"¡No existen resultados anteriores!", Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> mclParameters = presenter.getMclParameters();
        String pathFolderChosen = Environment.getExternalStorageDirectory() + File.separator + "Models";
        Metrics a = new Metrics(pathFolderChosen, pathFoldersResult, mclParameters);
        a.Metrics();
        Intent i = new Intent(this, FilesMainActivity.class);
        i.putExtra("pathFolder",pathFoldersResult);
        startActivity(i);
    }

}
