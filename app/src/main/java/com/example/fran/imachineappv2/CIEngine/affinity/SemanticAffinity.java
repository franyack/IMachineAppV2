package com.example.fran.imachineappv2.CIEngine.affinity;

import com.example.fran.imachineappv2.CIEngine.imagenet.TopPredictions;
import com.example.fran.imachineappv2.CIEngine.imagenet.WNIDPrediction;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Documentation here...
 *
 *
 * @author  Francisco Yackel
 * @version 1.0.0
 * @see "https://www.tutorialspoint.com/java/java_documentation.htm"
 */

// TODO: extends BaseAffinity?
public class SemanticAffinity {
    private double thresholdAffinity;

    public SemanticAffinity(double thresholdAffinity){
        this.thresholdAffinity = thresholdAffinity;
    }

    /**
     * This method is used to ...
     *
     *
     * @param  topPredictions ...
     * @return double[][] ...
     */
    // TODO: return a EJML matrix (more efficient)
    public double[][] getAffinityMatrix(List<TopPredictions> topPredictions) {
        double[][] result = new double[topPredictions.size()][topPredictions.size()];
        Set<String> dictionary = new TreeSet<>();  // TreeSet guarantees the order of elements when iterated
        List<WNIDPrediction> predResults;
        String wnId;
        Map<String, Float> predDict;

        // Loop for populating dictionary with all the WNIDs that will be handled
        for (TopPredictions topPrediction : topPredictions) {
            predResults = topPrediction.getResult();

            for (WNIDPrediction wnidPrediction: predResults) {
                wnId = wnidPrediction.getWnId();

                if (!dictionary.contains(wnId)){
                    dictionary.add(wnId);
                }

            }

        }
        double[] v1, v2;
        double v, v1_s, v2_s, corr;
        int d;

        // TODO: loop O(n^2) -> loop O(N*(N-1)/2)
        for (int i=0; i < topPredictions.size(); i++){
            predResults = topPredictions.get(i).getResult();
            predDict = new TreeMap<>();

            for (WNIDPrediction wnIdPred: predResults)
                predDict.put(wnIdPred.getWnId(), wnIdPred.getPrediction());

            v1 = new double[dictionary.size()];

            // Get a vector v1 with all predictions for each WNID handled
            d = 0;
            for(String wnIdDict : dictionary){
                v = 0.0;
                if(predDict.containsKey(wnIdDict))
                    v = predDict.get(wnIdDict);
                v1[d++] = v;
            }

            // Normalize values in v1 by the sum of total
            v1_s=0;
            for (double v1_r : v1) v1_s += v1_r;
            for (int r=0;r<v1.length;r++) v1[r] /= v1_s;

            for(int j=0;j<topPredictions.size();j++){
                // This loop only make sense for different elements
                if(i!=j){
                    predResults = topPredictions.get(j).getResult();
                    predDict = new TreeMap<>();

                    for (WNIDPrediction wnIdPred: predResults)
                        predDict.put(wnIdPred.getWnId(), wnIdPred.getPrediction());

                    v2=new double[dictionary.size()];

                    // Get a vector v2 with all predictions for each WNID handled
                    d = 0;
                    for(String wnIdDict : dictionary){
                        v = 0.0;
                        if(predDict.containsKey(wnIdDict))
                            v = predDict.get(wnIdDict);
                        v2[d++] = v;
                    }

                    // Normalize values in v2 by the sum of total
                    // TODO: is this necessary for correlation?
                    v2_s=0;
                    for (double v2_r : v2) v2_s += v2_r;
                    for (int r=0;r<v2.length;r++) v2[r] /= v2_s;

                    // Now compute the correlation between v1 and v2
                    corr = new PearsonsCorrelation().correlation(v2,v1);
                    corr = (corr + 1)/2.0; // Scale result from range [-1,1] to range [0,1]

                    // Prune correlation values that are less than 'thresholdAffinity'
                    if(corr < thresholdAffinity) corr = 0.0;
                }
                else{
                    // Otherwise, equal elements has correlation equals to 1
                    corr=1.0;
                }
                // Fill affinity matrix with correlation values
                result[i][j] = corr;
            }
        }
        return result;
    }

}
