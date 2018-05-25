package com.example.fran.imachineappv2;

import android.widget.CheckBox;

/**
 * Created by fran on 24/05/18.
 */

public interface DataManagerMvp {

    void deleteClusterResultFolder();

    void chooseGallery(MainActivityView view);

    void checkBoxClick(CheckBox chAllImages);

    boolean prepararImagenes(String path_chosen, CheckBox chAllImages);

    void alertBlackWindow(MainActivityView mainActivityView);

    void fillWorkingText();

    void procesarImagenes(MainActivityView mainActivityView);
}
