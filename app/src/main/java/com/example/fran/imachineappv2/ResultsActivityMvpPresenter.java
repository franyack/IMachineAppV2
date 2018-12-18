package com.example.fran.imachineappv2;

/**
 * Created by fran on 25/05/18.
 */

public interface ResultsActivityMvpPresenter {

    void confirmResults(String pathFolder, ResultsActivityView resultsActivityView);

    void deleteResults(String pathFolder, ResultsActivityView resultsActivityView);

    void backToMainActivity(String i);

    void notSufficientStorage();
}
