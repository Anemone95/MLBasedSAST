package top.anemone.mlsast.core.slice;

import edu.umd.cs.findbugs.BugInstance;
import org.junit.Test;
import top.anemone.mlsast.core.Monitor;
import top.anemone.mlsast.core.exception.NotFoundException;
import top.anemone.mlsast.core.exception.ParserException;
import top.anemone.mlsast.core.exception.SliceRunnerException;
import top.anemone.mlsast.core.parser.ReportParser;
import top.anemone.mlsast.core.parser.impl.SpotbugXMLReportParser;
import top.anemone.mlsast.core.slice.impl.JoanaSlicer;
import top.anemone.mlsast.core.slice.impl.JoanaSlicerTest;

import java.io.File;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class SliceRunnerTest {

    @Test
    public void run() throws URISyntaxException, ParserException, NotFoundException, SliceRunnerException {
        List<File> fileList=new LinkedList<>();
        fileList.add(new File(SliceRunnerTest.class.getClassLoader().getResource("java-vuln-sample-1.0-SNAPSHOT.jar").toURI()));
        ReportParser<BugInstance> spotbugXMLParser = new SpotbugXMLReportParser(
                new File(SliceRunnerTest.class.getClassLoader().getResource("java-vuln-sample.xml").toURI()),
                fileList);
        SliceProject<BugInstance> sliceProject = new SliceRunner<BugInstance>()
                .setReportParser(spotbugXMLParser)
                .setSlicer(new JoanaSlicer())
                .run(new Monitor() {
                    @Override
                    public void init(String stageName, int totalWork) {
                        ;
                    }

                    @Override
                    public void process(int idx, int totalWork, Object input, Object output, Exception exception) {
                        ;
                    }
                });
        assertEquals(sliceProject.source2taintFlow.size(),2);
    }
}