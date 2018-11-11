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
            v1 = new double[embed.length];

            for (int r=0;r<v1.length;r++) v1[r] = (double) embed[r];

            for(int j=0;j<vector.size();j++){
                if(i!=j){
                    embed = vector.get(j);
                    v2 = new double[embed.length];

                    for (int r=0;r<v2.length;r++) v2[r] = (double) embed[r];

                    corr = new PearsonsCorrelation().correlation(v2,v1);
                    corr = (corr + 1)/2.0; //Normalize output
                    if(corr < thresholdAffinity) corr = 0.0;
                }else{
                    corr=1;
                }
                result[i][j] = corr;
            }

        }
        return result;
    }
}
