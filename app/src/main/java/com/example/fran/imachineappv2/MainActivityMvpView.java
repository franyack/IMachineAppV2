package com.example.fran.imachineappv2;

/**
 * Created by fran on 24/05/18.
 */

public interface MainActivityMvpView {
    void showGalleryChosen(String result);

    void buttonChooseGalleryEnable(boolean b);

    void showWorkingText(String result);

    void clusterReady();
}
