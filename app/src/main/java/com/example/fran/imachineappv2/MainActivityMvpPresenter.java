package com.example.fran.imachineappv2;

import android.content.Context;
import android.widget.CheckBox;
import android.widget.ProgressBar;

/**
 * Created by fran on 24/05/18.
 */

public interface MainActivityMvpPresenter {

    void deleteClusterResultFolder(String pathFolderResult, MainActivityView mainActivityView);

    void chooseGallery(MainActivityView view);

    void showGalleryChosen(String path);

    void checkBoxClick(CheckBox chAllImages);

    void buttonChooseGalleryEnable(boolean b);

    int prepareImages(String s, CheckBox chAllImages, Context applicationContext);

    void fillWorkingText();

    void showWorkingText(String setearTexto);

    void showProgressBarWorking(ProgressBar progressBarWorking);

    void processImages(MainActivityView mainActivityView);

    void clustersReady();

    void folderGenerator(String pathFolder, MainActivityView mainActivityView);

    void showFilesManager(String pathFolder);

    boolean folderResultsExists(String pathFoldersResult);

    // TODO: deprecate. used reportProgress instead
    @Deprecated
    void growProgress();

    void reportProgress(final int p);

    // Map<String, Number> getMCLParameters();

    void callErrorToast(String s);

    void checkNumberImages(MainActivityView mainActivityView);

    void errorCopyingFiles();
}
