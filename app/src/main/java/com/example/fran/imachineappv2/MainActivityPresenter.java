package com.example.fran.imachineappv2;

import android.widget.CheckBox;
import android.widget.ProgressBar;

/**
 * Created by fran on 24/05/18.
 */

public class MainActivityPresenter implements MainActivityMvpPresenter {

    private MainActivityMvpView view;
    private DataManagerMvp dataManager;

    public MainActivityPresenter(MainActivityView mainActivityView) {
        view = mainActivityView;
        dataManager = new DataManager(this);
    }


    @Override
    public void deleteClusterResultFolder() {
        dataManager.deleteClusterResultFolder();
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


}
