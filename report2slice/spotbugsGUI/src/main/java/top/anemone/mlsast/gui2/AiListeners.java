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
import top.anemone.mlsast.core.exception.*;
import top.anemone.mlsast.core.parser.impl.SpotbugBugCollectionParser;
import top.anemone.mlsast.core.predict.PredictRunner;
import top.anemone.mlsast.core.predict.exception.PredictorException;
import top.anemone.mlsast.core.slice.SliceRunner;
import top.anemone.mlsast.core.slice.impl.JoanaSlicer;
import top.anemone.mlsast.core.utils.ExceptionUtil;

import javax.swing.*;
import java.util.LinkedList;
import java.util.List;


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

    public static List<Exception> doSliceAndPredict(Project project, AiAnalyzingDialog aiAnalyzingDialog) throws NotFoundException,
            PredictorRunnerException, ParserException, SliceRunnerException {
        List<Exception> exceptions = new LinkedList<>();
        Monitor monitor = new Monitor() {
            private String stage;

            @Override
            public void init(String stageName, int totalWork) {
                stage = stageName;
                aiAnalyzingDialog.updateStage("Doing " + stageName);
                aiAnalyzingDialog.updateCount(0, totalWork);
            }

            @Override
            public void process(int idx, int totalWork, Object input, Object output, Exception exception) {
                aiAnalyzingDialog.updateCount(idx, totalWork);
                if (exception != null) {
                    GuiUtil.addExceptions(exception, exceptions);
                }
            }
        };

        BugCollection bugCollection = MainFrame.getInstance().getBugCollection();

        if (AiProject.getInstance().getSliceProject() == null) {
            AiProject.getInstance().setSliceProject(
                    new SliceRunner<BugInstance>()
                            .setReportParser(new SpotbugBugCollectionParser(bugCollection))
                            .setSlicer(new JoanaSlicer())
                            .run(monitor)
            );
        }

        AiProject.getInstance().setPredictProject(
                new PredictRunner<>(AiProject.getInstance().getSliceProject())
                        .setReportParser(new SpotbugBugCollectionParser(bugCollection))
                        .setPredictor(AiProject.getInstance().getRemotePredictor())
                        .run(monitor)
        );
        return exceptions;
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
        AiProject.getInstance().setPredictProject(null);
        AiProject.getInstance().setSliceProject(null);
        AiProject.getInstance().setLabelProject(null);
        MainFrame.getInstance().getMainFrameTree().updateBugTree();
//        MainFrame.getInstance().newProject();
    }
}
