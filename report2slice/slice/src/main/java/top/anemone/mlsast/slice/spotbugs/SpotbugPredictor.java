package top.anemone.mlsast.slice.spotbugs;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.anemone.mlsast.slice.data.AIBasedSpotbugProject;
import top.anemone.mlsast.slice.data.TaintFlow;
import top.anemone.mlsast.slice.data.VO.Slice;
import top.anemone.mlsast.slice.exception.RemoteException;
import top.anemone.mlsast.slice.slice.JoanaSlicer;
import top.anemone.mlsast.slice.utils.ExceptionUtil;
import top.anemone.mlsast.slice.exception.BCELParserException;
import top.anemone.mlsast.slice.exception.NotFoundException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SpotbugPredictor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpotbugPredictor.class);

    public static Map<BugInstance, String> predictFromBugCollection(BugCollection bugCollection, PredictionMonitor monitor) throws IOException, BCELParserException, NotFoundException {
        if (!AIBasedSpotbugProject.getInstance().getServer().isAlive()) {
            throw new RemoteException(AIBasedSpotbugProject.getInstance().getServer().toString() + " is not alive");
        }

        List<File> appJarsinReport = bugCollection.getProject().getFileList().stream().map(File::new).collect(Collectors.toList());
        List<BugInstance> bugInstances = SpotbugParser.secBugFilter(bugCollection);
        // parse to taint flow
        Map<BugInstance, TaintFlow> bugInstance2Flow = AIBasedSpotbugProject.getInstance().getBugInstanceFlowMap();
        if (bugInstance2Flow.size() != bugInstances.size()) {
            monitor.bugInstance2FlowInit(bugInstances);
            for (int i = 0; i < bugInstances.size(); i++) {
                if (bugInstance2Flow.containsKey(bugInstances.get(i))) {
                    continue;
                }
                BugInstance bugInstance = bugInstances.get(i);

                List<TaintFlow> flows = null;
                String error = null;
                try {
                    flows = SpotbugParser.bugInstance2Flow(bugInstance, appJarsinReport);
                    bugInstance2Flow.put(bugInstance, flows.get(0));
                } catch (NotFoundException | BCELParserException e) {
                    error = ExceptionUtil.getStackTrace(e);
                }
                monitor.bugInstance2Flow(i, bugInstances, flows, error);
            }
        }

        // prepare
        JoanaSlicer slicer = AIBasedSpotbugProject.getInstance().getSlicer();
        if (slicer == null) {
            monitor.generateJoanaConfig();
            slicer = new JoanaSlicer();
            try {
                slicer.generateConfig(appJarsinReport, new LinkedList<>(), null);
            } catch (ClassHierarchyException | IOException e) {
                e.printStackTrace();
            }
            AIBasedSpotbugProject.getInstance().setSlicer(slicer);
        }


        monitor.sliceInit(bugInstances);
        Map<BugInstance, String> bugInstance2Slice = AIBasedSpotbugProject.getInstance().getBugInstanceSliceMap();
        for (int i = 0; i < bugInstances.size(); i++) {
            if (bugInstance2Slice.containsKey(bugInstances.get(i))) {
                continue;
            }
            TaintFlow flow = bugInstance2Flow.getOrDefault(bugInstances.get(i), null);
            if (flow == null) {
                continue;
            }
            String slice = null;
            String error = null;
            try {
                slice = slicer.computeSlice(flow);
                bugInstance2Slice.put(bugInstances.get(i), slice);
            } catch (NoSuchElementException | ClassCastException | IllegalStateException | GraphIntegrity.UnsoundGraphException | CancelException |NotFoundException e) {
                error = ExceptionUtil.getStackTrace(e);
            }
            monitor.slice(i, bugInstances, flow, slice, error);
        }
        monitor.predictionInit(bugInstances);
        for (int i = 0; i < bugInstances.size(); i++) {
            TaintFlow flow = bugInstance2Flow.get(bugInstances.get(i));
            String sliceStr = bugInstance2Slice.get(bugInstances.get(i));
            String isTP = AIBasedSpotbugProject.ERROR;
            if (sliceStr != null) {
                Slice slice = new Slice(flow, sliceStr, flow.getHash(), bugCollection.getProject().getProjectName());
                isTP = Boolean.toString(AIBasedSpotbugProject.getInstance().getServer().predict(slice));
            }
            AIBasedSpotbugProject.getInstance().setBugInstancePrediction(bugInstances.get(i), isTP);
            monitor.prediction(i, bugInstances, flow, isTP);
        }
        return AIBasedSpotbugProject.getInstance().getBugInstancePredictionMap();
    }
}
