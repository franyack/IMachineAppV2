package com.example.fran.imachineappv2;

import android.content.Context;
import android.widget.CheckBox;

/**
 * Created by fran on 24/05/18.
 */

public interface MainActivityMvpModel {

    void deleteClusterResultFolder(String pathFolderResult, MainActivityView mainActivityView);

    void chooseGallery(MainActivityView view);

    void checkBoxClick(CheckBox chAllImages);

    int prepareImages(String path_chosen, CheckBox chAllImages, Context applicationContext);

    void fillWorkingText();

    void startImageProcess(MainActivityView mainActivityView);

    void folderGenerator(String pathFolder, MainActivityView mainActivityView, boolean keepWorking);

    boolean folderResultsExist(String pathFoldersResult);

    // List<String> getMclParameters();

    void checkNumberImages(MainActivityView mainActivityView);
}
