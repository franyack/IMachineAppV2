package com.example.fran.imachineappv2.CIEngine.clustering;

import org.apache.commons.io.FileUtils;
import org.ejml.data.DMatrixRMaj;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Created by fran on 07/07/18.
 */

public class MetricsReporter {
    private static Logger LOGGER;
    private FileHandler fileHandler;

    static {
        Logger mainLogger = Logger.getLogger("com.example.fran.imachineapp");
        mainLogger.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter() {
            private static final String format = "[%1$tF %1$tT] [%2$-7s] %3$s %n";

            @Override
            public synchronized String format(LogRecord lr) {
                return String.format(Locale.US, format,
                        new Date(lr.getMillis()),
                        lr.getLevel().getLocalizedName(),
                        lr.getMessage()
                );
            }
        });
        mainLogger.addHandler(handler);

        // TODO: use mainLogger
        //LOGGER = Logger.getLogger(MetricsReporter.class.getName());
        LOGGER = mainLogger;
    }

    private String pathLogs;
    private double timeProcess;
    private List<String> imagesName;
    private List<Integer> clusters;

    private Map<Integer, List<String>> clustersMap;
    private Map<Integer, List<Integer>> clustersMapIdx;

    private Map<String, Number> mclParameters;
    private DMatrixRMaj affinityMatrix;

    public MetricsReporter(List<String> imagesName, List<Integer> clusters,
                           DMatrixRMaj affinityMatrix, Map<String, Number> mclParameters,
                           String pathLogs, double timeProcess){
        this.pathLogs = pathLogs;
        this.imagesName = imagesName;
        this.clusters = clusters;
        this.mclParameters = mclParameters;
        this.affinityMatrix = affinityMatrix;
        this.timeProcess = timeProcess;

        try {
            // TODO: check this, its not working
            // This block configure the logger with handler and formatter
            fileHandler = new FileHandler(this.pathLogs);
            LOGGER.addHandler(fileHandler);
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);

        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        setUpClusters();

    }

    private void setUpClusters(){
        //------------Getting clusters---------------------------
        clustersMap = new TreeMap<>();
        clustersMapIdx = new TreeMap<>();

        List<String> imagesMap;
        List<Integer> imagesMapIdx;

        for(int i=0;i<clusters.size();i++){
            if(!clustersMap.containsKey(clusters.get(i))){
                imagesMap = new ArrayList<>();
                imagesMap.add(imagesName.get(i));
                clustersMap.put(clusters.get(i),imagesMap);


                imagesMapIdx = new ArrayList<>();
                imagesMapIdx.add(i);
                clustersMapIdx.put(clusters.get(i),imagesMapIdx);

            }else{
                imagesMap = clustersMap.get(clusters.get(i));
                if (imagesMap != null) {
                    imagesMap.add(imagesName.get(i));
                    clustersMap.put(clusters.get(i),imagesMap);
                }


                imagesMapIdx = clustersMapIdx.get(clusters.get(i));
                if (imagesMapIdx != null) {
                    imagesMapIdx.add(i);
                    clustersMapIdx.put(clusters.get(i), imagesMapIdx);
                }
            }
        }
        //------------------------------------------------------
    }

    public String getShortName(String s){
        /*
          Get a short name for those folders that are going to be processed in this class
         */
        s = new File(s).getName().split("\\.")[0];
        s = s.toUpperCase();
        s = s.replaceAll("\\d", "");
        s = s.replaceAll("_", "");
        // TODO: remove number
        return s;
    }


    //The function need the 3 first characters of the actual-folder names are the same 3 first characters of
    //the images inside them.

    public void run(){
        String report;

        LOGGER.info("###############");
        report = "###############\n\n";
        report += "Time to process: " + timeProcess + " seconds.\n\n";
        report += "###############\n\n";
        LOGGER.info("MCL Parameters:");
        report += "MCL Parameters:\n";
        for(Map.Entry<String, Number> param : mclParameters.entrySet()){
            LOGGER.info(String.format(Locale.US, "%s: %s", param.getKey(), param.getValue()));
            report += String.format(Locale.US, "%s: %s", param.getKey(), param.getValue()) + "\n";
        }
        LOGGER.info("###############");
        report += "###############\n";
        Map<String, Integer> actualFoldersSize = new TreeMap<>();

        // Map<String, Integer> predictedFoldersMapInit = new TreeMap<>();

        // Initialize empty counters
        //for(File actualFolder: actualFoldersList)
        //    predictedFoldersMapInit.put(getShortName(actualFolder.getName()), 0);

        // This will start from predictedFoldersMapInit, and then filled in a loop
        Map<String, Integer> predictedClusterMap;

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
        for(String image: imagesName){
            image = getShortName(image);  // TODO: check not valid imageNames

            if (actualFoldersSize.containsKey(image))
                actualFoldersSize.put(image, Objects.requireNonNull(actualFoldersSize.get(image))+1);
            else
                actualFoldersSize.put(image, 0);

        }
        //LOGGER.info("Length of " + actualFolder.getName().toUpperCase() +" (" + getShortName(actualFolder.getName())+") folder: " + actualFolder.listFiles().length);

        //--------------------------------------------------------------------------------------------------------------------
        LOGGER.info("-");
        report += "-\n";
        for(Map.Entry<Integer, List<String>> cluster: clustersMap.entrySet()){
            //--------------------Seeking the winning class for each folder predicted------------------------------
            predictedClusterMap = new TreeMap<>();

            // Counting number of images in actual folders that are present in this predicted folder
            for(String image: cluster.getValue()) {
                image = getShortName(image);

                if (predictedClusterMap.containsKey(image))
                    predictedClusterMap.put(image, Objects.requireNonNull(predictedClusterMap.get(image))+1);
                else
                    predictedClusterMap.put(image, 0);
            }


            // The most frequent class is considered the most representative for this predicted cluster
            winningClass = null;
            for(Map.Entry<String, Integer> predictedCluster: predictedClusterMap.entrySet()){
                if(winningClass == null || predictedCluster.getValue().compareTo(winningClass.getValue())>0)
                    winningClass = predictedCluster;
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

            Integer size = truePositivesGroupedFolders.get(winningClass.getKey());
            if (size == null)
                size = 0;

            truePositivesGroupedFolders.put(winningClass.getKey(), size + winningClass.getValue());


            //-------------------------------------------------------------------------------------------------------

            //--------------Getting total size, grouping in one folder by group, for each predicted folder-----------
            size = totalSizeGroupedFolders.get(winningClass.getKey());
            if (size == null)
                size = 0;

            totalSizeGroupedFolders.put(winningClass.getKey(), size + cluster.getValue().size());

            //--------------------------------------------------------------------------------------------------------

            predictedClusterMap.clear();

        }

        Map<String, Float> sorensenDiceGrouped = new TreeMap<>();
        Map<String, Float> precisionGrouped = new TreeMap<>();
        Map<String, Float> recallGrouped = new TreeMap<>();
        Map<String, Float> f1ScoreGrouped = new TreeMap<>();


        for(String actualClass: actualFoldersSize.keySet()){
            sorensenDiceGrouped.put(actualClass, 0.f);
            precisionGrouped.put(actualClass,0.f);
            recallGrouped.put(actualClass, 0.f);
            f1ScoreGrouped.put(actualClass, 0.f);
        }
        //-----------------------Getting metrics for each grouped folders----------------------------------------------
        float precision, recall, sd, f1;
        int tp, totalp, totala;
        for(Map.Entry<String, Integer> truePositivesGroup:truePositivesGroupedFolders.entrySet()){
            tp = truePositivesGroup.getValue();  // True positives
            totalp = Objects.requireNonNull(totalSizeGroupedFolders.get(truePositivesGroup.getKey()));  // TP + FP = Total predicted positives
            totala = Objects.requireNonNull(actualFoldersSize.get(truePositivesGroup.getKey()));  // TP + FN = Total actual positives

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

        LOGGER.info("-");
        report += "-\n";
        LOGGER.info("Sorensen-Dice Average: " + sorensenAverage);
        report += "Sorensen-Dice Average: " + sorensenAverage + "\n";
        LOGGER.info("-");
        report += "-\n";
        LOGGER.info("Accuracy: " + accuracy);
        report += "Accuracy: " + accuracy + "\n";
        LOGGER.info("-");
        report += "-\n";

        //-------------------------------------Getting macro-averaging metrics---------------------------------------------
        float macroAveraginPrecision = 0;
        float macroAveraginRecall = 0;
        float macroAveraginF1Score = 0;
        for(Map.Entry<String, Float> precisionG:precisionGrouped.entrySet()){
            LOGGER.info("Precision for " + precisionG.getKey() + ": "+precisionG.getValue());
            report += "Precision for " + precisionG.getKey() + ": "+precisionG.getValue() + "\n";
            LOGGER.info("Recall for " + precisionG.getKey() + ": "+recallGrouped.get(precisionG.getKey()));
            report += "Recall for " + precisionG.getKey() + ": "+recallGrouped.get(precisionG.getKey()) + "\n";
            LOGGER.info("F1-Score for " + precisionG.getKey() + ": "+f1ScoreGrouped.get(precisionG.getKey()));
            report += "F1-Score for " + precisionG.getKey() + ": "+f1ScoreGrouped.get(precisionG.getKey()) + "\n";
            LOGGER.info("-");
            report += "-\n";
            macroAveraginPrecision+=precisionG.getValue();
            macroAveraginRecall+=recallGrouped.get(precisionG.getKey());
            macroAveraginF1Score+=f1ScoreGrouped.get(precisionG.getKey());
        }
        macroAveraginPrecision/=precisionGrouped.size();
        macroAveraginRecall/=recallGrouped.size();
        macroAveraginF1Score/=f1ScoreGrouped.size();
        //-------------------------------------------------------------------------------------------------------------------

        LOGGER.info("Precision Macro-Averaging: "+ macroAveraginPrecision);
        report += "Precision Macro-Averaging: "+ macroAveraginPrecision + "\n";
        LOGGER.info("Recall Macro-Averaging: "+ macroAveraginRecall);
        report += "Recall Macro-Averaging: "+ macroAveraginRecall + "\n";
        LOGGER.info("F1-Score Macro-Averaging: "+ macroAveraginF1Score);
        report += "F1-Score Macro-Averaging: "+ macroAveraginF1Score + "\n";
        LOGGER.info("-");
        report += "-\n";

        this.runSilhouette(report);

        // TODO: return these results in a Map?
    }

    public void runSilhouette(String report){
        // From https://en.wikipedia.org/wiki/Silhouette_(clustering)

        float averageClusterDistance, lowestAverageDistance, lowestAverageDistanceAuxiliary, silhouette, silhouetteAuxiliary;
        //Map<Integer, Float> silhouetteAvgCluster = new TreeMap<>();
        float silhouetteCluster;
        silhouette=0.f;
        Integer imgPointI;
        int sizeCluster;

        for(Map.Entry<Integer, List<Integer>> cluster:clustersMapIdx.entrySet()){
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
                        averageClusterDistance+= getAffinityValue(imgPointI, cluster.getValue().get(j));
                    }
                    averageClusterDistance/=(sizeCluster-1);  // all the data excepting by the given point i

                    // Let b(i) be the lowest average distance of i to all points in any other cluster,
                    // of which i is not a member.
                    // The cluster with this lowest average dissimilarity is said to be the "neighbouring cluster" of i
                    // because it is the next best fit cluster for point i
                    lowestAverageDistance=Float.POSITIVE_INFINITY;
                    for(Map.Entry<Integer, List<Integer>> cluster2:clustersMapIdx.entrySet()){
                        if(!Objects.equals(cluster.getKey(), cluster2.getKey())){  // if this is any other cluster
                            lowestAverageDistanceAuxiliary=0.f;
                            for(int j=0;j<cluster2.getValue().size();j++)
                                lowestAverageDistanceAuxiliary+= getAffinityValue(imgPointI,cluster2.getValue().get(j));

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
            //LOGGER.info(String.format(Locale.ENGLISH,"Silhouette for cluster %d: %a", cluster.getKey(), silhouetteCluster));
        }
        // The average of s(i) over all points of a cluster is a measure of how tightly grouped all the points in the cluster are.
        // Thus the average s(i) over all data of the entire dataset is a measure of how appropriately the data have been clustered.
        LOGGER.info("Average Silhouette: " + silhouette/imagesName.size());
        report += "Average Silhouette: " + silhouette/imagesName.size() + "\n";
        write(report);
    }

    private float getAffinityValue(Integer i1, Integer i2) {
        return (float) (1-affinityMatrix.get(i1, i2));
    }



    private void write(String report){
        try {
            File file = new File(pathLogs);
            if(file.exists()){
                FileUtils.cleanDirectory(file);
            }else{
                FileUtils.forceMkdir(file);
            }
            File metrics = new File(pathLogs, "Metrics");
            if(!metrics.exists()){
                metrics.mkdirs();
            }
            File gpxfile = new File(metrics, "metrics.txt");
            FileWriter writer = new FileWriter(gpxfile);
            writer.append(report);
            writer.flush();
            writer.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

}
