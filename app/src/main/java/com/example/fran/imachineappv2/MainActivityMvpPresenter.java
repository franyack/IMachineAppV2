package com.example.fran.imachineappv2;

import android.content.Context;
import android.widget.CheckBox;
import android.widget.ProgressBar;

import java.util.List;

/**
 * Created by fran on 24/05/18.
 */

public interface MainActivityMvpPresenter {

    void deleteClusterResultFolder(String pathFolderResult, MainActivityView mainActivityView);

    void chooseGallery(MainActivityView view);

    void showGalleryChosen(String path);

    void checkBoxClick(CheckBox chAllImages);

    void buttonChooseGalleryEnable(boolean b);

    int prepararImagenes(String s, CheckBox chAllImages, Context applicationContext);

    void fillWorkingText();

    void showWorkingText(String setearTexto);

    void showProgressBarWorking(ProgressBar progressBarWorking);

    void procesarImagenes(MainActivityView mainActivityView);

    void clustersReady();

    void folderGenerator(String pathFolder, MainActivityView mainActivityView);

    void showFilesManager(String pathFolder);

    boolean folderResultsExists(String pathFoldersResult);

    void growProgress();

    List<String> getMclParameters();

    void callErrorToast(String s);

}
