package com.example.fran.imachineappv2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Created by fran on 29/05/18.
 */

public class ResultsActivityModel implements ResultsActivityMvpModel {

    private ResultsActivityMvpPresenter presenter;

    ArrayList<String> vImages = new ArrayList<>();
    ArrayList<Integer> vClusters = new ArrayList<>();
    Vector<Integer> vClustersResult = new Vector<>();

    public ResultsActivityModel(ResultsActivityPresenter resultsActivityPresenter) {
        this.presenter = resultsActivityPresenter;
    }

    @Override
    public void showClustersResults(ArrayList<String> vImages, ArrayList<Integer> vClusters) {
        this.vImages = vImages;
        this.vClusters = vClusters;
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
                if (vClustersCopy.get(0).equals(vClustersCopy.get(i))) {
                    cant++;
                    vClustersCopy.remove(i);
                    i -= 1;
                }
            }
            vClustersResult.add(cant);
            vClustersCopy.remove(0);
        }

        String resu="";
        for (int i=0;i<vClustersResult.size();i++){
            resu=resu+"\n"+"Cluster "+i+": "+vClustersResult.get(i)+" image/s";
        }


    }

    @Override
    public void deleteResults(String pathFolderResult, final ResultsActivityView resultsActivityView) {
        File folder = new File(pathFolderResult);
        if (folder.exists()){
            try {
                FileUtils.deleteDirectory(folder);
                //You need to tell the media scanner about the new file so that it is immediately available to the user.
                Intent mediaScannerIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri fileContentUri = Uri.fromFile(folder);
                mediaScannerIntent.setData(fileContentUri);
                resultsActivityView.sendBroadcast(mediaScannerIntent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        presenter.backToMainActivity("");
    }

    @Override
    public void confirmResults(final String pathFolderTemporary, final ResultsActivityView resultsActivityView) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(resultsActivityView);
        builder.setTitle(R.string.attention);
        builder.setMessage(R.string.wishcopyormove);
        builder.setPositiveButton(R.string.label_copy, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                generateNewResultFolder(pathFolderTemporary,resultsActivityView);
            }
        });
        builder.setNeutralButton(R.string.label_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        builder.setNegativeButton(R.string.label_move, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                File file;
                List<String> imagesPath;
                imagesPath = readFromFile(resultsActivityView.getApplicationContext());
                for(int j=0;j<imagesPath.size();j++){
                    file = new File(imagesPath.get(j));
                    try {
                        FileUtils.forceDelete(file);
//                        MediaScannerConnection.scanFile(resultsActivityView, new String[] {file.toString()},null,null);
                        Intent mediaScannerIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        Uri fileContentUri = Uri.fromFile(file);
                        mediaScannerIntent.setData(fileContentUri);
                        resultsActivityView.sendBroadcast(mediaScannerIntent);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                generateNewResultFolder(pathFolderTemporary,resultsActivityView);
            }
        });
        final AlertDialog dialog=builder.create();
        dialog.show();
    }

    private List<String> readFromFile(Context context) {
        List<String> imagesPath = new ArrayList<>();
        try {
            InputStream inputStream = context.openFileInput("imagespath.txt");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString;
                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    imagesPath.add(receiveString);
                }

                inputStream.close();
                FileUtils.forceDelete(context.getFileStreamPath("imagespath.txt"));
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return imagesPath;
    }

    public void generateNewResultFolder(String pathFolderTemporary, ResultsActivityView resultsActivityView){
        int i = 1;
        File srcFolder = new File(pathFolderTemporary);
        File dstFolder = new File(Environment.getExternalStorageDirectory() + File.separator + "IMachineAppResult" + i);
        while(dstFolder.exists()){
            dstFolder = new File(Environment.getExternalStorageDirectory() + File.separator + "IMachineAppResult" + ++i);
        }
        try {
            FileUtils.copyDirectory(srcFolder,dstFolder);
            FileUtils.deleteDirectory(srcFolder);
            //You need to tell the media scanner about the new file so that it is immediately available to the user.
            for(File file:dstFolder.listFiles()){
                if(file.isDirectory()){
                    for(File f:file.listFiles()){
//                        MediaScannerConnection.scanFile(resultsActivityView, new String[] {f.toString()},null,null);
                        Intent mediaScannerIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        Uri fileContentUri = Uri.fromFile(f);
                        mediaScannerIntent.setData(fileContentUri);
                        resultsActivityView.sendBroadcast(mediaScannerIntent);
                    }
                }else{
//                    MediaScannerConnection.scanFile(resultsActivityView, new String[] {file.toString()},null,null);
                    Intent mediaScannerIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    Uri fileContentUri = Uri.fromFile(file);
                    mediaScannerIntent.setData(fileContentUri);
                    resultsActivityView.sendBroadcast(mediaScannerIntent);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        presenter.backToMainActivity(Environment.getExternalStorageDirectory() + File.separator + "IMachineAppResult" + i);
    }
}
