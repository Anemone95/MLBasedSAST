package top.anemone.mlsast.slice.spotbugs;

import edu.umd.cs.findbugs.BugInstance;
import top.anemone.mlsast.slice.data.TaintFlow;

import java.io.File;
import java.util.List;

public interface PredictionMonitor {
    void bugInstance2FlowInit(List<BugInstance> bugInstances);
    void bugInstance2Flow(int idx, List<BugInstance> bugInstances, List<TaintFlow> flows, String error);
    void unzipJarInit(List<File> appJarsinReport);
    void unzipJar(int i, List<File> appJarsinReport, String error);
    void generateJoanaConfig();
    void sliceInit(List<BugInstance> bugInstances);
    void slice(int idx, List<BugInstance> bugInstances, TaintFlow flow, String slice, String error);
    void predictionInit(List<BugInstance> bugInstances);
    void prediction(int idx, List<BugInstance> bugInstances, TaintFlow flow, String isTP);
}
