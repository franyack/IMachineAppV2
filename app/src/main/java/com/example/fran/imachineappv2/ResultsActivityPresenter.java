package com.example.fran.imachineappv2;

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
    public void confirmResults(String pathFolder, ResultsActivityView resultsActivityView) {
        resultsActivityModel.confirmResults(pathFolder,resultsActivityView);
    }

    @Override
    public void deleteResults(String pathFolder, ResultsActivityView resultsActivityView) {
        resultsActivityModel.deleteResults(pathFolder, resultsActivityView);
    }

    @Override
    public void backToMainActivity(String dstFolder) {
        view.backToMainActivity(dstFolder);
    }

    @Override
    public void notSufficientStorage() {
        view.notSufficientStorage();
    }
}
