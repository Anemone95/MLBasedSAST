package top.anemone.mlBasedSAST.slice.data;

import edu.umd.cs.findbugs.BugInstance;
import top.anemone.mlBasedSAST.slice.remote.LSTMServer;
import top.anemone.mlBasedSAST.slice.slice.JoanaSlicer;

import java.util.HashMap;
import java.util.Map;

public class AIBasedSpotbugProject {
    private static AIBasedSpotbugProject instance = new AIBasedSpotbugProject();

    private AIBasedSpotbugProject() {
        this.bugInstanceFlowMap = new HashMap<>();
        this.bugInstanceLabelMap = new HashMap<>();
        this.bugInstancePredictionMap = new HashMap<>();
        this.bugInstanceSliceMap = new HashMap<>();

    }

    public static AIBasedSpotbugProject getInstance() {
        return instance;
    }

    private LSTMServer server = new LSTMServer("http://127.0.0.1:8888/");

    private Map<BugInstance, TaintFlow> bugInstanceFlowMap;

    private Map<BugInstance, Boolean> bugInstancePredictionMap;

    private Map<BugInstance, Boolean> bugInstanceLabelMap;

    private Map<BugInstance, String> bugInstanceSliceMap;

    private JoanaSlicer slicer;

    public void clean(){
        bugInstanceFlowMap.clear();
        bugInstancePredictionMap.clear();
        bugInstanceLabelMap.clear();
        bugInstanceSliceMap.clear();
        slicer=null;
    }

    public LSTMServer getServer() {
        return server;
    }

    public void setServer(LSTMServer server) {
        this.server = server;
    }

    public Map<BugInstance, TaintFlow> getBugInstanceFlowMap() {
        return bugInstanceFlowMap;
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

    public Map<BugInstance, String> getBugInstanceSliceMap() {
        return bugInstanceSliceMap;
    }

    public void setBugInstanceSliceMap(Map<BugInstance, String> bugInstanceSliceMap) {
        this.bugInstanceSliceMap = bugInstanceSliceMap;
    }

    public JoanaSlicer getSlicer() {
        return slicer;
    }

    public void setSlicer(JoanaSlicer slicer) {
        this.slicer = slicer;
    }


//    public void setBugInstanceTraceMap(Map<BugInstance, TaintFlow> bugInstanceFlowMap) {
//        this.bugInstanceFlowMap = bugInstanceFlowMap;
//    }
    public Map<BugInstance, Boolean> getBugInstancePredictionMap(){
        return bugInstancePredictionMap;
    }

}
