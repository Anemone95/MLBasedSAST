package top.anemone.mlsast.core.predict;

import top.anemone.mlsast.core.slice.SliceProject;

import java.util.HashMap;
import java.util.Map;

public class PredictProject<T> {
    protected Map<T, PredictEnum> bugInstance2prediction;
    protected Map<T, Boolean> labelMap;
    protected SliceProject<T> sliceProject;
    public PredictProject(){
        this.bugInstance2prediction=new HashMap<>();
        this.labelMap=new HashMap<>();
    }
    public PredictProject(SliceProject<T> sliceProject){
        this.bugInstance2prediction=new HashMap<>();
        this.labelMap=new HashMap<>();
        this.sliceProject=sliceProject;
    }
    public void putPrediction(T bugInstance, PredictEnum result){
        bugInstance2prediction.put(bugInstance, result);
    }
    public PredictEnum getPrediction(T bugInstance){
        return bugInstance2prediction.getOrDefault(bugInstance, null);
    }
    public Map<T, PredictEnum> getPredictions(){
        return bugInstance2prediction;
    }
    public String getProjectName(){
        return sliceProject.getProjectName();
    }
    public Boolean getLabel(T bugInstance){
        return labelMap.getOrDefault(bugInstance, null);
    }
    public Map<T, Boolean> getLabelMap(){
        return labelMap;
    }
    public void putLabel(T bugInstance, Boolean label){
        labelMap.put(bugInstance, label);
    }

    public void setSliceProject(SliceProject<T> sliceProject) {
        this.sliceProject = sliceProject;
    }
    public SliceProject<T> getSliceProject(){
        return sliceProject;
    }
}
