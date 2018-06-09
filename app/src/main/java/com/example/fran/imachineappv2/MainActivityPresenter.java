package com.example.fran.imachineappv2;

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
    public void deleteClusterResultFolder(String pathFolderResult) {
        dataManager.deleteClusterResultFolder(pathFolderResult);
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
    public boolean prepararImagenes(String path_chosen, CheckBox chAllImages) {
        return dataManager.prepararImagenes(path_chosen, chAllImages);
    }

    @Override
    public void alertBlackWindow(MainActivityView mainActivityView) {
        dataManager.alertBlackWindow(mainActivityView);
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
    public void folderGenerator(String pathFolder) {
        dataManager.folderGenerator(pathFolder);
    }

    @Override
    public void showFilesManager(String pathFolder) {
        view.showFilesManagerActivity(pathFolder);
    }

    @Override
    public boolean folderResultsExists(String pathFoldersResult) {
        return dataManager.folderResultsExist(pathFoldersResult);
    }


}
