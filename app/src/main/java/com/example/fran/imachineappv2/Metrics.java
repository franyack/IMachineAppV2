package com.example.fran.imachineappv2;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import org.apache.commons.io.FileUtils;
import org.ejml.data.DMatrixRMaj;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Created by fran on 07/07/18.
 */

public class Metrics implements Serializable {
    // TODO: buscar manera de reportar hora, paquete, etc
    // --> check this: https://www.logicbig.com/tutorials/core-java-tutorial/logging/customizing-default-format.html
    private static final Logger LOGGER = Logger.getLogger(MainActivityView.class.getName());
    private String pathActual;
    private String pathPredicted;
    private List<String> mclParameters = new ArrayList<>();
    private DMatrixRMaj affinityMatrix;
    private ArrayList<String> vImages;
    private ArrayList<Integer> vClusters;
    private double timeProcess;

    public Metrics(DMatrixRMaj affinityMatrix, ArrayList<String> vImages, ArrayList<Integer> vClusters, String pathActual, String pathPredicted, List<String> mclParameters, double timeProcess){
        this.affinityMatrix = affinityMatrix;
        this.vImages = vImages;
        this.vClusters = vClusters;
        this.pathActual = pathActual;
        this.pathPredicted = pathPredicted;
        this.mclParameters = mclParameters;
        this.timeProcess = timeProcess;
    }

    public String getShortName(String s){
        /*
          Get a short name for those folders that are going to be processed in this class
         */
        int endIdx = Math.min(3, s.length());
        return s.substring(0, endIdx).toUpperCase();
    }

    //The function need the 3 first characters of the actual-folder names are the same 3 first characters of
    //the images inside them.

