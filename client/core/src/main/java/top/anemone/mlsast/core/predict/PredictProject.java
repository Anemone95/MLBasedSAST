package top.anemone.mlsast.core.predict;

import top.anemone.mlsast.core.data.taintTree.TaintFlow;
import top.anemone.mlsast.core.slice.SliceProject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PredictProject<T> {
    protected Map<T, Boolean> bugInstance2isSafe;
    protected Map<T, Set<TaintFlow>> bug2proof; //证明bug是误报，需要列举清洁的边
    protected Map<TaintFlow, Boolean> flowIsSafe;
    protected SliceProject<T> sliceProject;
    private Map<T, List<Exception>> errorBugsExceptions;
    public PredictProject(){
        this.bugInstance2isSafe =new HashMap<>();
        this.flowIsSafe =new HashMap<>();
        this.errorBugsExceptions=new HashMap<>();
        this.bug2proof=new HashMap<>();
    }
    public PredictProject(SliceProject<T> sliceProject){
        this();
        this.sliceProject=sliceProject;
    }
    public void putExceptions(T bugInstance, List<Exception> exceptions){
        errorBugsExceptions.put(bugInstance, exceptions);
    }
    public List<Exception> getExceptions(T bug){
        return errorBugsExceptions.get(bug);
    }
    public void putPrediction(T bugInstance, boolean result){
        bugInstance2isSafe.put(bugInstance, result);
    }
    public void putPrediction(TaintFlow edge, Boolean result){
        flowIsSafe.put(edge, result);
    }

    public Boolean bugIsSafe(T bugInstance){
        return bugInstance2isSafe.get(bugInstance);
    }
    public Boolean bugIsSafe(TaintFlow edge){
        return flowIsSafe.get(edge);
    }
    public Map<T, Boolean> getPredictions(){
        return bugInstance2isSafe;
    }
    public String getProjectName(){
        return sliceProject.getProjectName();
    }

    public void setSliceProject(SliceProject<T> sliceProject) {
        this.sliceProject = sliceProject;
    }
    public SliceProject<T> getSliceProject(){
        return sliceProject;
    }

    public void putProofs(T bug, Set<TaintFlow> proofs){
        bug2proof.put(bug, proofs);
    }
    public Set<TaintFlow> getProofs(T bug){
        return bug2proof.get(bug);
    }
}
