package com.example.fran.imachineappv2;

import java.util.ArrayList;

/**
 * Created by fran on 29/05/18.
 */

public interface ResultsActivityMvpModel {
    void showClustersResults(ArrayList<String> vImages, ArrayList<Integer> vClusters);

    void folderGenerator(String pathFolder);

    void deleteResults(String pathFolderResult);

    void confirmResults(String pathFolderTemporary);
}
