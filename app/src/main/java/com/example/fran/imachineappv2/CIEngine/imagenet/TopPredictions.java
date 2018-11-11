package com.example.fran.imachineappv2.CIEngine.imagenet;

import java.util.List;

public class TopPredictions {
    // private String imgPath;
    private List<WNIDPrediction> result;

    public TopPredictions(String s, List<WNIDPrediction> wnIdPredictions) {
        // imgPath = s;
        result = wnIdPredictions;
    }


//        public String getImg_path() {
//            return imgPath;
//        }
    public List<WNIDPrediction> getResult() {
    return result;
}
}