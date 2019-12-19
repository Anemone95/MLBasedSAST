package top.anemone.mlsast.gui2;

import edu.umd.cs.findbugs.BugInstance;
import top.anemone.mlsast.core.predict.PredictEnum;
import top.anemone.mlsast.core.predict.PredictProject;
import top.anemone.mlsast.core.predict.impl.LSTMRemotePredictor;
import top.anemone.mlsast.core.slice.SliceProject;

import java.util.HashMap;
import java.util.Map;

public class AiProject {
    private static AiProject instance = new AiProject();
    private LSTMRemotePredictor server = new LSTMRemotePredictor("http://127.0.0.1:8888/");
    private SliceProject<BugInstance> sliceProject;
    private PredictProject<BugInstance> predictProject;

    private AiProject() {
    }

    public static AiProject getInstance() {
        return instance;
    }

    public LSTMRemotePredictor getServer() {
        return server;
    }

    public void setServer(LSTMRemotePredictor server) {
        this.server = server;
    }


    public PredictProject<BugInstance> getPredictProject() {
        return predictProject;
    }

    public void setPredictProject(PredictProject<BugInstance> predictProject) {
        this.predictProject = predictProject;
    }

    public Map<BugInstance, Boolean> getLabelMap() {
        return predictProject.getLabelMap();
    }

//    public void setLabelMap(Map<BugInstance, Boolean> labelMap) {
//        this.labelMap = labelMap;
//    }

    public SliceProject<BugInstance> getSliceProject() {
        return sliceProject;
    }

    public void setSliceProject(SliceProject<BugInstance> sliceProject) {
        this.sliceProject = sliceProject;
    }

    public Boolean getBugInstanceLabel(BugInstance bug) {
        if (predictProject!=null){
            return predictProject.getLabel(bug);
        } else {
            return null;
        }
    }

    public PredictEnum getBugInstancePrediction(BugInstance bug) {
        if (predictProject!=null){
            return predictProject.getPrediction(bug);
        } else {
            return null;
        }

    }
}
