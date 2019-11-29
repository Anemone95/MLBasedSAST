package top.anemone.mlBasedSAST.slice.spotbugs;

import edu.umd.cs.findbugs.BugInstance;
import top.anemone.mlBasedSAST.slice.data.TaintFlow;

import java.io.File;
import java.util.List;

public interface PredictorCallback {
    void bugInstance2FlowInit(List<BugInstance> bugInstances);
    void bugInstance2Flow(int idx, List<BugInstance> bugInstances, List<TaintFlow> flows, String error);
    void unzipJarInit(List<File> appJarsinReport);
    void unzipJar(int i, List<File> appJarsinReport, String error);
    void generateJoanaConfig();
    void sliceInit(List<BugInstance> bugInstances);
    void slice(int idx, List<BugInstance> bugInstances, TaintFlow flow, String error);
    void predictionInit(List<BugInstance> bugInstances);
    void prediction(int idx, List<BugInstance> bugInstances, TaintFlow flow, Boolean isTP);
}
