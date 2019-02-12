package com.example.fran.imachineappv2.CIEngine.clustering;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.NormOps_DDRM;
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.ejml.equation.Equation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Logger;

// See https://github.com/GuyAllard/markov_clustering/blob/master/markov_clustering/mcl.py

public class MCLDenseEJML {
    private int maxIterations = 100;
    private int expansionPow = 2;
    private int inflationPow = 2;
    private double epsilon = 1e-3;
    private double thresholdPrune = 0.01;

    private static final Logger LOGGER = Logger.getLogger(MCLDenseEJML.class.getName());

    public MCLDenseEJML(int maxIterations, int expansionPow, int inflationPow, double epsilon, double thresholdPrune){
        this.maxIterations = maxIterations;
        this.expansionPow = expansionPow;
        this.inflationPow = inflationPow;
        this.epsilon = epsilon;
        this.thresholdPrune = thresholdPrune;
    }

    public MCLDenseEJML(){

    }

    private DMatrixRMaj prune(DMatrixRMaj M){
        DMatrixRMaj R = M.copy();

        for (int i=0; i < M.numRows; i++) {
            for (int j = 0; j < M.numCols; j++) {
                if(Double.isNaN(M.get(i,j)) || Double.isInfinite(M.get(i,j))){
                    if(i==j){
                        R.set(i, j, 1.0);
                    }else{
                        R.set(i, j, 0.0);
                    }
                }else{
                    if (M.get(i, j) <= thresholdPrune) {
                        R.set(i, j, 0.0);
                    }
                }
            }
        }
        return R;
    }

    private void normalize(DMatrixRMaj M) {
        // TODO: similar method that returns a matrix, avoiding modifications on the given one
        // TODO: handle division by zero when having rows with all zeros
        double suma=0;
        Equation eq = new Equation();
        eq.alias(M, "M");
        eq.alias(suma,"s");

//        for (int i=0;i<M.numRows;i++) {
//            for (int j = 0; j < M.numCols; j++) {
//                eq.alias(i, "i");
//                eq.alias(j, "j");
//                eq.process("s = s + M(i,j)");
////                suma = suma + M.get(i,j);
//            }
//        }
//        for (int i=0;i<M.numRows;i++){
//            for (int j=0;j<M.numCols;j++){
////                eq.alias(i,"i");
////                eq.alias(j,"j");
////                eq.process("s = s + M(i,j)");
//                M.set(i,j,M.get(i,j)/suma);
//            }
        for(int i=0; i < M.numRows; i++){
            eq.alias(i, "i");
            //eq.process("M(i,:) = exp(M(i,:) - max(M(i,:)))");  // to prevent high values in exp(M)
            //eq.process("M(i,:) = M(i,:) / sum(M(i,:))");
            eq.process("M(i,:) = M(i,:) / sum(M(i,:))");
//            eq.process("M(i,:) = M(i,:) / s");
        }
    }

    private void normalizeF(DMatrixRMaj M) {
        NormOps_DDRM.normalizeF(M);
    }

    private void expand(DMatrixRMaj M){
        // TODO: similar method that returns a matrix, avoiding modifications on the given one
        Equation eq = new Equation();
        eq.alias(M, "M");

        for(int i=0; i < this.expansionPow-1; i++){
            eq.process("M = M * M");
        }
    }

    private void inflate(DMatrixRMaj M){
        // TODO: similar method that returns a matrix, avoiding modifications on the given one
        Equation eq = new Equation();
        eq.alias(M, "M", this.inflationPow, "p");

        for(int i=0; i < M.numRows; i++){
            eq.alias(i, "i");
            eq.process("M(i,:) = M(i,:) .^ p");

        }

    }

    private double deltaAbs(DMatrixRMaj M1, DMatrixRMaj M2){
        // assert M1.numRows == M2.numRows && M1.numCols == M2.numCols;
        int nElements = M1.numCols*M1.numCols;

        Equation eq = new Equation();
        eq.alias(M1, "M1", M2, "M2", nElements, "n");

        eq.process("d = sum(abs(M2 - M1)) / n");

        return eq.lookupDouble("d");
    }


    public DMatrixRMaj run(DMatrixRMaj M){
        // TODO: similar method that returns a matrix, avoiding modifications on the given one

        Equation eq = new Equation();
        eq.alias(M, "M");

        // First, fill diagonal of M with 1s (self loops)
        for (int i=0; i < M.numRows; i++)
            M.set(i, i, 1.0);

        // Then, normalize M
        this.normalize(M);
        M = this.prune(M);

        boolean converged = false;
        DMatrixRMaj Mprev = M.copy();

        int i = 1;

        long startLoop = System.nanoTime();

        while (!converged && i < this.maxIterations){
            // TODO: use a verbose flag
            long startIteration = System.nanoTime();

            this.expand(M);
            this.inflate(M);
            this.normalize(M);
            M = this.prune(M);

            double tIteration = (System.nanoTime() - startIteration) / 1e6;

            double delta = this.deltaAbs(M,  Mprev);

//            LOGGER.info(String.format("Iteration %d took %f ms", i, tIteration));
//            LOGGER.info(String.format("Delta: %f", delta));

            //M.print();

            Mprev = M.copy();

            converged = delta < this.epsilon;

            i += 1;
        }

        double tLoop = (System.nanoTime() - startLoop) / 1e9;

//        LOGGER.info(String.format("Total process took %f seconds", tLoop));

        return M;

    }

