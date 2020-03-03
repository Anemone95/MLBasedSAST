package top.anemone.mlsast.console;

import edu.umd.cs.findbugs.BugInstance;
import net.sourceforge.argparse4j.inf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sourceforge.argparse4j.ArgumentParsers;
import top.anemone.mlsast.core.Monitor;
import top.anemone.mlsast.core.data.VO.Slice;
import top.anemone.mlsast.core.data.taintTree.TaintFlow;
import top.anemone.mlsast.core.exception.*;
import top.anemone.mlsast.core.parser.impl.SpotbugXMLReportParser;
import top.anemone.mlsast.core.parser.impl.SpotbugXMLReportParserForTest;
import top.anemone.mlsast.core.predict.PredictProject;
import top.anemone.mlsast.core.predict.PredictRunner;
import top.anemone.mlsast.core.predict.exception.PredictorException;
import top.anemone.mlsast.core.predict.impl.BLSTMRemotePredictor;
import top.anemone.mlsast.core.slice.SliceProject;
import top.anemone.mlsast.core.slice.SliceRunner;
import top.anemone.mlsast.core.slice.impl.JoanaSlicer;
import top.anemone.mlsast.core.utils.ExceptionUtil;
import top.anemone.mlsast.core.utils.JsonUtil;

import java.io.File;
import java.io.IOException;


public class AiConsole {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiConsole.class);

    public static void main(String[] args) throws ParserException, NotFoundException, SliceRunnerException, PredictorRunnerException, IOException, PredictorException {
        ArgumentParser parser = ArgumentParsers.newFor("AiConsole").build()
                .defaultHelp(true);
        Subparsers subparsers = parser.addSubparsers().title("command").dest("command");
        Subparser sliceParser = subparsers.addParser("slice").defaultHelp(true)
                .help("Slice and dump into dir");
        sliceParser.addArgument("-f", "--report-file").required(true)
                .help("Specify SpotBugs analysis report(.xml)");
        sliceParser.addArgument("-o", "--output-dir")
                .help("Specify slice output dir");

        Subparser predictParser = subparsers.addParser("predictIsSafe").defaultHelp(true)
                .help("Slice and then predictIsSafe whether the bug is true positive");
        predictParser.addArgument("-f", "--report-file").required(true)
                .help("Specify SpotBugs analysis results(.xml)");
        predictParser.addArgument("-s", "--server").setDefault("http://127.0.0.1:8000")
                .help("Specify prediction remote server");
        predictParser.addArgument("-o", "--output").setDefault("predictIsSafe.json")
                .help("Specify prediction output file");

        Subparser experimentParser = subparsers.addParser("experiment").defaultHelp(true)
                .help("Slice and then predictIsSafe whether the bug is true positive");
        experimentParser.addArgument("-f", "--report-file").required(true)
                .help("Specify SpotBugs analysis results(.xml)");
        experimentParser.addArgument("-t", "--test-file")
                .help("Specify test file to predict");
        experimentParser.addArgument("-s", "--server").setDefault("http://127.0.0.1:8000")
                .help("Specify prediction remote server");
        experimentParser.addArgument("-o", "--output").setDefault("predictIsSafe.json")
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
            public void process(int idx, int totalWork, Object input, Object output, Exception exception) {
                if(exception!=null){
                    if (exception instanceof SourceNotFoundExcetion){
                        LOGGER.warn(exception.getMessage());
                    } else{
                        LOGGER.error(ExceptionUtil.getStackTrace(exception));
                    }
                }
                if(stage.equals("Prediction")){
                    LOGGER.info(String.format("Predict result: %s.", output));
                } else {
                    LOGGER.info(String.format("Stage: %s, Progress: %d/%d.", stage, idx, totalWork));
                }
            }
        };
        if (ns.getString("command").equals("slice")) {
            SliceProject<BugInstance> sliceProject = new SliceRunner<BugInstance>()
                    .setReportParser(new SpotbugXMLReportParser(new File(ns.getString("report_file")), null))
                    .setSlicer(new JoanaSlicer())
                    .run(monitor);
            File outputDir;
            if (ns.getString("output_dir")!=null){
                outputDir=new File(ns.getString("output_dir")+File.separator+sliceProject.getProjectName());
            } else {
                outputDir=new File("slice"+File.separator+sliceProject.getProjectName());
            }
            if (!outputDir.exists()){
                outputDir.mkdirs();
            }
            Slice slice;
            for (TaintFlow edge: sliceProject.getTaintEdge2slice().keySet()) {
                slice = new Slice(edge, sliceProject.getTaintEdge2slice().get(edge),
                        sliceProject.getTaintProject().getProjectName());

                File subOutputDir=new File(outputDir, sliceProject.getSliceHash(edge).substring(0,2));
                if (!subOutputDir.exists()){
                    subOutputDir.mkdirs();
                }
                JsonUtil.dumpToFile(slice, subOutputDir+"/slice-"+sliceProject.getSliceHash(edge)+".json");
            }
        } else if (ns.getString("command").equals("predictIsSafe")) {
            BLSTMRemotePredictor remotePredictor=new BLSTMRemotePredictor(ns.getString("server"));
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
        } else if (ns.getString("command").equals("experiment")){
            BLSTMRemotePredictor remotePredictor=new BLSTMRemotePredictor(ns.getString("server"));
            if(!remotePredictor.isAlive()){
                LOGGER.error("Remote predictor not alive");
                System.exit(1);
            }
            PredictProject<BugInstance> predictProject = new PredictRunner<BugInstance>()
                    .setReportParser(new SpotbugXMLReportParserForTest(new File(ns.getString("report_file")), null, new File(ns.getString("test_file"))))
                    .setSlicer(new JoanaSlicer())
                    .setPredictor(remotePredictor)
                    .run(monitor);
            JsonUtil.dumpToFile(predictProject.getPredictions(), ns.getString("output"));
        }
    }

}
