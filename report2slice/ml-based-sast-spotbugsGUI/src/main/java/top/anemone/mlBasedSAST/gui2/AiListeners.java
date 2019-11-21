package top.anemone.mlBasedSAST.gui2;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.log.ConsoleLogger;
import edu.umd.cs.findbugs.log.LogSync;
import edu.umd.cs.findbugs.log.Logger;
import org.slf4j.LoggerFactory;
import top.anemone.mlBasedSAST.slice.data.AIBasedSpotbugProject;
import top.anemone.mlBasedSAST.slice.data.TaintFlow;
import top.anemone.mlBasedSAST.slice.data.TransformedJar;
import top.anemone.mlBasedSAST.slice.data.VO.Slice;
import top.anemone.mlBasedSAST.slice.exception.NotFoundException;
import top.anemone.mlBasedSAST.slice.slice.JoanaSlicer;
import top.anemone.mlBasedSAST.slice.slice.Report2Slice;
import top.anemone.mlBasedSAST.slice.spotbugs.SpotbugParser;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;


public class AiListeners implements LogSync {
    private final Logger logger = new ConsoleLogger(this);
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Report2Slice.class);
    public static final boolean GUI2_DEBUG = SystemProperties.getBoolean("gui2.debug");

    void setServer() {
        AiSetServerDialog dialog = new AiSetServerDialog(MainFrame.getInstance(), logger, true);
        dialog.setLocationRelativeTo(MainFrame.getInstance());
        dialog.setVisible(true);
    }

    void sliceAndPredict() {

        AnalysisCallback ac = new AnalysisCallback() {
            @Override
            public void analysisInterrupted() {
                MainFrame instance = MainFrame.getInstance();
                instance.releaseDisplayWait();
            }

            @Override
            public void analysisFinished(BugCollection results) {
                MainFrame instance = MainFrame.getInstance();
                instance.releaseDisplayWait();
            }
        };
        if (MainFrame.getInstance().getProject().getProjectName() == null) {
            error("No project");
            return;
        }

        AiAnalyzingDialog.show(MainFrame.getInstance().getProject(), ac, false);

//        if (ac.finished) {
//            return ac.getBugCollection();
//        } else {
//            return null;
//        }

    }

    public static void doSliceAndPredict(Project project, AiAnalyzingDialog aiAnalyzingDialog) throws IOException, ClassHierarchyException, InterruptedException {

        BugCollection bugCollection = MainFrame.getInstance().getBugCollection();
        List<File> appJarsinReport = project.getFileList().stream().map(File::new).collect(Collectors.toList());
        List<BugInstance> bugInstances = SpotbugParser.secBugFilter(bugCollection);
        Map<BugInstance, TaintFlow> bugInstance2Flow = AIBasedSpotbugProject.getInstance().getBugInstanceFlowMap();
        // parse to taint flow
        if (bugInstance2Flow.size()!=bugInstances.size()){
            aiAnalyzingDialog.updateStage("Parse bug instances to taint flow");
            for (int i = 0; i < bugInstances.size(); i++) {
                if (bugInstance2Flow.containsKey(bugInstances.get(i))){
                    continue;
                }
                BugInstance bugInstance = bugInstances.get(i);
                try {
                    List<TaintFlow> flows = SpotbugParser.bugInstance2Flow(bugInstance, appJarsinReport);
                    bugInstance2Flow.put(bugInstance, flows.get(0));
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(MainFrame.getInstance(), e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
                }
                aiAnalyzingDialog.updateCount(i + 1, bugInstances.size());
                LOGGER.info("Parsing instance: " + bugInstance);
            }
        }

        // prepare
        JoanaSlicer slicer=AIBasedSpotbugProject.getInstance().getSlicer();
        if (slicer==null){
            aiAnalyzingDialog.updateStage("Unzip jar");
            aiAnalyzingDialog.updateCount(0, appJarsinReport.size());
            Set<String> entryPackages = Report2Slice.getEntryPackages(new ArrayList(bugInstance2Flow.values()));
            Path tempDirectory = Files.createTempDirectory("mlBasedSAST");
            List<File> transformedAppJars = new LinkedList<>();
            List<URL> libJars = new LinkedList<>();
            for (int i = 0; i < appJarsinReport.size(); i++) {
                File appJar = appJarsinReport.get(i);
                TransformedJar jar = Report2Slice.transformJar(appJar, tempDirectory, entryPackages);
                transformedAppJars.add(jar.getAppJarPath());
                for (File f : Objects.requireNonNull(jar.getLibPath().toFile().listFiles())) {
                    libJars.add(f.toURL());
                }
                aiAnalyzingDialog.updateCount(i + 1, appJarsinReport.size());

            }
            aiAnalyzingDialog.updateStage("Generating Joana Config");
            libJars.add(Report2Slice.class.getClassLoader().getResource("contrib/servlet-api.jar"));
            slicer = new JoanaSlicer();
            slicer.generateConfig(transformedAppJars, libJars, null);
            AIBasedSpotbugProject.getInstance().setSlicer(slicer);
        }


        aiAnalyzingDialog.updateStage("Slicing...");
        aiAnalyzingDialog.updateCount(0, bugInstances.size());
        Map<BugInstance, String> bugInstance2Slice = AIBasedSpotbugProject.getInstance().getBugInstanceSliceMap();
        for (int i = 0; i < bugInstances.size(); i++) {
            if (bugInstance2Slice.containsKey(bugInstances.get(i))){
                continue;
            }
            TaintFlow flow = bugInstance2Flow.get(bugInstances.get(i));
            String slice;
            try {
                slice = slicer.computeSlice(flow);
                bugInstance2Slice.put(bugInstances.get(i), slice);
            } catch (NotFoundException | CancelException | GraphIntegrity.UnsoundGraphException e) {
//                JOptionPane.showMessageDialog(MainFrame.getInstance(), "Err when processing this taintFlow: "+ flow+",\n"+e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                continue;
            }
            aiAnalyzingDialog.updateCount(i + 1, bugInstances.size());
        }

        aiAnalyzingDialog.updateStage("Getting Prediction...");
        aiAnalyzingDialog.updateCount(0, bugInstances.size());
        for (int i = 0; i < bugInstances.size(); i++) {
            TaintFlow flow = bugInstance2Flow.get(bugInstances.get(i));
            String sliceStr = bugInstance2Slice.get(bugInstances.get(i));
            Slice slice=new Slice(flow, sliceStr, flow.getHash(), project.toString());
            boolean isTP=AIBasedSpotbugProject.getInstance().getServer().predict(slice);
            AIBasedSpotbugProject.getInstance().setBugInstancePrediction(bugInstances.get(i), isTP);
            aiAnalyzingDialog.updateCount(i + 1, bugInstances.size());
        }

    }

    /**
     * Show an error dialog.
     */
    @Override
    public void error(String message) {
        JOptionPane.showMessageDialog(MainFrame.getInstance(), message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Write a message to stdout.
     */
    @Override
    public void writeToLog(String message) {
        if (GUI2_DEBUG) {
            System.out.println(message);
        }
    }

    public void cleanDB() {
        AIBasedSpotbugProject.getInstance().clean();
        MainFrame.getInstance().getMainFrameTree().updateBugTree();
//        MainFrame.getInstance().newProject();
    }
}
