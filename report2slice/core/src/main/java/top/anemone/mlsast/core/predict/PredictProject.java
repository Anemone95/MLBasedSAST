package top.anemone.mlsast.core.predict;

import top.anemone.mlsast.core.data.taintTree.TaintEdge;
import top.anemone.mlsast.core.slice.SliceProject;

import java.util.HashMap;
import java.util.Map;

public class PredictProject<T> {
    protected Map<T, Boolean> bugInstance2isSafe;
    protected Map<TaintEdge, Boolean> edge2isSafe;
    protected Map<T, Boolean> labelMap;
    protected SliceProject<T> sliceProject;
    public PredictProject(){
        this.bugInstance2isSafe =new HashMap<>();
        this.edge2isSafe =new HashMap<>();
        this.labelMap=new HashMap<>();
    }
    public PredictProject(SliceProject<T> sliceProject){
        this.bugInstance2isSafe =new HashMap<>();
        this.edge2isSafe =new HashMap<>();
        this.labelMap=new HashMap<>();
        this.sliceProject=sliceProject;
    }
    public void putPrediction(T bugInstance, boolean result){
        bugInstance2isSafe.put(bugInstance, result);
    }
    public void putPrediction(TaintEdge edge, Boolean result){
        edge2isSafe.put(edge, result);
    }

    public Boolean getPrediction(T bugInstance){
        return bugInstance2isSafe.get(bugInstance);
    }
    public Boolean getPrediction(TaintEdge edge){
        return edge2isSafe.get(edge);
    }
    public Map<T, Boolean> getPredictions(){
        return bugInstance2isSafe;
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
