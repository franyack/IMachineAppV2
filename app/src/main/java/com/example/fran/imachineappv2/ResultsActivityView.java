package com.example.fran.imachineappv2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * Created by fran on 02/04/18.
 */

public class ResultsActivityView extends Activity implements ResultsActivityMvpView {

    private static final Logger LOGGER = Logger.getLogger(ResultsActivityView.class.getName());

    private ResultsActivityMvpPresenter presenter;

    TextView tvResults;

    ArrayList<String> vImages = new ArrayList<>();
    ArrayList<Integer> vClusters = new ArrayList<>();
    Vector<Integer> vClustersResult = new Vector<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGGER.info("Hello World!");
        presenter = new ResultsActivityPresenter(this);

        vImages = (ArrayList<String>) getIntent().getSerializableExtra("vImages");
        vClusters = (ArrayList<Integer>) getIntent().getSerializableExtra("vClusters");
        setContentView(R.layout.results);
        tvResults = (TextView) findViewById(R.id.resultados);
        presenter.fillTvResults(vImages,vClusters);
    }

    public void showClustersResult(String result){tvResults.setText(result);}

    public void generarCarpetas(View view) {
        String pathFolder = Environment.getExternalStorageDirectory() + File.separator + "clusterResult";
        presenter.folderGenerator(pathFolder);
    }
    public void showFolderAlert(String pathFolder){
        AlertDialog.Builder alert = new AlertDialog.Builder(ResultsActivityView.this);
        alert.setTitle("Proceso finalizado!");
        alert.setMessage("La carpeta fue creada en la siguiente ubicación:\n\n" + pathFolder);
        alert.setPositiveButton("OK", null);
        alert.show();
    }

    public void volverMainActivity(View view) {
        Intent i = getBaseContext().getPackageManager()
                .getLaunchIntentForPackage(getBaseContext().getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }
}
