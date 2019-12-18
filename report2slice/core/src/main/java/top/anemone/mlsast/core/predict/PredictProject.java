package top.anemone.mlsast.core.predict;

import top.anemone.mlsast.core.slice.SliceProject;

import java.util.HashMap;
import java.util.Map;

public class PredictProject<T> {
    private Map<T, Boolean> bugInstance2label;
    private Map<T, PredictEnum> bugInstance2prediction;
    private SliceProject<T> sliceProject;
    public PredictProject(SliceProject<T> sliceProject){
        this.sliceProject=sliceProject;
        this.bugInstance2prediction=new HashMap<>();
    }
    public void putPrediction(T bugInstance, PredictEnum result){
        bugInstance2prediction.put(bugInstance, result);
    }
    public Map<T, PredictEnum> getPredictions(){
        return bugInstance2prediction;
    }
    public String getProjectName(){
        return sliceProject.getProjectName();
    }
}
