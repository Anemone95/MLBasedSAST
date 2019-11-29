package top.anemone.mlBasedSAST.gui2;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
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
import top.anemone.mlBasedSAST.slice.exception.BCELParserException;
import top.anemone.mlBasedSAST.slice.exception.NotFoundException;
import top.anemone.mlBasedSAST.slice.slice.Report2Slice;
import top.anemone.mlBasedSAST.slice.spotbugs.PredictorCallback;
import top.anemone.mlBasedSAST.slice.spotbugs.SpotbugPredictor;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.*;


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

    public static void doSliceAndPredict(Project project, AiAnalyzingDialog aiAnalyzingDialog) throws IOException, NotFoundException, BCELParserException {
        PredictorCallback callback=new PredictorCallback() {
            @Override
            public void bugInstance2FlowInit(List<BugInstance> bugInstances) {
                aiAnalyzingDialog.updateStage("Parse bug instances to taint flow");
            }

            @Override
            public void bugInstance2Flow(int idx, List<BugInstance> bugInstances, List<TaintFlow> flows, String error) {
                aiAnalyzingDialog.updateCount(idx + 1, bugInstances.size());
                LOGGER.info("Parsing instance: " + bugInstances.get(idx));
            }

            @Override
            public void unzipJarInit(List<File> appJarsinReport) {
                aiAnalyzingDialog.updateStage("Unzip jar");
                aiAnalyzingDialog.updateCount(0, appJarsinReport.size());
            }

            @Override
            public void unzipJar(int i, List<File> appJarsinReport, String error) {
                aiAnalyzingDialog.updateCount(i + 1, appJarsinReport.size());
            }

            @Override
            public void generateJoanaConfig() {
                aiAnalyzingDialog.updateStage("Generating Joana Config");
                aiAnalyzingDialog.updateCount(0,1);
            }

            @Override
            public void sliceInit(List<BugInstance> bugInstances) {
                aiAnalyzingDialog.updateStage("Slicing...");
                aiAnalyzingDialog.updateCount(0, bugInstances.size());
            }

            @Override
            public void slice(int idx, List<BugInstance> bugInstances, TaintFlow flow, String error) {
                aiAnalyzingDialog.updateCount(idx + 1, bugInstances.size());
            }

            @Override
            public void predictionInit(List<BugInstance> bugInstances) {
                aiAnalyzingDialog.updateStage("Getting Prediction...");
                aiAnalyzingDialog.updateCount(0, bugInstances.size());
            }

            @Override
            public void prediction(int idx, List<BugInstance> bugInstances, TaintFlow flow, Boolean isTP) {
                aiAnalyzingDialog.updateCount(idx + 1, bugInstances.size());
            }
        };

        BugCollection bugCollection = MainFrame.getInstance().getBugCollection();
        SpotbugPredictor.predictFromBugCollection(bugCollection, callback);
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