    public void getScore(MainActivityView mainActivityView){

        String output;
        output = "Time to process: " + timeProcess + "seconds\n";
        output += "-\n";
        output += "MCL maxIt: " + mclParameters.get(0) + "\n";
        output += "MCL expPow: " + mclParameters.get(1) + "\n";
        output += "MCL infPow: " + mclParameters.get(2) + "\n";
        output += "MCL threshPrune: " + mclParameters.get(3) + "\n";
        output += "-\n";
//        LOGGER.info("-");
//        LOGGER.info("MCL maxIt: " + mclParameters.get(0));
//        LOGGER.info("MCL expPow: " + mclParameters.get(1));
//        LOGGER.info("MCL infPow: " + mclParameters.get(2));
//        LOGGER.info("MCL threshPrune: " + mclParameters.get(3));
//        LOGGER.info("-");

        File actualFolders = new File(pathActual);
        File [] actualFoldersList = actualFolders.listFiles();
        Map<String, Integer> actualFoldersSize = new TreeMap<>();

        File filePathPredicted = new File(pathPredicted);
        File [] foldersPredictedList = filePathPredicted.listFiles();

        Map<String, Integer> predictedFoldersMapInit = new TreeMap<>();

        // Initialize empty counters
        for(File actualFolder: actualFoldersList)
            predictedFoldersMapInit.put(getShortName(actualFolder.getName()), 0);

        // This will start from predictedFoldersMapInit, and then filled in a loop
        Map<String, Integer> predictedFoldersMap;

        //----------------Used only for extra-information------------------------------------
        /*
        float qs = 0;
        Map<String, Float> res;
        Map<Integer, Integer> interTotal;
        Map<String, Map<Integer,Integer>> winnerInterTotal;
        Map<String, Map<String, Float>> predictFolders = new TreeMap<>();
        Map<String, Map<String, Map<Integer, Integer>>> predictFolders2 = new TreeMap<>();
        */
        //-----------------------------------------------------------------------------------

        Map<String, Integer> truePositivesGroupedFolders = new TreeMap<>();
        Map<String, Integer> totalSizeGroupedFolders = new TreeMap<>();

        Map.Entry<String, Integer> winningClass;
        //----------------------------------Getting size by each actual folder in actualFoldersSize---------------------------
        for(File actualFolder: actualFoldersList){
            actualFoldersSize.put(getShortName(actualFolder.getName()), actualFolder.listFiles().length);
//            LOGGER.info("Length of " + actualFolder.getName().toUpperCase() +" (" + getShortName(actualFolder.getName())+") folder: " + actualFolder.listFiles().length);
            output += "Length of " + actualFolder.getName().toUpperCase() +" (" + getShortName(actualFolder.getName())+") folder: " + actualFolder.listFiles().length + "\n";
        }
        //--------------------------------------------------------------------------------------------------------------------
//        LOGGER.info("-");
        output += "-\n";
        for(File folderPredicted: foldersPredictedList){
            //--------------------Seeking the winning class for each folder predicted------------------------------
            predictedFoldersMap = new TreeMap<>(predictedFoldersMapInit);  // Start from a zero-counters map

            // Counting number of images in actual folders that are present in this predicted folder
            for(File image: folderPredicted.listFiles())
                predictedFoldersMap.put(getShortName(image.getName()), predictedFoldersMap.get(getShortName(image.getName()))+1);

            // The most frequent class is considered the most representative for this predicted folder
            winningClass = null;
            for(Map.Entry<String, Integer> predictedMap: predictedFoldersMap.entrySet()){
                if(winningClass == null || predictedMap.getValue().compareTo(winningClass.getValue())>0)
                    winningClass = predictedMap;
            }

            if(winningClass == null)
                // To avoid exceptions later
                continue;
            //------------------------------------------------------------------------------------------------------

            /*
            //-----------------------FolderName -> <ActualNameWinner, SorensenDice>---------------------------------
            qs = (float) (2.0 * winningClass.getValue() / (actualFoldersSize.get(winningClass.getKey()) + folderPredicted.listFiles().length));
            res = new TreeMap<>();
            res.put(winningClass.getKey(),qs);
            predictFolders.put(folderPredicted.getName(),res); //just for verify...
            //------------------------------------------------------------------------------------------------------

            //----------------FolderName -> ActualNameWinner -> Intersection, SizeFolderResult----------------------
            interTotal = new TreeMap<>();
            interTotal.put(winningClass.getValue(), folderPredicted.listFiles().length);
            winnerInterTotal = new TreeMap<>();
            winnerInterTotal.put(winningClass.getKey(), interTotal);
            predictFolders2.put(folderPredicted.getName(),winnerInterTotal); //just for more details...
            //------------------------------------------------------------------------------------------------------
            */

            //-----------Looking for true positives, grouping in one folder by group, for each predicted folder-----
            if(truePositivesGroupedFolders.containsKey(winningClass.getKey()))
                truePositivesGroupedFolders.put(winningClass.getKey(), truePositivesGroupedFolders.get(winningClass.getKey()) + winningClass.getValue());
            else
                truePositivesGroupedFolders.put(winningClass.getKey(), winningClass.getValue());
            //-------------------------------------------------------------------------------------------------------

            //--------------Getting total size, grouping in one folder by group, for each predicted folder-----------
            if(totalSizeGroupedFolders.containsKey(winningClass.getKey()))
                totalSizeGroupedFolders.put(winningClass.getKey(), totalSizeGroupedFolders.get(winningClass.getKey()) + folderPredicted.listFiles().length);
            else
                totalSizeGroupedFolders.put(winningClass.getKey(), folderPredicted.listFiles().length);
            //--------------------------------------------------------------------------------------------------------

            predictedFoldersMap.clear();

        }

        Map<String, Float> sorensenDiceGrouped = new TreeMap<>();
        Map<String, Float> precisionGrouped = new TreeMap<>();
        Map<String, Float> recallGrouped = new TreeMap<>();
        Map<String, Float> f1ScoreGrouped = new TreeMap<>();

        String folderName;

        for(File actualFolder: actualFoldersList){
            folderName = getShortName(actualFolder.getName());
            sorensenDiceGrouped.put(folderName, 0.f);
            precisionGrouped.put(folderName,0.f);
            recallGrouped.put(folderName, 0.f);
            f1ScoreGrouped.put(folderName, 0.f);
        }
        //-----------------------Getting metrics for each grouped folders----------------------------------------------
        float precision, recall, sd, f1;
        int tp, totalp, totala;
        for(Map.Entry<String, Integer> truePositivesGroup:truePositivesGroupedFolders.entrySet()){
            tp = truePositivesGroup.getValue();  // True positives
            totalp = totalSizeGroupedFolders.get(truePositivesGroup.getKey());  // TP + FP = Total predicted positives
            totala = actualFoldersSize.get(truePositivesGroup.getKey());  // TP + FN = Total actual positives

            precision = (float) tp/totalp;  // P = TP/(TP+FP)
            recall = (float) tp/totala;  // R = TP/(TP+FN)
            f1 = 2*precision*recall/(precision+recall);  // F1=2*P*R/(P+R)
            sd = (float) (2.0*tp)/(totala+totalp);  // SD=2|A+B|/(|A|+|B|)

            sorensenDiceGrouped.put(truePositivesGroup.getKey(), sd);
            precisionGrouped.put(truePositivesGroup.getKey(), precision);
            recallGrouped.put(truePositivesGroup.getKey(), recall);
            f1ScoreGrouped.put(truePositivesGroup.getKey(), f1);
        }
        //--------------------------------------------------------------------------------------------------------------

        //---------------------Getting average Sorencen-Dice coefficient------------------------------------------------
        float sorensenAverage = 0.f;
        for(Map.Entry<String, Float> sorensen:sorensenDiceGrouped.entrySet())
            sorensenAverage += sorensen.getValue();
        sorensenAverage/=sorensenDiceGrouped.size();
        //---------------------------------------------------------------------------------------------------------------

        //----------------------------------------Getting Total Accuracy-------------------------------------------------
        int truePositives = 0;
        for(Map.Entry<String,Integer> truePositiveGroup:truePositivesGroupedFolders.entrySet())
            truePositives += truePositiveGroup.getValue();
        int totalImages = 0;
        for(Map.Entry<String, Integer> tot:totalSizeGroupedFolders.entrySet())
            totalImages += tot.getValue();

        float accuracy = (float) truePositives/totalImages;  // The greater the acc, the higher the density of clusters
        //----------------------------------------------------------------------------------------------------------------
        output += "-\n";
        output += "Sorensen-Dice Average: " + sorensenAverage + "\n";
        output += "-\n";
        output += "Accuracy: " + accuracy + "\n";
        output += "-\n";
//        LOGGER.info("-");
//        LOGGER.info("Sorensen-Dice Average: " + sorensenAverage);
//        LOGGER.info("-");
//        LOGGER.info("Accuracy: " + accuracy);
//        LOGGER.info("-");

        //-------------------------------------Getting macro-averaging metrics---------------------------------------------
        float macroAveraginPrecision = 0;
        float macroAveraginRecall = 0;
        float macroAveraginF1Score = 0;
        for(Map.Entry<String, Float> precisionG:precisionGrouped.entrySet()){
//            LOGGER.info("Precision for " + precisionG.getKey() + ": "+precisionG.getValue());
//            LOGGER.info("Recall for " + precisionG.getKey() + ": "+recallGrouped.get(precisionG.getKey()));
//            LOGGER.info("F1-Score for " + precisionG.getKey() + ": "+f1ScoreGrouped.get(precisionG.getKey()));
//            LOGGER.info("-");
            output += "Precision for " + precisionG.getKey() + ": "+precisionG.getValue() + "\n";
            output += "Recall for " + precisionG.getKey() + ": "+recallGrouped.get(precisionG.getKey()) + "\n";
            output += "F1-Score for " + precisionG.getKey() + ": "+f1ScoreGrouped.get(precisionG.getKey()) + "\n";
            output += "-\n";
            macroAveraginPrecision+=precisionG.getValue();
            macroAveraginRecall+=recallGrouped.get(precisionG.getKey());
            macroAveraginF1Score+=f1ScoreGrouped.get(precisionG.getKey());
        }
        macroAveraginPrecision/=precisionGrouped.size();
        macroAveraginRecall/=recallGrouped.size();
        macroAveraginF1Score/=f1ScoreGrouped.size();
        //-------------------------------------------------------------------------------------------------------------------
        output += "Precision Macro-Averaging: "+ macroAveraginPrecision + "\n";
        output += "Recall Macro-Averaging: "+ macroAveraginRecall + "\n";
        output += "F1-Score Macro-Averaging: "+ macroAveraginF1Score + "\n";
        output += "-\n";
//        LOGGER.info("Precision Macro-Averaging: "+ macroAveraginPrecision);
//        LOGGER.info("Recall Macro-Averaging: "+ macroAveraginRecall);
//        LOGGER.info("F1-Score Macro-Averaging: "+ macroAveraginF1Score);
//        LOGGER.info("-");

        Silhouette(output, mainActivityView);
    }

