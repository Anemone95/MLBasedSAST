package top.anemone.mlBasedSAST.spotbugs.gui2;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.log.ConsoleLogger;
import edu.umd.cs.findbugs.log.LogSync;
import edu.umd.cs.findbugs.log.Logger;
import top.anemone.mlBasedSAST.data.AIBasedSpotbugProject;
import top.anemone.mlBasedSAST.data.Trace;
import top.anemone.mlBasedSAST.exception.BCELParserException;
import top.anemone.mlBasedSAST.exception.NotFoundException;
import top.anemone.mlBasedSAST.spotbugs.SpotbugParser;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


public class AiListeners implements LogSync {
    private final Logger logger = new ConsoleLogger(this);
    public static final boolean GUI2_DEBUG = SystemProperties.getBoolean("gui2.debug");

    void setServer() {
        AiSetServerDialog dialog = new AiSetServerDialog(MainFrame.getInstance(), logger, true);
        dialog.setLocationRelativeTo(MainFrame.getInstance());
        dialog.setVisible(true);
    }
    void sliceAndPredict(){
        BugCollection bugCollection=MainFrame.getInstance().getBugCollection();
        Project project=MainFrame.getInstance().getProject();
        List<File> appJarsinReport=project.getFileList().stream().map(File::new).collect(Collectors.toList());
        List<BugInstance> bugInstances = SpotbugParser.secBugFilter(bugCollection);
        // parse to traces
        for (BugInstance bugInstance : bugInstances) {
            try {
                List<Trace> traces = SpotbugParser.bugInstance2Trace(bugInstance, appJarsinReport);
                AIBasedSpotbugProject.getInstance().getBugInstanceTraceMap().put(bugInstance, traces.get(0));
            } catch (Exception e) {
                error(e.toString());
            }
            System.out.println("Parsing instance: "+bugInstance);
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
}
