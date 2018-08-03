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
    //TODO: buscar manera de reportar hora, paquete, etc
    private static final Logger LOGGER = Logger.getLogger(MainActivityView.class.getName());
    private String pathActual;
    private String pathPredicted;
    private List<String> mclParameters = new ArrayList<>();

    public Metrics(String pathActual,String pathPredicted, List<String> mclParameters){
        this.pathActual = pathActual;
        this.pathPredicted = pathPredicted;
        this.mclParameters = mclParameters;
    }

    //The function need the 3 first characters of the actual-folder names are the same 3 first characters of
    //the images inside them.
    public void Metrics(){
        LOGGER.info("-");
        //TODO:PRUNNING y epsConvergence
        LOGGER.info("MCL maxIt: " + mclParameters.get(0));
        LOGGER.info("MCL expPow: " + mclParameters.get(1));
        LOGGER.info("MCL infPow: " + mclParameters.get(2));
        LOGGER.info("-");
        float qs = 0;

        File actualFolders = new File(pathActual);
        File [] actualFoldersList = actualFolders.listFiles();
        Map<String, Integer> actualFoldersSize = new TreeMap<>();


        File filePathPredicted = new File(pathPredicted);
        File [] foldersPredictedList = filePathPredicted.listFiles();

        Map<String, Integer> predictedFoldersMap = new TreeMap<>();

        //----------------Used only for extra-information------------------------------------
        Map<String, Map<String, Float>> predictFolders = new TreeMap<>();
        Map<String, Map<String, Map<Integer, Integer>>> predictFolders2 = new TreeMap<>();
        //-----------------------------------------------------------------------------------

        Map<String, Integer> truePositivesGroupedFolders = new TreeMap<>();
        Map<String, Integer> totalSizeGroupedFolders = new TreeMap<>();

        Map.Entry<String, Integer> winningClass;
        //----------------------------------Getting size by each actual folder in actualFoldersSize---------------------------
        for(File actualFolder: actualFoldersList){
            actualFoldersSize.put(actualFolder.getName().substring(0,3).toUpperCase(), actualFolder.listFiles().length);
            LOGGER.info("Length of " + actualFolder.getName().toUpperCase() +" (" + actualFolder.getName().substring(0,3).toUpperCase()+") folder: " + actualFolder.listFiles().length);
        }
        //--------------------------------------------------------------------------------------------------------------------
        LOGGER.info("-");
        for(File folderPredicted: foldersPredictedList){
            //--------------------Seeking the winning class for each folder predicted------------------------------
            for(File actualFolder: actualFoldersList){
                predictedFoldersMap.put(actualFolder.getName().substring(0,3).toUpperCase(), 0);
            }
            for(File image: folderPredicted.listFiles()){
                predictedFoldersMap.put(image.getName().substring(0,3).toUpperCase(), predictedFoldersMap.get(image.getName().substring(0,3).toUpperCase())+1);
            }
            winningClass = null;
            for(Map.Entry<String, Integer> predictedMap: predictedFoldersMap.entrySet()){
                if(winningClass == null || predictedMap.getValue().compareTo(winningClass.getValue())>0){
                    winningClass = predictedMap;
                }
            }
            //------------------------------------------------------------------------------------------------------

            //-----------------------FolderName -> <ActualNameWinner, SorensenDice>---------------------------------
            qs = (float) (2.0 * winningClass.getValue() / (actualFoldersSize.get(winningClass.getKey()) + folderPredicted.listFiles().length));
            Map<String, Float> res = new TreeMap<>();
            res.put(winningClass.getKey(),qs);
            predictFolders.put(folderPredicted.getName(),res); //just for verify...
            //------------------------------------------------------------------------------------------------------

            //----------------FolderName -> ActualNameWinner -> Intersection, SizeFolderResult----------------------
            Map<Integer, Integer> interTotal = new TreeMap<>();
            interTotal.put(winningClass.getValue(), folderPredicted.listFiles().length);
            Map<String, Map<Integer,Integer>> winnerInterTotal = new TreeMap<>();
            winnerInterTotal.put(winningClass.getKey(), interTotal);
            predictFolders2.put(folderPredicted.getName(),winnerInterTotal); //just for more details...
            //------------------------------------------------------------------------------------------------------

            //-----------Looking for true positives, grouping in one folder by group, for each predicted folder-----
            if(truePositivesGroupedFolders.containsKey(winningClass.getKey())){
                truePositivesGroupedFolders.put(winningClass.getKey(), truePositivesGroupedFolders.get(winningClass.getKey())+winningClass.getValue());
            }else{
                truePositivesGroupedFolders.put(winningClass.getKey(), winningClass.getValue());
            }
            //-------------------------------------------------------------------------------------------------------

            //--------------Getting total size, grouping in one folder by group, for each predicted folder-----------
            if(totalSizeGroupedFolders.containsKey(winningClass.getKey())){
                totalSizeGroupedFolders.put(winningClass.getKey(), totalSizeGroupedFolders.get(winningClass.getKey()) + folderPredicted.listFiles().length);
            }else{
                totalSizeGroupedFolders.put(winningClass.getKey(), folderPredicted.listFiles().length);
            }
            //--------------------------------------------------------------------------------------------------------

            predictedFoldersMap.clear();

        }

        Map<String, Float> sorensenDiceGrouped = new TreeMap<>();
        Map<String, Float> precisionGrouped = new TreeMap<>();
        Map<String, Float> recallGrouped = new TreeMap<>();
        Map<String, Float> f1ScoreGrouped = new TreeMap<>();
        for(File actualFolder: actualFoldersList){
            sorensenDiceGrouped.put(actualFolder.getName().substring(0,3).toUpperCase(),(float) 0);
            precisionGrouped.put(actualFolder.getName().substring(0,3).toUpperCase(),(float) 0);
            recallGrouped.put(actualFolder.getName().substring(0,3).toUpperCase(),(float) 0);
            f1ScoreGrouped.put(actualFolder.getName().substring(0,3).toUpperCase(),(float) 0);
        }
        //-----------------------Getting metrics for each grouped folders----------------------------------------------
        for(Map.Entry<String, Integer> truePositivesGroup:truePositivesGroupedFolders.entrySet()){
            sorensenDiceGrouped.put(truePositivesGroup.getKey(), (float) (2.0*truePositivesGroup.getValue())/(actualFoldersSize.get(truePositivesGroup.getKey())+totalSizeGroupedFolders.get(truePositivesGroup.getKey())));
            precisionGrouped.put(truePositivesGroup.getKey(), (float) truePositivesGroup.getValue()/totalSizeGroupedFolders.get(truePositivesGroup.getKey()));
            recallGrouped.put(truePositivesGroup.getKey(), (float) truePositivesGroup.getValue()/actualFoldersSize.get(truePositivesGroup.getKey()));
            f1ScoreGrouped.put(truePositivesGroup.getKey(), (2*precisionGrouped.get(truePositivesGroup.getKey())*recallGrouped.get(truePositivesGroup.getKey()))/(precisionGrouped.get(truePositivesGroup.getKey())+recallGrouped.get(truePositivesGroup.getKey())));
        }
        //--------------------------------------------------------------------------------------------------------------

        //---------------------Getting average Sorencen-Dice coefficient------------------------------------------------
        float sorensenAverge = 0;
        for(Map.Entry<String, Float> sorensen:sorensenDiceGrouped.entrySet()){
            sorensenAverge+=sorensen.getValue();
        }
        sorensenAverge/=sorensenDiceGrouped.size();
        //---------------------------------------------------------------------------------------------------------------

        //----------------------------------------Getting Total Accuracy-------------------------------------------------
        int truePositives = 0;
        for(Map.Entry<String,Integer> truePositiveGroup:truePositivesGroupedFolders.entrySet()){
            truePositives+=truePositiveGroup.getValue();
        }
        int totalImages = 0;
        for(Map.Entry<String, Integer> tot:totalSizeGroupedFolders.entrySet()){
            totalImages+=tot.getValue();
        }

        float accuracy = (float) truePositives/totalImages;
        //----------------------------------------------------------------------------------------------------------------

        LOGGER.info("-");
        LOGGER.info("Sorensen-Dice Average: " + sorensenAverge);
        LOGGER.info("-");
        LOGGER.info("Accuracy: " + accuracy);
        LOGGER.info("-");

        //-------------------------------------Getting macro-averaging metrics---------------------------------------------
        float macroAveraginPrecision = 0;
        float macroAveraginRecall = 0;
        float macroAveraginF1Score = 0;
        for(Map.Entry<String, Float> precision:precisionGrouped.entrySet()){
            LOGGER.info("Precision for " + precision.getKey() + ": "+precision.getValue());
            LOGGER.info("Recall for " + precision.getKey() + ": "+recallGrouped.get(precision.getKey()));
            LOGGER.info("F1-Score for " + precision.getKey() + ": "+f1ScoreGrouped.get(precision.getKey()));
            LOGGER.info("-");
            macroAveraginPrecision+=precision.getValue();
            macroAveraginRecall+=recallGrouped.get(precision.getKey());
            macroAveraginF1Score+=f1ScoreGrouped.get(precision.getKey());
        }
        macroAveraginPrecision/=precisionGrouped.size();
        macroAveraginRecall/=recallGrouped.size();
        macroAveraginF1Score/=f1ScoreGrouped.size();
        //-------------------------------------------------------------------------------------------------------------------

        LOGGER.info("Precision Macro-Averaging: "+ macroAveraginPrecision);
        LOGGER.info("Recall Macro-Averaging: "+ macroAveraginRecall);
        LOGGER.info("F1-Score Macro-Averaging: "+ macroAveraginF1Score);
        LOGGER.info("-");
    }

}
