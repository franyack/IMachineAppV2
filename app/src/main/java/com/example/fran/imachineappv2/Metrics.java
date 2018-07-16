package com.example.fran.imachineappv2;

import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * Created by fran on 07/07/18.
 */

public class Metrics {
    private static final Logger LOGGER = Logger.getLogger(MainActivityView.class.getName());
    String pathFoldersModel;
    String pathFoldersResult;
    List<String> mclParameters = new ArrayList<>();

    public Metrics(String pathFoldersModel,String pathFoldersResult, List<String> mclParameters){
        this.pathFoldersModel = pathFoldersModel;
        this.pathFoldersResult = pathFoldersResult;
        this.mclParameters = mclParameters;
    }

    //The function need that the 3 first characters of the model-folder names are the same 3 first characters of
    //the images inside them.
    public void Metrics(){
        LOGGER.info("-");
        LOGGER.info("MCL maxIt: " + mclParameters.get(0));
        LOGGER.info("MCL expPow: " + mclParameters.get(1));
        LOGGER.info("MCL infPow: " + mclParameters.get(2));
        LOGGER.info("-");
        float qs = 0;
        File foldersModel = new File(pathFoldersModel);
        File [] foldersModelList = foldersModel.listFiles();
        Map<String, Integer> modelsFolderSize = new TreeMap<>();
        Map<String, Integer> models = new TreeMap<>();
        File foldersResult = new File(pathFoldersResult);
        File [] foldersResultList = foldersResult.listFiles();
        Map<String, Map<String, Float>> results = new TreeMap<>();
        Map<String, Map<String, Map<Integer, Integer>>> results2 = new TreeMap<>();
        Map<String, Integer> intersection = new TreeMap<>();
        Map<String, Integer> totalSizeFolder = new TreeMap<>();
        Map.Entry<String, Integer> maxValue;
        for(File folderModel: foldersModelList){
            modelsFolderSize.put(folderModel.getName().substring(0,3).toUpperCase(),folderModel.listFiles().length);
            LOGGER.info("Length of " + folderModel.getName().toUpperCase() +" (" + folderModel.getName().substring(0,3).toUpperCase()+") folder: " + folderModel.listFiles().length);
        }
        LOGGER.info("-");
        for(File folderResult: foldersResultList){
            for(File folderModel: foldersModelList){
                models.put(folderModel.getName().substring(0,3).toUpperCase(), 0);
            }
            for(File file: folderResult.listFiles()){
                models.put(file.getName().substring(0,3).toUpperCase(), models.get(file.getName().substring(0,3).toUpperCase())+1);
            }
            maxValue = null;
            for(Map.Entry<String, Integer> model: models.entrySet()){
                if(maxValue == null || model.getValue().compareTo(maxValue.getValue())>0){
                    maxValue = model;
                }
            }
//          FolderName -> <ModelWinnerName, SorensenDice>
            qs = (float) (2.0 * maxValue.getValue() / (modelsFolderSize.get(maxValue.getKey()) + folderResult.listFiles().length));
            Map<String, Float> res = new TreeMap<>();
            res.put(maxValue.getKey(),qs);
            results.put(folderResult.getName(),res); //just for verify...

//          FolderName -> ModelWinnerName -> Intersection, SizeFolderResult
            Map<Integer, Integer> interTotal = new TreeMap<>();
            interTotal.put(maxValue.getValue(), folderResult.listFiles().length);
            Map<String, Map<Integer,Integer>> winnerInterTotal = new TreeMap<>();
            winnerInterTotal.put(maxValue.getKey(), interTotal);
            results2.put(folderResult.getName(),winnerInterTotal); //just for more details...

            if(intersection.containsKey(maxValue.getKey())){
                intersection.put(maxValue.getKey(), intersection.get(maxValue.getKey())+maxValue.getValue());
            }else{
                intersection.put(maxValue.getKey(), maxValue.getValue());
            }
            if(totalSizeFolder.containsKey(maxValue.getKey())){
                totalSizeFolder.put(maxValue.getKey(), totalSizeFolder.get(maxValue.getKey()) + folderResult.listFiles().length);
            }else{
                totalSizeFolder.put(maxValue.getKey(), folderResult.listFiles().length);
            }

            models.clear();

        }

        Map<String, Float> sorensenDiceMap = new TreeMap<>();
        Map<String, Float> precisionMap = new TreeMap<>();
        Map<String, Float> recallMap = new TreeMap<>();
        Map<String, Float> f1Score = new TreeMap<>();
        for(File folderModel: foldersModelList){
            sorensenDiceMap.put(folderModel.getName().substring(0,3).toUpperCase(),(float) 0);
            precisionMap.put(folderModel.getName().substring(0,3).toUpperCase(),(float) 0);
            recallMap.put(folderModel.getName().substring(0,3).toUpperCase(),(float) 0);
            f1Score.put(folderModel.getName().substring(0,3).toUpperCase(),(float) 0);
        }

        for(Map.Entry<String, Integer> modelClass:intersection.entrySet()){
            sorensenDiceMap.put(modelClass.getKey(), (float) (2.0*modelClass.getValue())/(modelsFolderSize.get(modelClass.getKey())+totalSizeFolder.get(modelClass.getKey())));
            precisionMap.put(modelClass.getKey(), (float) modelClass.getValue()/totalSizeFolder.get(modelClass.getKey()));
            recallMap.put(modelClass.getKey(), (float) modelClass.getValue()/modelsFolderSize.get(modelClass.getKey()));
            f1Score.put(modelClass.getKey(), (2*precisionMap.get(modelClass.getKey())*recallMap.get(modelClass.getKey()))/(precisionMap.get(modelClass.getKey())+recallMap.get(modelClass.getKey())));
        }

        float sorensenAverge = 0;
        for(Map.Entry<String, Float> sorensen:sorensenDiceMap.entrySet()){
            sorensenAverge+=sorensen.getValue();
        }
        sorensenAverge/=sorensenDiceMap.size();

        int truePositives = 0;
        for(Map.Entry<String,Integer> inter:intersection.entrySet()){
            truePositives+=inter.getValue();
        }
        int totalImages = 0;
        for(Map.Entry<String, Integer> tot:totalSizeFolder.entrySet()){
            totalImages+=tot.getValue();
        }

        float accuracy = (float) truePositives/totalImages;

        LOGGER.info("-");
        LOGGER.info("Sorensen-Dice Average: " + sorensenAverge);
        LOGGER.info("-");
        LOGGER.info("Accuracy: " + accuracy);
        LOGGER.info("-");
        float macroAveraginPrecision = 0;
        float macroAveraginRecall = 0;
        float macroAveraginF1Score = 0;
        for(Map.Entry<String, Float> precision:precisionMap.entrySet()){
            LOGGER.info("Precision for " + precision.getKey() + ": "+precision.getValue());
            LOGGER.info("Recall for " + precision.getKey() + ": "+recallMap.get(precision.getKey()));
            LOGGER.info("F1-Score for " + precision.getKey() + ": "+f1Score.get(precision.getKey()));
            LOGGER.info("-");
            macroAveraginPrecision+=precision.getValue();
            macroAveraginRecall+=recallMap.get(precision.getKey());
            macroAveraginF1Score+=f1Score.get(precision.getKey());
        }
        macroAveraginPrecision/=precisionMap.size();
        macroAveraginRecall/=recallMap.size();
        macroAveraginF1Score/=f1Score.size();
        LOGGER.info("Precision Macro-Averaging: "+ macroAveraginPrecision);
        LOGGER.info("Recall Macro-Averaging: "+ macroAveraginRecall);
        LOGGER.info("F1-Score Macro-Averaging: "+ macroAveraginF1Score);
        LOGGER.info("-");
    }

}
