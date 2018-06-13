package com.example.fran.imachineappv2;

import java.util.ArrayList;

/**
 * Created by fran on 25/05/18.
 */

public interface ResultsActivityMvpPresenter {

    void fillTvResults(ArrayList<String> vImages, ArrayList<Integer> vClusters);

    void folderGenerator(String pathFolder);

    void showFolderAlert(String pathFolder);

    void confirmResults(String pathFolder);

    void deleteResults(String pathFolder);

    void backToMainActivity(String i);
}
