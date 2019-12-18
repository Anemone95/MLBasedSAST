package top.anemone.mlsast.console;

import edu.umd.cs.findbugs.BugInstance;
import net.sourceforge.argparse4j.inf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sourceforge.argparse4j.ArgumentParsers;
import top.anemone.mlsast.core.Monitor;
import top.anemone.mlsast.core.data.TaintFlow;
import top.anemone.mlsast.core.data.VO.Slice;
import top.anemone.mlsast.core.exception.NotFoundException;
import top.anemone.mlsast.core.exception.ParserException;
import top.anemone.mlsast.core.exception.PredictorRunnerException;
import top.anemone.mlsast.core.exception.SliceRunnerException;
import top.anemone.mlsast.core.parser.impl.SpotbugXMLReportParser;
import top.anemone.mlsast.core.predict.PredictProject;
import top.anemone.mlsast.core.predict.PredictRunner;
import top.anemone.mlsast.core.predict.impl.LSTMRemotePredictor;
import top.anemone.mlsast.core.slice.SliceProject;
import top.anemone.mlsast.core.slice.SliceRunner;
import top.anemone.mlsast.core.slice.impl.JoanaSlicer;
import top.anemone.mlsast.core.utils.JsonUtil;

import java.io.File;
import java.io.IOException;


public class AiConsole {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiConsole.class);

    public static void main(String[] args) throws ParserException, NotFoundException, SliceRunnerException, PredictorRunnerException, IOException {
        ArgumentParser parser = ArgumentParsers.newFor("AiConsole").build()
                .defaultHelp(true);
        Subparsers subparsers = parser.addSubparsers().title("command").dest("command");
        Subparser sliceParser = subparsers.addParser("slice").defaultHelp(true)
                .help("Slice and dump into dir");
        sliceParser.addArgument("-f", "--report-file").required(true)
                .help("Specify SpotBugs analysis report(.xml)");
        sliceParser.addArgument("-o", "--output-dir").setDefault("slice")
                .help("Specify slice output dir");

        Subparser predictParser = subparsers.addParser("predict").defaultHelp(true)
                .help("Slice and then predict whether the bug is true positive");
        predictParser.addArgument("-f", "--report-file").required(true)
                .help("Specify SpotBugs analysis results(.xml)");
        predictParser.addArgument("-s", "--server").setDefault("http://127.0.0.1:8888")
                .help("Specify prediction remote server");
        predictParser.addArgument("-o", "--output").setDefault("predict.json")
                .help("Specify prediction output file");

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        Monitor monitor = new Monitor() {
            private String stage;

            @Override
            public void init(String stageName, int totalWork) {
                stage = stageName;
                LOGGER.info(stageName + ", Total work: " + totalWork);
            }

            @Override
            public void process(int idx, int totalWork, Object input, Object output, String error) {
                if(stage.equals("Prediction")){
                    LOGGER.info(String.format("Predict result: %s.", output));
                } else {
                    LOGGER.info(String.format("Stage: %s, Progress: %d/%d.", stage, idx, totalWork));
                }
                if(error!=null && error.length()!=0){
                    LOGGER.error(error);
                }

            }
        };
        if (ns.getString("command").equals("slice")) {
            SliceProject<BugInstance> sliceProject = new SliceRunner<BugInstance>()
                    .setReportParser(new SpotbugXMLReportParser(new File(ns.getString("report_file")), null))
                    .setSlicer(new JoanaSlicer())
                    .run(monitor);
            File outputDir=new File(ns.getString("output_dir")+File.separator+sliceProject.getProjectName());
            if (!outputDir.exists()){
                outputDir.mkdirs();
            }
            for (int i = 0; i < sliceProject.getBugInstances().size(); i++) {
                TaintFlow flow = sliceProject.getTaintFlow(sliceProject.getBugInstances().get(i)).get(0);
                String sliceStr = sliceProject.getBugInstance2slice().get(sliceProject.getBugInstances().get(i));
                Slice slice = new Slice(flow, sliceStr, flow.getHash(), sliceProject.getTaintProject().getProjectName());
                JsonUtil.dumpToFile(slice, outputDir+"/slice-"+flow.getHash());
            }
        } else if (ns.getString("command").equals("predict")) {
            LSTMRemotePredictor remotePredictor=new LSTMRemotePredictor(ns.getString("server"));
            if(!remotePredictor.isAlive()){
                LOGGER.error("Remote predictor not alive");
                System.exit(1);
            }
            PredictProject<BugInstance> predictProject = new PredictRunner<BugInstance>()
                    .setReportParser(new SpotbugXMLReportParser(new File(ns.getString("report_file")), null))
                    .setSlicer(new JoanaSlicer())
                    .setPredictor(remotePredictor)
                    .run(monitor);
            JsonUtil.dumpToFile(predictProject.getPredictions(), ns.getString("output"));
        }
    }

}
