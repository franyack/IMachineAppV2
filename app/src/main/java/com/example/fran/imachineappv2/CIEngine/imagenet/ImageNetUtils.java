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
        reader.close();  // TODO: here is not the place to close the reader
        return wnidWordsList;
    }

    public static List<HierarchyLookup> loadHierarchyLookup(BufferedReader reader) throws IOException {
        // TODO: what if this is loaded as a TreeMap to make more efficient the access (it is really needed due to the size)
        List<HierarchyLookup> hierarchyLookupList = new ArrayList<>();
        String line;
        String w2;
        String w1;
        while ((line = reader.readLine()) != null) {
            w1 = line.substring(0,line.lastIndexOf(" "));
            w2 = line.substring(line.lastIndexOf(" ")+1);
            hierarchyLookupList.add(new HierarchyLookup(w1,w2));
        }
        reader.close();  // TODO: here is not the place to close the reader
        return hierarchyLookupList;
    }

    public static String getWNIDFromLabel(String label, List<WNIDWords> wnidWordsList){
        String result="";
        for (WNIDWords wnidWords : wnidWordsList)
            if (label.equals(wnidWords.getWord())){
                result = wnidWords.getWnid();
                break;
            }

        if (result.equals("")) {
            System.out.print(String.format("Error: no WNID found for the label '%s'", label));
            result = null;
        }

        return result;
    }

    /*
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
    */

    public static ArrayList<String> getFullHierarchy(String wnid, int depth, List<HierarchyLookup> hierarchyLookupList){
        ArrayList<String> results = new ArrayList<>();
        int d=0;

        // First add the initial leaf of the tree to retrieve
        results.add(wnid);

        // Then loop from parent to child until the given depth is reached
        while (d<depth){
            for (HierarchyLookup hierarchyLookup: hierarchyLookupList)
                if (wnid.equals(hierarchyLookup.getW2())){
                    results.add(hierarchyLookup.getW1());
                    wnid = hierarchyLookup.getW1();
                    d += 1;
                    // TODO: continue?
                    // TODO: full search? change structure to do it more efficient?

                }

            // d += 1;
        }

        return results;
    }

    // TODO: limit to Top K predictions? or change to processPredictions since it doesn't limit to the top ones
    // TODO: thresholdConfidence instead of thresh_prob
    public static List<WNIDPrediction> processTopPredictions(List<Classifier.Recognition> results,
                                                      List<WNIDWords> wnidWordsList,
                                                      List<HierarchyLookup> hierarchyLookups,
                                                      int depth, double thresh_prob) {
        List<WNIDPrediction> predictions = new ArrayList<>();
        String wnid;
        ArrayList<String> fullHierarchy;

        // Loop to add those predictions that are family of the given results, under depth and thresh_prob restrictions
        for (Classifier.Recognition r : results){
            // Just add those predictions with acceptable confidence
            if (r.getConfidence()>=thresh_prob){
                // First get the WNID corresponding to the given label
                wnid = getWNIDFromLabel(r.getTitle(), wnidWordsList);
                if (wnid == null)
                    // No WNID found for the given label
                    continue;
                // Then look for the related WNIDs on the given hierarchy map
                fullHierarchy = getFullHierarchy(wnid, depth, hierarchyLookups);
                // Finally add each of these WNIDs with the same confidence as the given prediction
                for (String w : fullHierarchy)
                    predictions.add(new WNIDPrediction(w, r.getConfidence()));
            }
        }
        List<WNIDPrediction> result = new ArrayList<>();

        // Now loop to merge confidence values of those predictions having the same WNID
        for (WNIDPrediction prediction : predictions){
            boolean add = true;

            // Look for the given prediction in the current resulting list
            for (WNIDPrediction r : result){
                if(prediction.getWnId().equals(r.getWnId())){
                    // Add the two confidence values for the same WNID
                    r.setPrediction(r.getPrediction()+prediction.getPrediction());
                    add = false;  // since it is already added
                    break;
                }
            }
            if(add)
                // This is a new prediction to be added
                result.add(new WNIDPrediction(prediction.getWnId(),prediction.getPrediction()));
        }

        Collections.sort(result, new sortByPrediction());  // TODO: always a new variable?

        return result;
    }

    public static class sortByPrediction implements Comparator<WNIDPrediction> {
        public int compare(WNIDPrediction a, WNIDPrediction b) {
            return Float.compare(a.getPrediction(), b.getPrediction());
        }
    }

}