    // TODO: think on another method to perform weighted average
    public static DMatrixRMaj averageMatrices(List<DMatrixRMaj> matList){
        int n = matList.size(); // n matrices to average

        // Create resulting matrix
        DMatrixRMaj res = new DMatrixRMaj(matList.get(0).numRows, matList.get(0).numCols);

        // Fill matrix with all zeros
        res.zero();

        // Loop to add matrices
        for (DMatrixRMaj mat: matList) {
            // TODO: assert mat.size() == res.size() ?
            CommonOps_DDRM.add(res, mat, res);
        }

        // Divide by total of matrices
        CommonOps_DDRM.divide(res, (float) n);

        return res;
    }

    public DMatrixRMaj generateRandomAffinityMatrix(int n, int seed){
        Random rand = new Random(seed);

        return RandomMatrices_DDRM.symmetric(n, 0.0, 1.0, rand);  // TODO: new here?
    }

    public ArrayList<ArrayList<Integer>> getClusters(DMatrixRMaj M){
        int n = M.numRows;  // TODO: assert square matrix

        ArrayList<ArrayList<Integer>> clusters = new ArrayList<>();

        boolean add;

        for(int i=0; i < n; i++){

            if(M.get(i, i) < thresholdPrune) continue;

            ArrayList<Integer> cluster = new ArrayList<>();

            for(int j=0; j < n; j++){
                add = true;
                if(M.get(j, i) >= thresholdPrune){
                    if (clusters.size()>0){
                        for (int l=0;l<clusters.size();l++){
                            for (int k=0;k<clusters.get(l).size();k++){
                                if(clusters.get(l).get(k)==j){
                                    add=false;
                                    break;
                                }
                            }
                        }
                    }
                    if(add){
                        cluster.add(j);
                    }

                }
            }

            if (cluster.size() > 0){
                clusters.add(cluster);
            }
        }

        return clusters;
    }

    public static void postCluster(List<Integer> clusters, DMatrixRMaj affinityMatrix) {
        /**
         * Function to perform a post-cluster to merge one-image clusters with the most similar ones
         * INPLACE
         */

        // Get those images contained in a one-image cluster

        // TODO: this could be improved with a proper structure based on Map to get O(1) access
        // -> then we could avoid the unnecessary loop

        // Not using threshold to get always the most similar cluster despite the similarity
        // NOTE: now that there is a cluster of 'others', a threshold may be convenient to
        // define how similar a image should be to be matched with a cluster or to be in 'others'

        List<Integer> imagesNotClustered = new ArrayList<>();
        int size;

        for(int i=0;i<clusters.size();i++){
            size=0;

            for(int j=0; j<clusters.size();j++){
                if(Objects.equals(clusters.get(i), clusters.get(j))){
                    size++;
                }
            }

            if(size==1){
                // Then this image is part of a one-image cluster
                imagesNotClustered.add(i);
            }
        }

        // Now for each single image, get the most similar cluster to put the image there
        int maxAffIdx;

        List<Integer> othersCluster = new ArrayList<>();

        for(int imageIdx:imagesNotClustered){
            // TODO: most similar image, or most similar cluster (in avg) ??
            maxAffIdx=getIndexMaxAffinity(imageIdx, affinityMatrix);

            if (maxAffIdx == -1){
                // Put this image in a single cluster for all the images with no similar cluster
                othersCluster.add(imageIdx);
                continue;
            }

            // Otherwise, set a the new closest cluster to the given image
            clusters.set(imageIdx, clusters.get(maxAffIdx));

        }

        // Finally, add the "others" cluster having the max index of clusters
        int othersClusterIdx = Collections.max(clusters) + 1;

        for(int imageIdx:othersCluster)
            clusters.set(imageIdx, othersClusterIdx);
    }

    private static int getIndexMaxAffinity(int idx, DMatrixRMaj affinityMatrix) {

        double maxAff=0.0;
        int maxAffIdx=-1;

        for(int j=0;j<affinityMatrix.numCols;j++){
            if(idx!=j){
                if(affinityMatrix.get(idx,j)>maxAff){
                    maxAff=affinityMatrix.get(idx,j);
                    maxAffIdx = j;
                }
            }
        }
        return maxAffIdx;
    }

    /*
    public static void main(String[] args){
        int maxIt = 100;
        int expPow = 2;
        int infPow = 2;
        double epsConvergence = 1e-3;
        double threshPrune = 0.01;
        int n = 100;
        int seed = 1234;

        MCLDenseEJML mcl = new MCLDenseEJML(maxIt, expPow, infPow, epsConvergence, threshPrune);


        double[][] data = {
                {1.0, 0.5, 0.0, 0.5, 0.0, 0.0},
                {0.5, 1.0, 0.0, 0.0, 0.0, 0.0},
                {0.0, 0.0, 1.0, 0.0, 0.2, 0.0},
                {0.5, 0.0, 0.0, 1.0, 0.0, 0.0},
                {0.0, 0.0, 0.2, 0.0, 1.0, 0.0},
                {0.0, 0.0, 0.0, 0.0, 0.0, 1.0}
        };


        DMatrixRMaj m = new DMatrixRMaj(data);

        //DMatrixRMaj m = mcl.generateRandomAffinityMatrix(n, seed);

        //m.print();
        m = mcl.run(m);
        //m.print();

        ArrayList<ArrayList<Integer>> clusters = mcl.getClusters(m);

        LOGGER.info(String.format("Clusters obtained: %s", clusters));
    }
    */

}
