package com.example.fran.imachineappv2.CIEngine.imagenet;

import com.example.fran.imachineappv2.CIEngine.predictor.Classifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ImageNetUtils {

    public static List<WNIDWords> loadWNIDWords(BufferedReader reader) throws IOException {
        List<WNIDWords> wnidWordsList = new ArrayList<>();
        String line;
        String wnid;
        String words;
        while ((line = reader.readLine()) != null) {
            wnid = line.substring(0,line.lastIndexOf("\t"));
            words = line.substring(line.lastIndexOf("\t")+1);
            wnidWordsList.add(new WNIDWords(wnid,words));
        }
        reader.close();
        return wnidWordsList;
    }

    public static List<HierarchyLookup> loadHierarchyLookup(BufferedReader reader) throws IOException {
        List<HierarchyLookup> hierarchyLookupList = new ArrayList<>();
        String line;
        String w2;
        String w1;
        while ((line = reader.readLine()) != null) {
            w1 = line.substring(0,line.lastIndexOf(" "));
            w2 = line.substring(line.lastIndexOf(" ")+1);
            hierarchyLookupList.add(new HierarchyLookup(w1,w2));
        }
        reader.close();
        return hierarchyLookupList;
    }

    public static String getWNIDFromLabel(String label, List<WNIDWords> wnidWordsList){
        String result="";
        for (WNIDWords wnidWords : wnidWordsList)
            if (label.equals(wnidWords.getWord())){
                result = wnidWords.getWnid();
                break;
            }

        if (result.equals(""))
            System.out.print("ERROR!");

        return result;
    }

    public static String getLabelFromWNID(String wnid, List<WNIDWords> wnidWordsList){
        String result = "";
        for (int i=0;i<wnidWordsList.size();i++){
            if (wnid.equals(wnidWordsList.get(i).getWnid())){
                result = wnidWordsList.get(i).getWord();
                break;
            }
        }
        return result;
    }

    public static ArrayList<String> getFullHierarchy(String wnid, int depth, List<HierarchyLookup> hierarchyLookupList){
        ArrayList<String> results = new ArrayList<>();
        int d=0;
        results.add(wnid);
        while (d<depth){
            for (HierarchyLookup hierarchyLookup: hierarchyLookupList)
                if (wnid.equals(hierarchyLookup.getW2())){
                    results.add(hierarchyLookup.getW1());
                    wnid = hierarchyLookup.getW1();
                }

            d += 1;
        }

        return results;
    }


    public static List<WNIDPrediction> processTopPredictions(List<Classifier.Recognition> results,
                                                      List<WNIDWords> wnidWordsList,
                                                      List<HierarchyLookup> hierarchyLookups,
                                                      int depth, double thresh_prob) {
        List<WNIDPrediction> predictions = new ArrayList<>();
        String wnid;
        ArrayList<String> fullHierarchy;
        for (int i = 0; i< results.size(); i++){
            if (results.get(i).getConfidence()>thresh_prob){
                wnid = getWNIDFromLabel(results.get(i).getTitle(), wnidWordsList);
                fullHierarchy = getFullHierarchy(wnid,depth, hierarchyLookups);
                for (int j=0;j<fullHierarchy.size();j++){
                    predictions.add(new WNIDPrediction(fullHierarchy.get(j), results.get(i).getConfidence()));
                }
            }
        }
        List<WNIDPrediction> result = new ArrayList<>();

        // TODO: adding always the first element?
        result.add(new WNIDPrediction(predictions.get(0).getWnId(),predictions.get(0).getPrediction()));

        for (int i=1; i<predictions.size();i++){
            int d = 0;
            boolean add = true;
            while (d<result.size()){
                if(predictions.get(i).getWnId().equals(result.get(d).getWnId())){
                    result.get(d).setPrediction(result.get(d).getPrediction()+predictions.get(i).getPrediction());
                    add = false;
                    break;
                }
                d+=1;
            }
            if(add){
                result.add(new WNIDPrediction(predictions.get(i).getWnId(),predictions.get(i).getPrediction()));
            }
        }

        Collections.sort(result, new sortByPredicction());  // TODO: always a new variable?

        return result;
    }

    public static class sortByPredicction implements Comparator<WNIDPrediction> {
        public int compare(WNIDPrediction a, WNIDPrediction b)
        {
            return Float.compare(a.getPrediction(), b.getPrediction());
        }
    }

}
