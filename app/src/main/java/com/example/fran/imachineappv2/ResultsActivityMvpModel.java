package com.example.fran.imachineappv2;

import android.content.Context;

import java.util.ArrayList;

/**
 * Created by fran on 29/05/18.
 */

public interface ResultsActivityMvpModel {
    void showClustersResults(ArrayList<String> vImages, ArrayList<Integer> vClusters);

    void deleteResults(String pathFolderResult, ResultsActivityView resultsActivityView);

    void confirmResults(String pathFolderTemporary, ResultsActivityView resultsActivityView);
}
