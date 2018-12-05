package com.example.fran.imachineappv2;

import org.ejml.data.DMatrixRMaj;

/**
 * Created by fran on 24/05/18.
 */

public interface MainActivityMvpView {
    void showGalleryChosen(String result);

    void buttonChooseGalleryEnable(boolean b);

    void showWorkingText(String result);

    void clusterReady();

    void showFilesManagerActivity(String pathFolder);

    void growProgress();

    void callErrorToast(String s);

    void errorCopyingFiles();
}
