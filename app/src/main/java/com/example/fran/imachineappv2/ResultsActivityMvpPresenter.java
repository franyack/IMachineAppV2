package com.example.fran.imachineappv2;

import java.util.ArrayList;

/**
 * Created by fran on 25/05/18.
 */

public interface ResultsActivityMvpPresenter {

    void fillTvResults(ArrayList<String> vImages, ArrayList<Integer> vClusters);

    void showClustersResult(String resu);

    void folderGenerator(String pathFolder);

    void showFolderAlert(String pathFolder);
}
