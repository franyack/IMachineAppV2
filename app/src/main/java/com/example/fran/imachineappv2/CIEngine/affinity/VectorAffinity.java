package com.example.fran.imachineappv2.CIEngine.affinity;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import java.util.List;

// TODO: extends BaseAffinity?
public class VectorAffinity {
    private double thresholdAffinity;

    public VectorAffinity(double thresholdAffinity){
        this.thresholdAffinity = thresholdAffinity;
    }

    // TODO: return a EJML matrix (more efficient)
    public double[][] getAffinityMatrix(List<float[]> vector) {
        double[][] result = new double[vector.size()][vector.size()];
        float [] embed;
        double[] v1, v2;
        double corr;

        // TODO: loop O(n^2) -> loop O(N*(N-1)/2)
        for (int i=0; i < vector.size(); i++){
            embed = vector.get(i);

            // Get v1 from embed elements
            v1 = new double[embed.length];
            for (int r=0;r<v1.length;r++) v1[r] = (double) embed[r];

            for(int j=0;j<vector.size();j++){
                // This loop only make sense for different elements
                if(i!=j){
                    embed = vector.get(j);

                    // Get v1 from embed elements
                    v2 = new double[embed.length];
                    for (int r=0;r<v2.length;r++) v2[r] = (double) embed[r];

                    // Now compute the correlation between v1 and v2
                    corr = new PearsonsCorrelation().correlation(v2,v1);
                    corr = (corr + 1)/2.0; // Scale result from range [-1,1] to range [0,1]

                    // Prune correlation values that are less than 'thresholdAffinity'
                    if(corr < thresholdAffinity) corr = 0.0;
                }else{
                    // Otherwise, equal elements has correlation equals to 1
                    corr=1;
                }
                // Fill affinity matrix with correlation values
                result[i][j] = corr;
            }

        }
        return result;
    }
}
