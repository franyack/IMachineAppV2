package com.example.fran.imachineappv2;

import android.widget.CheckBox;
import android.widget.ProgressBar;

import java.util.ArrayList;

/**
 * Created by fran on 24/05/18.
 */

public interface MainActivityMvpPresenter {

    void deleteClusterResultFolder();

    void chooseGallery(MainActivityView view);

    void showGalleryChosen(String path);

    void checkBoxClick(CheckBox chAllImages);

    void buttonChooseGalleryEnable(boolean b);

    boolean prepararImagenes(String s, CheckBox chAllImages);

    void alertBlackWindow(MainActivityView mainActivityView);

    void fillWorkingText();

    void showWorkingText(String setearTexto);

    void showProgressBarWorking(ProgressBar progressBarWorking);

    void procesarImagenes(MainActivityView mainActivityView);

    void clustersReady(ArrayList<String> vImages, ArrayList<Integer> vClusters);
}
