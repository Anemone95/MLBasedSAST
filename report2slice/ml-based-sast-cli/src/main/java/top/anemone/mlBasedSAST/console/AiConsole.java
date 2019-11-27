package top.anemone.mlBasedSAST.console;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.PluginException;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sourceforge.argparse4j.ArgumentParsers;
import top.anemone.mlBasedSAST.slice.data.AIBasedSpotbugProject;
import top.anemone.mlBasedSAST.slice.data.TaintFlow;
import top.anemone.mlBasedSAST.slice.remote.LSTMServer;
import top.anemone.mlBasedSAST.slice.spotbugs.PredictorCallback;
import top.anemone.mlBasedSAST.slice.spotbugs.SpotbugParser;
import top.anemone.mlBasedSAST.slice.spotbugs.SpotbugPredictor;
import top.anemone.mlBasedSAST.slice.utils.JsonUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class AiConsole {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiConsole.class);

    public static void main(String[] args) throws PluginException, IOException, DocumentException {
        ArgumentParser parser = ArgumentParsers.newFor("AiConsole").build()
                .defaultHelp(true);
        parser.addArgument("-f", "--results-file").required(true)
                .help("Specify SpotBugs analysis results(.xml)");
        parser.addArgument("-s", "--server").setDefault("http://127.0.0.1:8888")
                .help("Specify prediction server");
        parser.addArgument("-o", "--output")
                .help("Specify prediction output file");
        Namespace ns=null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }
        AIBasedSpotbugProject.getInstance().setServer(new LSTMServer(ns.getString("server")));
        getPredictionFromXML(ns.getString("results_file"));
        if (ns.getString("output")==null){
            JsonUtil.toJson(AIBasedSpotbugProject.getInstance().getBugInstancePredictionMap());
        } else {
            JsonUtil.dumpToFile(AIBasedSpotbugProject.getInstance().getBugInstancePredictionMap(), ns.getString("output"));
        }

    }
    public static void getPredictionFromXML(String xmlFile) throws IOException, PluginException, DocumentException {

        PredictorCallback callback=new PredictorCallback() {
            @Override
            public void bugInstance2FlowInit(List<BugInstance> bugInstances) {
                LOGGER.info("Parsing bug instances to taint flow");
            }

            @Override
            public void bugInstance2Flow(int idx, List<BugInstance> bugInstances, List<TaintFlow> flows, String error) {
                LOGGER.info(String.format("Parsing instance(%d/%d): %s", idx+1, bugInstances.size(), bugInstances.get(idx)));
            }

            @Override
            public void unzipJarInit(List<File> appJarsinReport) {
                LOGGER.info("Unzipping jar");
            }

            @Override
            public void unzipJar(int idx, List<File> appJarsinReport, String error) {
                LOGGER.info(String.format("Unzipping jar(%d/%d)", idx+1, appJarsinReport.size()));
            }

            @Override
            public void generateJoanaConfig() {
                LOGGER.info("Generating Joana Config");
            }

            @Override
            public void sliceInit(List<BugInstance> bugInstances) {
                LOGGER.info("Slicing...");
            }

            @Override
            public void slice(int idx, List<BugInstance> bugInstances, TaintFlow flow, String error) {
                LOGGER.info(String.format("Slicing (%d/%d): %s", idx+1, bugInstances.size(), bugInstances.get(idx)));
            }

            @Override
            public void predictionInit(List<BugInstance> bugInstances) {
                LOGGER.info("Getting Prediction...");
            }

            @Override
            public void prediction(int idx, List<BugInstance> bugInstances, TaintFlow flow, boolean isTP) {
                LOGGER.info(String.format("Getting Prediction (%d/%d): %b", idx+1, bugInstances.size(), isTP));
            }
        };

        SpotbugParser spotbugParser = new SpotbugParser();
        BugCollection bugCollection = spotbugParser.loadBugs(new File(xmlFile));
        SpotbugPredictor.predictFromBugCollection(bugCollection, callback);
    }
}