    public void Silhouette(String output, MainActivityView mainActivityView){
        // From https://en.wikipedia.org/wiki/Silhouette_(clustering)
        //------------Getting clusters---------------------------
        Map<Integer, List<String>> clusters = new TreeMap<>();
        List<String> images;
        for(int i=0;i<vClusters.size();i++){
            images = new ArrayList<>();
            if(!clusters.containsKey(vClusters.get(i))){
                images.add(vImages.get(i));
                clusters.put(vClusters.get(i),images);
            }else{
                images = clusters.get(vClusters.get(i));
                images.add(vImages.get(i));
                clusters.put(vClusters.get(i),images);
            }
        }
        //------------------------------------------------------
        float averageClusterDistance, lowestAverageDistance, lowestAverageDistanceAuxiliary, silhouette, silhouetteAuxiliary;
        //Map<Integer, Float> silhouetteAvgCluster = new TreeMap<>();
        float silhouetteCluster;
        silhouette=0.f;
        String imgPointI;
        int sizeCluster;

        for(Map.Entry<Integer, List<String>> cluster:clusters.entrySet()){
            sizeCluster = cluster.getValue().size();
            silhouetteCluster = 0.f;
            // for clusters with size=1, the score is 0 to prevent large number of clusters
            if(sizeCluster>1){
                for(int i=0;i<sizeCluster;i++){
                    // Let a(i) be the average distance between i and all other data within the same cluster.
                    // We can interpret a(i) as a measure of how well i is assigned to its cluster
                    // (the smaller the value, the better the assignment).
                    imgPointI = cluster.getValue().get(i);
                    averageClusterDistance=0.f;
                    for(int j=0;j<sizeCluster;j++){
                        if(i == j) continue;  // we don't have to take into account the given point i
                        averageClusterDistance+=getAffinityDistance(imgPointI, cluster.getValue().get(j));
                    }
                    averageClusterDistance/=(sizeCluster-1);  // all the data excepting by the given point i

                    // Let b(i) be the lowest average distance of i to all points in any other cluster,
                    // of which i is not a member.
                    // The cluster with this lowest average dissimilarity is said to be the "neighbouring cluster" of i
                    // because it is the next best fit cluster for point i
                    lowestAverageDistance=Float.POSITIVE_INFINITY;
                    for(Map.Entry<Integer, List<String>> cluster2:clusters.entrySet()){
                        if(!Objects.equals(cluster.getKey(), cluster2.getKey())){  // if this is any other cluster
                            lowestAverageDistanceAuxiliary=0.f;
                            for(int j=0;j<cluster2.getValue().size();j++)
                                lowestAverageDistanceAuxiliary+=getAffinityDistance(imgPointI,cluster2.getValue().get(j));

                            lowestAverageDistanceAuxiliary/=cluster2.getValue().size();
                            if(lowestAverageDistanceAuxiliary<lowestAverageDistance)
                                lowestAverageDistance=lowestAverageDistanceAuxiliary;
                        }
                    }
                    // We now define a silhouette:
                    // s(i) = (b(i) - a(i))/max(a(i), b(i))
                    silhouetteAuxiliary = (lowestAverageDistance-averageClusterDistance) / Math.max(averageClusterDistance,lowestAverageDistance);

                    // Adding this computed value to avg measures
                    silhouette+=silhouetteAuxiliary;
                    silhouetteCluster+=silhouetteAuxiliary;
                }
            }
            silhouetteCluster /= sizeCluster;
            //silhouetteAvgCluster.put(cluster.getKey(), silhouetteCluster);
//            LOGGER.info(String.format(Locale.ENGLISH,"Silhouette for cluster %d: %a", cluster.getKey(), silhouetteCluster));
        }
        // The average of s(i) over all points of a cluster is a measure of how tightly grouped all the points in the cluster are.
        // Thus the average s(i) over all data of the entire dataset is a measure of how appropriately the data have been clustered.
//        LOGGER.info("Average Silhouette: " + silhouette/vImages.size());
        output += "Average Silhouette: " + silhouette/vImages.size() + "\n";
        writeToFile(output, mainActivityView);
    }

    private float getAffinityDistance(String img1, String img2) {
        int i=0;
        while(!vImages.get(i).equals(img1)){
            i++;
        }
        int j=0;
        while(!vImages.get(j).equals(img2)){
            j++;
        }
        return (float) (1-affinityMatrix.get(i,j));
    }

    private void writeToFile(String data, MainActivityView mainActivityView) {
        try {
            String pathMetrics = Environment.getExternalStorageDirectory() + File.separator + "IMachineAppMetrics";
            File file = new File(pathMetrics);
            if(file.exists()){
                FileUtils.cleanDirectory(file);
            }else{
                FileUtils.forceMkdir(file);
            }
            File metrics = new File(pathMetrics, "Metrics");
            if(!metrics.exists()){
                metrics.mkdirs();
            }
            File gpxfile = new File(metrics, "metrics.txt");
            FileWriter writer = new FileWriter(gpxfile);
            writer.append(data);
            writer.flush();
            writer.close();
            File scan = new File(metrics, "metrics.txt");
            Intent mediaScannerIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri fileContentUri = Uri.fromFile(scan);
            mediaScannerIntent.setData(fileContentUri);
            mainActivityView.getApplicationContext().sendBroadcast(mediaScannerIntent);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
