package com.example.fran.imachineappv2;

import java.util.ArrayList;

/**
 * Created by fran on 25/05/18.
 */

public class ResultsActivityPresenter implements ResultsActivityMvpPresenter {

    private ResultsActivityMvpView view;
    private ResultsActivityMvpModel resultsActivityModel;

    public ResultsActivityPresenter(ResultsActivityView resultsActivityView) {
        view = resultsActivityView;
        resultsActivityModel = new ResultsActivityModel(this);
    }


    @Override
    public void fillTvResults(ArrayList<String> vImages, ArrayList<Integer> vClusters) {
        resultsActivityModel.showClustersResults(vImages,vClusters);
    }

    @Override
    public void showClustersResult(String resu) {
        view.showClustersResult(resu);
    }

    @Override
    public void folderGenerator(String pathFolder) {
        resultsActivityModel.folderGenerator(pathFolder);
    }

    @Override
    public void showFolderAlert(String pathFolder) {
        view.showFolderAlert(pathFolder);
    }
}
