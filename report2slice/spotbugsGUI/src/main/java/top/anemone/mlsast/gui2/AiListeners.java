package top.anemone.mlsast.gui2;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.log.ConsoleLogger;
import edu.umd.cs.findbugs.log.LogSync;
import edu.umd.cs.findbugs.log.Logger;
import org.slf4j.LoggerFactory;
import top.anemone.mlsast.core.Monitor;
import top.anemone.mlsast.core.data.TaintFlow;
import top.anemone.mlsast.core.exception.BCELParserException;
import top.anemone.mlsast.core.exception.NotFoundException;
import top.anemone.mlsast.core.predict.deprecated.SpotbugPredictor;

import javax.swing.*;
import java.io.IOException;
import java.util.*;


public class AiListeners implements LogSync {
    private final Logger logger = new ConsoleLogger(this);
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AiListeners.class);
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
        Monitor monitor = new Monitor() {
            private String stage;

            @Override
            public void init(String stageName, int totalWork) {
                stage = stageName;
                aiAnalyzingDialog.updateStage("Doing "+stageName);
                aiAnalyzingDialog.updateCount(0,totalWork);
            }

            @Override
            public void process(int idx, int totalWork, Object input, Object output, String error) {
                aiAnalyzingDialog.updateCount(idx + 1, totalWork);
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
