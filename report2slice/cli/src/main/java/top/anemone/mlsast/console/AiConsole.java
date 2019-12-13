package top.anemone.mlsast.console;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.PluginException;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentAction;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sourceforge.argparse4j.ArgumentParsers;
import top.anemone.mlsast.slice.data.AIBasedSpotbugProject;
import top.anemone.mlsast.slice.data.TaintFlow;
import top.anemone.mlsast.slice.exception.BCELParserException;
import top.anemone.mlsast.slice.exception.NotFoundException;
import top.anemone.mlsast.slice.remote.LSTMServer;
import top.anemone.mlsast.slice.spotbugs.PredictionMonitor;
import top.anemone.mlsast.slice.spotbugs.SpotbugParser;
import top.anemone.mlsast.slice.spotbugs.SpotbugPredictor;
import top.anemone.mlsast.slice.utils.JsonUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AiConsole {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiConsole.class);

    public static void main(String[] args) throws PluginException, IOException, DocumentException, NotFoundException, BCELParserException {
        ArgumentParser parser = ArgumentParsers.newFor("AiConsole").build()
                .defaultHelp(true);
        parser.addArgument("-f", "--results-file").required(true)
                .help("Specify SpotBugs analysis results(.xml)");
        parser.addArgument("-s", "--server").setDefault("http://127.0.0.1:8888")
                .help("Specify prediction server");
        parser.addArgument("-o", "--output")
                .help("Specify prediction output file");
        parser.addArgument( "-d","--dump-slice").action(Arguments.storeTrue())
                .help("dump slice for each bug instance");
        Namespace ns=null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }
        AIBasedSpotbugProject.getInstance().setServer(new LSTMServer(ns.getString("server")));
        getPredictionFromXML(ns.getString("results_file"), ns.getBoolean("dump_slice"));
        if (ns.getString("output")==null){
            JsonUtil.toJson(AIBasedSpotbugProject.getInstance().getBugInstancePredictionMap());
        } else {
            JsonUtil.dumpToFile(AIBasedSpotbugProject.getInstance().getBugInstancePredictionMap(), ns.getString("output"));
        }

    }
    public static void getPredictionFromXML(String xmlFile, boolean dumpSlice) throws IOException, PluginException, DocumentException, NotFoundException, BCELParserException {

        PredictionMonitor callback=new PredictionMonitor() {
            @Override
            public void bugInstance2FlowInit(List<BugInstance> bugInstances) {
                LOGGER.info("Parsing bug instances to taint flow");
            }

            @Override
            public void bugInstance2Flow(int idx, List<BugInstance> bugInstances, List<TaintFlow> flows, String error) {
                LOGGER.info(String.format("Parsing instance(%d/%d): %s", idx+1, bugInstances.size(), bugInstances.get(idx)));
                if (error!=null && error.length()!=0){
                    LOGGER.error("Got exception: "+error);
                }
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
            public void slice(int idx, List<BugInstance> bugInstances, TaintFlow flow, String slice, String error) {
                LOGGER.info(String.format("Slicing (%d/%d): %s", idx+1, bugInstances.size(), bugInstances.get(idx)));
                if (error!=null && error.length()!=0){
                    LOGGER.error("Got exception: "+error);
                }
                if (dumpSlice){
                    File sliceDir=new File("slice_dir");
                    if (!sliceDir.exists()){
                        sliceDir.mkdirs();
                    }
                    Map<String, String> map=new HashMap<>();
                    map.put("flow",flow.toString());
                    map.put("slice",slice);
                    try {
                        JsonUtil.dumpToFile(map,sliceDir+"/"+bugInstances.get(idx).toString().replace(": ","_")+"_"+idx+".json");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }

            @Override
            public void predictionInit(List<BugInstance> bugInstances) {
                LOGGER.info("Getting Prediction...");
            }

            @Override
            public void prediction(int idx, List<BugInstance> bugInstances, TaintFlow flow, String isTP) {
                LOGGER.info(String.format("Getting Prediction (%d/%d): %s", idx+1, bugInstances.size(), isTP));
            }
        };

        SpotbugParser spotbugParser = new SpotbugParser();
        BugCollection bugCollection = spotbugParser.loadBugs(new File(xmlFile));
        SpotbugPredictor.predictFromBugCollection(bugCollection, callback);
    }
}
