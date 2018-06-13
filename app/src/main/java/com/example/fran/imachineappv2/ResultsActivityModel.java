package com.example.fran.imachineappv2;

import android.os.Environment;

import com.codekidlabs.storagechooser.utils.FileUtil;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
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
    public void folderGenerator(String pathFolder) {
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
        presenter.showFolderAlert(pathFolder);
    }

    @Override
    public void deleteResults(String pathFolderResult) {
        File folder = new File(pathFolderResult);
        if (folder.exists()){
            try {
                FileUtils.deleteDirectory(folder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        presenter.backToMainActivity("");
    }

    @Override
    public void confirmResults(String pathFolderTemporary) {
        int i = 1;
        File srcFolder = new File(pathFolderTemporary);
        File dstFolder = new File(Environment.getExternalStorageDirectory() + File.separator + "IMachineAppResult" + i);
        while(dstFolder.exists()){
            dstFolder = new File(Environment.getExternalStorageDirectory() + File.separator + "IMachineAppResult" + ++i);
        }
        try {
            FileUtils.copyDirectory(srcFolder,dstFolder);
            FileUtils.deleteDirectory(srcFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        presenter.backToMainActivity(Environment.getExternalStorageDirectory() + File.separator + "IMachineAppResult" + i);
    }
}
