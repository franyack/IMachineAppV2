package com.example.fran.imachineappv2;

import android.content.Context;
import android.widget.CheckBox;
import android.widget.ProgressBar;

/**
 * Created by fran on 24/05/18.
 */

public class MainActivityPresenter implements MainActivityMvpPresenter {

    private MainActivityMvpView view;
    private MainActivityMvpModel dataManager;

    public MainActivityPresenter(MainActivityView mainActivityView) {
        view = mainActivityView;
        dataManager = new MainActivityModel(this);
    }


    @Override
    public void deleteClusterResultFolder(String pathFolderResult, MainActivityView mainActivityView) {
        dataManager.deleteClusterResultFolder(pathFolderResult, mainActivityView);
    }

    @Override
    public void chooseGallery(MainActivityView view) {
        dataManager.chooseGallery(view);
    }

    public void showGalleryChosen(String result){
        if(view!=null){
            view.showGalleryChosen(result);
        }
    }

    @Override
    public void checkBoxClick(CheckBox chAllImages) {
        if(view!=null){
            dataManager.checkBoxClick(chAllImages);
        }
    }

    @Override
    public void buttonChooseGalleryEnable(boolean b) {
        if(view!=null){
            view.buttonChooseGalleryEnable(b);
        }
    }

    @Override
    public int prepareImages(String path_chosen, CheckBox chAllImages, Context applicationContext) {
        return dataManager.prepareImages(path_chosen, chAllImages, applicationContext);
    }

    @Override
    public void fillWorkingText() {
        dataManager.fillWorkingText();
    }

    public void showWorkingText(String result){
        // TODO: add some image? improve UI?
        if(view!=null){
            view.showWorkingText(result);
        }
    }

    @Override
    public void showProgressBarWorking(ProgressBar progressBarWorking) {
//        dataManager.showProgressBarWorking(progressBarWorking);
        // TODO: ???
    }

    @Override
    public void processImages(MainActivityView mainActivityView) {
        dataManager.startImageProcess(mainActivityView);
    }

    @Override
    public void clustersReady() {
        view.clusterReady();
    }

    @Override
    public void folderGenerator(String pathFolder, MainActivityView mainActivityView) {
        dataManager.folderGenerator(pathFolder, mainActivityView);
    }

    @Override
    public void showFilesManager(String pathFolder) {
        view.showFilesManagerActivity(pathFolder);
    }

    @Override
    public boolean folderResultsExists(String pathFoldersResult) {
        return dataManager.folderResultsExist(pathFoldersResult);
    }

    // TODO: deprecate. used reportProgress instead
    @Deprecated
    @Override
    public void growProgress() {
        view.growProgress();
    }

    @Override
    public void reportProgress(final int p){
        view.reportProgress(p);
    }

    //@Override
    // public Map<String, Number> getMCLParameters() {
    // return dataManager.getMCLParameters();
    //}

    @Override
    public void callErrorToast(String s) {
        view.callErrorToast(s);
    }

    @Override
    public void checkNumberImages(MainActivityView mainActivityView) {
        dataManager.checkNumberImages(mainActivityView);
    }


}
