package top.anemone.mlsast.gui2;

import edu.umd.cs.findbugs.BugInstance;
import top.anemone.mlsast.core.predict.PredictProject;
import top.anemone.mlsast.core.predict.impl.LSTMRemotePredictor;
import top.anemone.mlsast.core.slice.SliceProject;

import java.util.HashSet;
import java.util.Set;

public class AiProject {
    private static AiProject instance = new AiProject();
    private LSTMRemotePredictor remotePredictor = new LSTMRemotePredictor("http://127.0.0.1:8888/");
    private SliceProject<BugInstance> sliceProject;
    private PredictProject<BugInstance> predictProject;
    private PredictProject<BugInstance> labelProject;
    public Set<BugInstance> bugInstanceIsLabeled=new HashSet<>();
    public LabelPredictor labelPredictor = new LabelPredictor();

    private AiProject() {
    }

    public static AiProject getInstance() {
        return instance;
    }

    public boolean isLabeled(BugInstance b){
        return bugInstanceIsLabeled.contains(b);
    }

    public LSTMRemotePredictor getRemotePredictor() {
        return remotePredictor;
    }

    public void setRemotePredictor(LSTMRemotePredictor remotePredictor) {
        this.remotePredictor = remotePredictor;
    }


    public PredictProject<BugInstance> getPredictProject() {
        return predictProject;
    }

    public void setPredictProject(PredictProject<BugInstance> predictProject) {
        this.predictProject = predictProject;
    }


    public SliceProject<BugInstance> getSliceProject() {
        return sliceProject;
    }

    public void setSliceProject(SliceProject<BugInstance> sliceProject) {
        this.sliceProject = sliceProject;
    }

    public Boolean getLabeledIsSafe(BugInstance bug) {
        if (labelProject != null) {
            return labelProject.bugIsSafe(bug);
        } else {
            return null;
        }
    }

    public Boolean getBugInstanceIsSafe(BugInstance bug) {
        if (predictProject != null) {
            return predictProject.bugIsSafe(bug);
        } else {
            return null;
        }

    }

    public PredictProject<BugInstance> getLabelProject() {
        return labelProject;
    }

    public void setLabelProject(PredictProject<BugInstance> labelProject) {
        this.labelProject = labelProject;
    }
}
