package com.example.fran.imachineappv2;

import android.widget.CheckBox;

import java.util.List;

/**
 * Created by fran on 24/05/18.
 */

public interface MainActivityMvpModel {

    void deleteClusterResultFolder(String pathFolderResult);

    void chooseGallery(MainActivityView view);

    void checkBoxClick(CheckBox chAllImages);

    boolean prepararImagenes(String path_chosen, CheckBox chAllImages);

    void alertBlackWindow(MainActivityView mainActivityView);

    void fillWorkingText();

    void startImageProcess(MainActivityView mainActivityView);

    void folderGenerator(String pathFolder);

    boolean folderResultsExist(String pathFoldersResult);

    List<String> getMclParameters();
}
