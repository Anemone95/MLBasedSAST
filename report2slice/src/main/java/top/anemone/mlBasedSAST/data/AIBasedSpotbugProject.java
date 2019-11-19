package top.anemone.mlBasedSAST.data;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.Project;
import top.anemone.mlBasedSAST.data.VO.Label;
import top.anemone.mlBasedSAST.remote.LSTMServer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AIBasedSpotbugProject {
    private static AIBasedSpotbugProject instance = new AIBasedSpotbugProject();

    private AIBasedSpotbugProject() {
        this.bugInstanceTraceMap = new HashMap<>();
        this.bugInstanceLabelMap = new HashMap<>();
        this.bugInstancePredictionMap = new HashMap<>();
    }

    public static AIBasedSpotbugProject getInstance() {
        return instance;
    }
    private Project project;

    private LSTMServer server = new LSTMServer("http://127.0.0.1:8888/");

    private Map<BugInstance, Trace> bugInstanceTraceMap;

    private Map<BugInstance, Boolean> bugInstancePredictionMap;

    private Map<BugInstance, Boolean> bugInstanceLabelMap;

    public LSTMServer getServer() {
        return server;
    }

    public void setServer(LSTMServer server) {
        this.server = server;
    }

    public Map<BugInstance, Trace> getBugInstanceTraceMap() {
        return bugInstanceTraceMap;
    }

    public Boolean getBugInstanceLabel(BugInstance bugInstance){
        return bugInstanceLabelMap.getOrDefault(bugInstance,null);
    }
    public void setBugInstanceLabel(BugInstance bugInstance, boolean isTP) {
        bugInstanceLabelMap.put(bugInstance, isTP);
    }

    public Boolean getBugInstancePrediction(BugInstance bugInstance){
        return bugInstancePredictionMap.getOrDefault(bugInstance,null);
    }
    public void setBugInstancePrediction(BugInstance bugInstance, boolean isTP){
        bugInstancePredictionMap.put(bugInstance, isTP);
    }

//    public void setBugInstanceTraceMap(Map<BugInstance, Trace> bugInstanceTraceMap) {
//        this.bugInstanceTraceMap = bugInstanceTraceMap;
//    }
}
