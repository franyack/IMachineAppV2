package com.example.fran.imachineappv2;

import java.util.ArrayList;

/**
 * Created by fran on 25/05/18.
 */

public class ResultsActivityPresenter implements ResultsActivityMvpPresenter {

    private ResultsActivityMvpView view;
    private DataManagerMvp dataManager;

    public ResultsActivityPresenter(ResultsActivityView resultsActivityView) {
        view = resultsActivityView;
        dataManager = new DataManager(this);
    }


    @Override
    public void fillTvResults(ArrayList<String> vImages, ArrayList<Integer> vClusters) {
        dataManager.showClustersResults(vImages,vClusters);
    }

    @Override
    public void showClustersResult(String resu) {
        view.showClustersResult(resu);
    }

    @Override
    public void folderGenerator(String pathFolder) {
        dataManager.folderGenerator(pathFolder);
    }

    @Override
    public void showFolderAlert(String pathFolder) {
        view.showFolderAlert(pathFolder);
    }
}
