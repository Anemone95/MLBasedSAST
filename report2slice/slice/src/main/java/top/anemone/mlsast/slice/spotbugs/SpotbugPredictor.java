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
import top.anemone.mlsast.slice.data.TransformedJar;
import top.anemone.mlsast.slice.data.VO.Slice;
import top.anemone.mlsast.slice.exception.CreateDirectoryException;
import top.anemone.mlsast.slice.exception.RemoteException;
import top.anemone.mlsast.slice.slice.JoanaSlicer;
import top.anemone.mlsast.slice.slice.Report2Slice;
import top.anemone.mlsast.slice.utils.ExceptionUtil;
import top.anemone.mlsast.slice.utils.JarUtil;
import top.anemone.mlsast.slice.exception.BCELParserException;
import top.anemone.mlsast.slice.exception.NotFoundException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class SpotbugPredictor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpotbugPredictor.class);

    public static Map<BugInstance, String> predictFromBugCollection(BugCollection bugCollection, PredictionMonitor callback) throws IOException, NotFoundException, BCELParserException {
        return predictFromBugCollection(bugCollection, callback, null);
    }

    public static Map<BugInstance, String> predictFromBugCollection(BugCollection bugCollection, PredictionMonitor callback, Path tempDirectory) throws IOException, BCELParserException, NotFoundException {
        if (!AIBasedSpotbugProject.getInstance().getServer().isAlive()) {
            throw new RemoteException(AIBasedSpotbugProject.getInstance().getServer().toString() + " is not alive");
        }

        List<File> appJarsinReport = bugCollection.getProject().getFileList().stream().map(File::new).collect(Collectors.toList());
        List<String> auxClasspath = bugCollection.getProject().getAuxClasspathEntryList();
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
                    bugInstance2Flow.put(bugInstance, flows.get(0));
                } catch (NotFoundException | BCELParserException e) {
                    error = ExceptionUtil.getStackTrace(e);
                }
                callback.bugInstance2Flow(i, bugInstances, flows, error);
            }
        }

        // prepare
        JoanaSlicer slicer = AIBasedSpotbugProject.getInstance().getSlicer();
        if (slicer == null) {
            callback.unzipJarInit(appJarsinReport);
            Set<String> entryPackages = Report2Slice.getEntryPackages(new ArrayList(bugInstance2Flow.values()));
            if (tempDirectory == null) {
                try {
                    tempDirectory = Files.createTempDirectory("mlBasedSAST");
                } catch (IOException e) {
                    throw new CreateDirectoryException(e.getMessage());
                }
            }
            List<File> transformedAppJars = new LinkedList<>();
            // TODO lib jar also need transform
            List<File> libJars = auxClasspath.stream().map(File::new).collect(Collectors.toList());
            Set<String> jarsMd5 = new HashSet<>();
            for (int i = 0; i < appJarsinReport.size(); i++) {
                File appJar = appJarsinReport.get(i);
                String jarMD5 = JarUtil.getJarMD5(appJar);
                if (!jarsMd5.contains(jarMD5)) {
                    jarsMd5.add(jarMD5);
                    TransformedJar jar = null;
                    String error = null;
                    try {
                        jar = Report2Slice.transformJar(appJar, tempDirectory, entryPackages);
                        transformedAppJars.add(jar.getAppJarPath());
                        libJars.addAll(Arrays.asList(Objects.requireNonNull(jar.getLibPath().toFile().listFiles())));
                    } catch (IOException | InterruptedException e) {
                        error = ExceptionUtil.getStackTrace(e);
                    }
                    callback.unzipJar(i, appJarsinReport, error);
                }
            }
            callback.generateJoanaConfig();
            libJars.add(new File(JarUtil.getPath() + "/contrib/servlet-api.jar"));
            slicer = new JoanaSlicer();
            // filter same lib
            List<URL> uniLibjars = new LinkedList<>();
            for (File lib : libJars) {
                String jarMD5 = JarUtil.getJarMD5(lib);
                if (!jarsMd5.contains(jarMD5)) {
                    jarsMd5.add(jarMD5);
                    uniLibjars.add(lib.toURL());
                }
            }

            try {
                slicer.generateConfig(transformedAppJars, uniLibjars, null);
            } catch (ClassHierarchyException | IOException e) {
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
            TaintFlow flow = bugInstance2Flow.getOrDefault(bugInstances.get(i), null);
            if (flow == null) {
                continue;
            }
            String slice = null;
            String error = null;
            try {
                slice = slicer.computeSlice(flow);
                bugInstance2Slice.put(bugInstances.get(i), slice);
            } catch (GraphIntegrity.UnsoundGraphException | CancelException | ClassHierarchyException | NotFoundException | IOException | NoSuchElementException | ClassCastException e) {
                error = ExceptionUtil.getStackTrace(e);
            }
            callback.slice(i, bugInstances, flow, slice, error);
        }

        callback.predictionInit(bugInstances);
        for (int i = 0; i < bugInstances.size(); i++) {
            TaintFlow flow = bugInstance2Flow.get(bugInstances.get(i));
            String sliceStr = bugInstance2Slice.get(bugInstances.get(i));
            String isTP = AIBasedSpotbugProject.ERROR;
            if (sliceStr != null) {
                Slice slice = new Slice(flow, sliceStr, flow.getHash(), bugCollection.getProject().getProjectName());
                isTP = Boolean.toString(AIBasedSpotbugProject.getInstance().getServer().predict(slice));
            }
            AIBasedSpotbugProject.getInstance().setBugInstancePrediction(bugInstances.get(i), isTP);
            callback.prediction(i, bugInstances, flow, isTP);
        }
        return AIBasedSpotbugProject.getInstance().getBugInstancePredictionMap();
    }
}
