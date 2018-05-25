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

public class ResultsActivityView extends Activity {

    private static final Logger LOGGER = Logger.getLogger(ResultsActivityView.class.getName());


    ArrayList<String> vImages = new ArrayList<>();
    ArrayList<Integer> vClusters = new ArrayList<>();
    Vector<Integer> vClustersResult = new Vector<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGGER.info("Hello World!");
//        vImages = (ArrayList<String>) getIntent().getSerializableExtra("vImages");
//        vClusters = (ArrayList<Integer>) getIntent().getSerializableExtra("vClusters");
//        obtenerClusters(vImages,vClusters);

    }

    private void obtenerClusters(ArrayList<String> vImages, ArrayList<Integer> vClusters) {
        ArrayList<Integer> vClustersCopy = new ArrayList<Integer>(vClusters);
        if (vClustersResult.size()>0){
            vClustersResult.clear();
        }
        int cant;
        while (!vClustersCopy.isEmpty()) {
            cant = 1;
            for (int i = 1; i < vClustersCopy.size(); i++) {
                if (vClustersCopy.get(i) == null) {
                    break;
                }
                if (vClustersCopy.get(0) == vClustersCopy.get(i)) {
                    cant++;
                    vClustersCopy.remove(i);
                    i -= 1;
                }
            }
            vClustersResult.add(cant);
            vClustersCopy.remove(0);
        }

        mostrarResultados(vClustersResult);
    }


    private void mostrarResultados(Vector<Integer> vClustersResult) {
        String resu="";
        for (int i=0;i<vClustersResult.size();i++){
            resu=resu+"\n"+"Cluster "+i+": "+vClustersResult.get(i)+" image/s";
        }

        setContentView(R.layout.results);
        TextView res = findViewById(R.id.resultados);
        res.setText(resu);
    }

    public void generarCarpetas(View view) {
        String pathFolder = Environment.getExternalStorageDirectory() + File.separator + "clusterResult";
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
        for (int i = 0; i < vClustersResult.size(); i++) {
            folder = new File(Environment.getExternalStorageDirectory() +
                    File.separator + "clusterResult" + File.separator + "Cluster" + i);
            folder.mkdirs();
            for (int j = 0; j < vClusters.size(); j++) {
                if (vClusters.get(j) == i) {
                    File source = new File(vImages.get(j));
                    File destination = new File(folder.getAbsolutePath() + File.separator + "image" + j + ".jpg");
                    FileChannel src = null;
                    FileChannel dst = null;
                    try {
                        src = new FileInputStream(source).getChannel();
                        dst = new FileOutputStream(destination).getChannel();
                        dst.transferFrom(src, 0, src.size());
                        src.close();
                        dst.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
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
