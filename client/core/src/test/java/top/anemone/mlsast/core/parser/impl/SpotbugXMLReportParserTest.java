package top.anemone.mlsast.core.parser.impl;

import edu.umd.cs.findbugs.BugInstance;
import org.junit.Test;
import top.anemone.mlsast.core.data.TaintProject;
import top.anemone.mlsast.core.exception.NotFoundException;
import top.anemone.mlsast.core.exception.ParserException;

import java.io.File;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class SpotbugXMLReportParserTest {

    @Test
    public void report2taintProject() throws NotFoundException, ParserException, URISyntaxException {
        List<File> fileList=new LinkedList<>();
        fileList.add(new File(SpotbugXMLReportParserTest.class.getClassLoader().getResource("java-vuln-sample-1.0-SNAPSHOT.jar").toURI()));
        SpotbugXMLReportParser spotbugXMLParser = new SpotbugXMLReportParser(
                new File(SpotbugXMLReportParserTest.class.getClassLoader().getResource("java-vuln-sample.xml").toURI()),
                fileList);
        TaintProject<BugInstance> taintProject = spotbugXMLParser.report2taintProject(null);
        List l=  taintProject.getTaintTreeMap().get(taintProject.getBugInstances().get(0));
        assertEquals(l.size(),2);
    }
}