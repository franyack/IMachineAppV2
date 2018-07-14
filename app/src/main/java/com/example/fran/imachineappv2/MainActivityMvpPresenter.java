package com.example.fran.imachineappv2;

import android.widget.CheckBox;
import android.widget.ProgressBar;

import java.util.List;

/**
 * Created by fran on 24/05/18.
 */

public interface MainActivityMvpPresenter {

    void deleteClusterResultFolder(String pathFolderResult);

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

    void clustersReady(String[] imagespath);

    void folderGenerator(String pathFolder);

    void showFilesManager(String pathFolder);

    boolean folderResultsExists(String pathFoldersResult);

    void growProgress();

    List<String> getMclParameters();
}
