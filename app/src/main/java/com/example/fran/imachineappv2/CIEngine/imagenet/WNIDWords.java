package com.example.fran.imachineappv2.CIEngine.imagenet;

public class WNIDWords {
    private String wnid;
    private String words;

    public WNIDWords(String wnid, String word){
        this.wnid = wnid;
        this.words = word;
    }

    public String getWnid(){
        return wnid;
    }

    public String getWord() {
        return words;
    }
}