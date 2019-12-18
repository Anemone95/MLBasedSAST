package top.anemone.mlsast.gui2;

import edu.umd.cs.findbugs.BugInstance;
import top.anemone.mlsast.core.predict.impl.LSTMRemotePredictor;
import top.anemone.mlsast.core.data.TaintFlow;
import top.anemone.mlsast.core.slice.impl.JoanaSlicer;

import java.util.HashMap;
import java.util.Map;

public class AIBasedSpotbugProject {
    private static AIBasedSpotbugProject instance = new AIBasedSpotbugProject();

    private LSTMRemotePredictor server;
    private AIBasedSpotbugProject() {
        server = new LSTMRemotePredictor("http://127.0.0.1:8888/");
    }

    public static AIBasedSpotbugProject getInstance() {
        return instance;
    }

    private JoanaSlicer slicer;

    public void clean(){
        slicer=null;
    }

    public LSTMRemotePredictor getServer() {
        return server;
    }

    public void setServer(LSTMRemotePredictor server) {
        this.server = server;
    }

    public JoanaSlicer getSlicer() {
        return slicer;
    }

    public void setSlicer(JoanaSlicer slicer) {
        this.slicer = slicer;
    }

}
