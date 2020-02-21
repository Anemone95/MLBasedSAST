package top.anemone.mlsast.core.predict;

import edu.umd.cs.findbugs.BugInstance;
import org.junit.Test;
import top.anemone.mlsast.core.Monitor;
import top.anemone.mlsast.core.exception.NotFoundException;
import top.anemone.mlsast.core.exception.ParserException;
import top.anemone.mlsast.core.exception.PredictorRunnerException;
import top.anemone.mlsast.core.exception.SliceRunnerException;
import top.anemone.mlsast.core.parser.ReportParser;
import top.anemone.mlsast.core.parser.impl.SpotbugXMLReportParser;
import top.anemone.mlsast.core.predict.exception.PredictorException;
import top.anemone.mlsast.core.predict.impl.BLSTMRemotePredictor;
import top.anemone.mlsast.core.slice.SliceProject;
import top.anemone.mlsast.core.slice.SliceRunner;
import top.anemone.mlsast.core.slice.SliceRunnerTest;
import top.anemone.mlsast.core.slice.impl.JoanaSlicer;

import java.io.File;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class PredictRunnerTest {

    @Test
    public void run() throws URISyntaxException, PredictorException, SliceRunnerException, ParserException, NotFoundException, PredictorRunnerException {
        List<File> fileList=new LinkedList<>();
        fileList.add(new File(PredictRunner.class.getClassLoader().getResource("java-vuln-sample-1.0-SNAPSHOT.jar").toURI()));
        ReportParser<BugInstance> spotbugXMLParser = new SpotbugXMLReportParser(
                new File(SliceRunnerTest.class.getClassLoader().getResource("java-vuln-sample.xml").toURI()),
                fileList);

        Predictor remotePredictor=new BLSTMRemotePredictor("http://127.0.0.1:8000");
        PredictProject<BugInstance> predictProject = new PredictRunner<BugInstance>()
                .setReportParser(spotbugXMLParser)
                .setSlicer(new JoanaSlicer())
                .setPredictor(remotePredictor)
                .run(new Monitor() {
                    @Override
                    public void init(String stageName, int totalWork) {
                        ;
                    }

                    @Override
                    public void process(int idx, int totalWork, Object input, Object output, Exception exception) {
                        assertNull(exception);
                    }
                });
        assertEquals(predictProject.edge2isSafe.size(),8);
        assertEquals(predictProject.bugInstance2isSafe.size(),1);
    }
}