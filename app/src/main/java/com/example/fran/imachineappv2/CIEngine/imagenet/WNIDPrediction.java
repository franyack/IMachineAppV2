package com.example.fran.imachineappv2.CIEngine.imagenet;

public class WNIDPrediction{
    private String wnId;
    private float prediction;

    public WNIDPrediction(String wnId, float prediction){
        // TODO: confidence instead of prediction
        this.wnId = wnId;
        this.prediction = prediction;
    }

    public String getWnId() {
        return wnId;
    }

    public float getPrediction() {
        return prediction;
    }

    public void setPrediction(float prediction) {
        this.prediction = prediction;
    }
}