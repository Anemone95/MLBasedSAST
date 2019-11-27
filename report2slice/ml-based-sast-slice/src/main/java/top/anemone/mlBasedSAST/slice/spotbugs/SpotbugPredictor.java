package top.anemone.mlBasedSAST.slice.spotbugs;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.anemone.mlBasedSAST.slice.data.AIBasedSpotbugProject;
import top.anemone.mlBasedSAST.slice.data.TaintFlow;
import top.anemone.mlBasedSAST.slice.data.TransformedJar;
import top.anemone.mlBasedSAST.slice.data.VO.Slice;
import top.anemone.mlBasedSAST.slice.exception.BCELParserException;
import top.anemone.mlBasedSAST.slice.exception.CreateDirectoryException;
import top.anemone.mlBasedSAST.slice.exception.NotFoundException;
import top.anemone.mlBasedSAST.slice.exception.RemoteException;
import top.anemone.mlBasedSAST.slice.slice.JoanaSlicer;
import top.anemone.mlBasedSAST.slice.slice.Report2Slice;
import top.anemone.mlBasedSAST.slice.utils.ExceptionUtil;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class SpotbugPredictor {

    private static final Logger LOGGER = LoggerFactory.getLogger(Report2Slice.class);

    public static Map<BugInstance, Boolean> predictFromBugCollection(BugCollection bugCollection, PredictorCallback callback) throws CreateDirectoryException, RemoteException {
        if(!AIBasedSpotbugProject.getInstance().getServer().isAlive()){
            throw new RemoteException(AIBasedSpotbugProject.getInstance().getServer().toString()+"is not alive");
        }

        List<File> appJarsinReport = bugCollection.getProject().getFileList().stream().map(File::new).collect(Collectors.toList());
        List<String> auxClasspath= bugCollection.getProject().getAuxClasspathEntryList();
        List<BugInstance> bugInstances = SpotbugParser.secBugFilter(bugCollection);
        // parse to taint flow
        Map<BugInstance, TaintFlow> bugInstance2Flow = AIBasedSpotbugProject.getInstance().getBugInstanceFlowMap();
        if (bugInstance2Flow.size() != bugInstances.size()) {
            callback.bugInstance2FlowInit(bugInstances);
            for (int i = 0; i < bugInstances.size(); i++) {
                if (bugInstance2Flow.containsKey(bugInstances.get(i))) {
                    continue;
                }
                BugInstance bugInstance = bugInstances.get(i);

                List<TaintFlow> flows = null;
                String error = null;
                try {
                    flows = SpotbugParser.bugInstance2Flow(bugInstance, appJarsinReport);
                } catch (NotFoundException|BCELParserException|IOException e) {
                    error=ExceptionUtil.getStackTrace(e);
                }
                bugInstance2Flow.put(bugInstance, flows.get(0));
                callback.bugInstance2Flow(i, bugInstances, flows, error);
            }
        }

        // prepare
        JoanaSlicer slicer = AIBasedSpotbugProject.getInstance().getSlicer();
        if (slicer == null) {
            callback.unzipJarInit(appJarsinReport);
            Set<String> entryPackages = Report2Slice.getEntryPackages(new ArrayList(bugInstance2Flow.values()));
            Path tempDirectory = null;
            try {
                tempDirectory = Files.createTempDirectory("mlBasedSAST");
            } catch (IOException e) {
                throw new CreateDirectoryException(e.getMessage());
            }
            List<File> transformedAppJars = new LinkedList<>();
            List<URL> libJars = auxClasspath.stream().map(e-> {
                try {
                    return new File(e).toURI().toURL();
                } catch (MalformedURLException ex) { // impossible
                    LOGGER.warn("new URL("+e+") Error:");
                    return null;
                }
            }).collect(Collectors.toList());
            // impossible
            if (libJars.contains(null)){
                throw new NullPointerException("lib jars contains null pointer");
            }
            for (int i = 0; i < appJarsinReport.size(); i++) {
                File appJar = appJarsinReport.get(i);
                TransformedJar jar = null;
                String error=null;
                try {
                    jar = Report2Slice.transformJar(appJar, tempDirectory, entryPackages);
                    transformedAppJars.add(jar.getAppJarPath());
                    for (File f : Objects.requireNonNull(jar.getLibPath().toFile().listFiles())) {
                        libJars.add(f.toURI().toURL());
                    }
                } catch (IOException|InterruptedException e) {
                    error=ExceptionUtil.getStackTrace(e);
                }
                callback.unzipJar(i, appJarsinReport, error);
            }
            callback.generateJoanaConfig();
            libJars.add(Report2Slice.class.getClassLoader().getResource("contrib/servlet-api.jar"));
            slicer = new JoanaSlicer();
            try {
                slicer.generateConfig(transformedAppJars, libJars, null);
            } catch (ClassHierarchyException|IOException e) {
                e.printStackTrace();
            }
            AIBasedSpotbugProject.getInstance().setSlicer(slicer);
        }


        callback.sliceInit(bugInstances);
        Map<BugInstance, String> bugInstance2Slice = AIBasedSpotbugProject.getInstance().getBugInstanceSliceMap();
        for (int i = 0; i < bugInstances.size(); i++) {
            if (bugInstance2Slice.containsKey(bugInstances.get(i))) {
                continue;
            }
            TaintFlow flow = bugInstance2Flow.get(bugInstances.get(i));
            String slice;
            String error=null;
            try {
                slice = slicer.computeSlice(flow);
                bugInstance2Slice.put(bugInstances.get(i), slice);
            } catch (GraphIntegrity.UnsoundGraphException|CancelException|ClassHierarchyException|NotFoundException|IOException e) {
                error=ExceptionUtil.getStackTrace(e);
            }
            callback.slice(i, bugInstances, flow, error);
        }

        callback.predictionInit(bugInstances);
        for (int i = 0; i < bugInstances.size(); i++) {
            TaintFlow flow = bugInstance2Flow.get(bugInstances.get(i));
            String sliceStr = bugInstance2Slice.get(bugInstances.get(i));
            Slice slice = new Slice(flow, sliceStr, flow.getHash(), bugCollection.getProject().getProjectName());
            Boolean isTP  = AIBasedSpotbugProject.getInstance().getServer().predict(slice);
            AIBasedSpotbugProject.getInstance().setBugInstancePrediction(bugInstances.get(i), isTP);
            callback.prediction(i, bugInstances, flow, isTP);
        }
        return AIBasedSpotbugProject.getInstance().getBugInstancePredictionMap();
    }
}
