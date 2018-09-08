package com.example.fran.imachineappv2;

import android.content.Context;
import android.widget.CheckBox;
import android.widget.ProgressBar;

import java.util.List;

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
    public int prepararImagenes(String path_chosen, CheckBox chAllImages, Context applicationContext) {
        return dataManager.prepararImagenes(path_chosen, chAllImages, applicationContext);
    }

    @Override
    public void fillWorkingText() {
        dataManager.fillWorkingText();
    }

    public void showWorkingText(String result){
        if(view!=null){
            view.showWorkingText(result);
        }
    }

    @Override
    public void showProgressBarWorking(ProgressBar progressBarWorking) {
//        dataManager.showProgressBarWorking(progressBarWorking);
    }

    @Override
    public void procesarImagenes(MainActivityView mainActivityView) {
        dataManager.startImageProcess(mainActivityView);
//        dataManager.procesarImagenes();
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

    @Override
    public void growProgress() {
        view.growProgress();
    }

    @Override
    public List<String> getMclParameters() {
        return dataManager.getMclParameters();
    }

    @Override
    public void callErrorToast(String s) {
        view.callErrorToast(s);
    }

    @Override
    public void checkNumberImages(MainActivityView mainActivityView) {
        dataManager.checkNumberImages(mainActivityView);
    }


}
