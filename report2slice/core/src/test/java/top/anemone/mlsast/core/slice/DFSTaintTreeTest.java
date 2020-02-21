package top.anemone.mlsast.core.slice;

import edu.umd.cs.findbugs.BugInstance;
import org.junit.Test;
import top.anemone.mlsast.core.data.TaintProject;
import top.anemone.mlsast.core.data.taintTree.TaintFlow;
import top.anemone.mlsast.core.data.taintTree.TaintTreeNode;
import top.anemone.mlsast.core.exception.NotFoundException;
import top.anemone.mlsast.core.exception.ParserException;
import top.anemone.mlsast.core.parser.ReportParser;
import top.anemone.mlsast.core.parser.impl.SpotbugXMLReportParser;
import top.anemone.mlsast.core.parser.impl.SpotbugXMLReportParserTest;

import java.io.File;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class DFSTaintTreeTest {

    @Test
    public void getTaintFlows() throws NotFoundException, ParserException, URISyntaxException {
        List<File> fileList=new LinkedList<>();
        fileList.add(new File(DFSTaintTreeTest.class.getClassLoader().getResource("java-vuln-sample-1.0-SNAPSHOT.jar").toURI()));
        ReportParser<BugInstance> spotbugXMLParser = new SpotbugXMLReportParser(
                new File(SpotbugXMLReportParserTest.class.getClassLoader()
                        .getResource("java-vuln-sample.xml").toURI()),
                fileList);
        TaintProject<BugInstance> taintProject = spotbugXMLParser.report2taintProject(null);
        TaintTreeNode firstNode= taintProject.getTaintTreeMap().get(taintProject.getBugInstances().get(0)).get(0);
        TaintFlow taintFlows= new DFSTaintTree().getTaintFlows(firstNode);
        assertEquals(taintFlows.size(),2);
    }
}