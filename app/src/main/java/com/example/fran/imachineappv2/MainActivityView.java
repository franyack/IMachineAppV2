package com.example.fran.imachineappv2;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivityView extends AppCompatActivity implements MainActivityMvpView {

    private MainActivityMvpPresenter presenter;

    TextView path_chosen;
    CheckBox chAllImages;
    Button btnChooseGallery;
    TextView workingText;
    ProgressBar progressBarWorking;

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

        deleteClusterResultFolder();

        path_chosen = (TextView) findViewById(R.id.path_chosen);
        chAllImages = (CheckBox) findViewById(R.id.checkTodasLasImagenes);
        btnChooseGallery = (Button) findViewById(R.id.btnCarpetaProcesar);
        progressBarWorking = (ProgressBar) findViewById(R.id.pb_working);
    }

    private void deleteClusterResultFolder() {presenter.deleteClusterResultFolder();}

    public void chooseGallery(View view) {presenter.chooseGallery(MainActivityView.this);}

    public void showGalleryChosen(String result){path_chosen.setText(result);}

    public void buttonChooseGalleryEnable(boolean enable){btnChooseGallery.setEnabled(enable);}

    public void checkBoxClick(View view) {presenter.checkBoxClick(chAllImages);}

    public void showWorkingText(String result){workingText.setText(result);}

    public void procesarImagenes(View view) {

//        presenter.procesarImagenes();
        if (!presenter.prepararImagenes(path_chosen.toString(),chAllImages)){
            Toast.makeText(getApplicationContext(),"Debe seleccionar un directorio a procesar", Toast.LENGTH_SHORT).show();
            return;
        }

        presenter.alertBlackWindow(MainActivityView.this);

        setContentView(R.layout.working);

        workingText = (TextView) findViewById(R.id.workingTexto);

        presenter.fillWorkingText();

        presenter.procesarImagenes(MainActivityView.this);

    }

    @Override
    public void clusterReady() {
        Intent i = new Intent(this, ResultsActivityView.class);
        startActivity(i);
    }




}
