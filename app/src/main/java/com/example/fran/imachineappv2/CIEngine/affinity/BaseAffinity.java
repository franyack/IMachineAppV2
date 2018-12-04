package com.example.fran.imachineappv2.CIEngine.affinity;

import com.example.fran.imachineappv2.CIEngine.imagenet.TopPredictions;

import java.util.List;

// TODO: to be used?
public abstract class BaseAffinity {
    public abstract double[][] getAffinityMatrix(List<TopPredictions> topPredictionsList);
}
