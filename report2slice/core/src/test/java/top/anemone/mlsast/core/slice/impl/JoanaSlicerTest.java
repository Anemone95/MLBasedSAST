package top.anemone.mlsast.core.slice.impl;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.umd.cs.findbugs.BugInstance;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.anemone.mlsast.core.data.TaintProject;
import top.anemone.mlsast.core.data.taintTree.MethodLocation;
import top.anemone.mlsast.core.data.taintTree.TaintTreeNode;
import top.anemone.mlsast.core.exception.NotFoundException;
import top.anemone.mlsast.core.exception.ParserException;
import top.anemone.mlsast.core.exception.SlicerException;
import top.anemone.mlsast.core.parser.ReportParser;
import top.anemone.mlsast.core.parser.impl.SpotbugXMLReportParser;
import top.anemone.mlsast.core.parser.impl.SpotbugXMLReportParserTest;
import top.anemone.mlsast.core.slice.Slicer;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import static org.junit.Assert.*;

public class JoanaSlicerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JoanaSlicer.class);

    @Ignore
    @Test
    public void computeSliceTest1() throws GraphIntegrity.UnsoundGraphException, CancelException, ClassHierarchyException, IOException, NotFoundException {
        List<File> jarFiles= Arrays.asList(new File("src/test/resources/joana-target-1.0-SNAPSHOT.jar"));
        List<URL> libJars = new LinkedList<>();
        String exclusionsFile=null;
        String entryClass="Ltop/anemone/joana/target/Main";
        String entryMethod="main";
        String entryRef="([Ljava/lang/String;)V";
        JoanaLineSlicer.Line sink = new JoanaLineSlicer.Line("top/anemone/joana/target/Main.java", 17);
        String pdgFile=null;
        JoanaSlicer slicer=new JoanaSlicer();
        slicer.config(jarFiles, libJars, exclusionsFile);

        SDG sdg=slicer.computeSlice(entryClass, entryMethod, entryRef,  pdgFile);
//        System.out.println(result);
    }
    @Test
    public void computeSliceTest2() throws ClassHierarchyException, IOException, NotFoundException, URISyntaxException, ParserException, SlicerException {
        List<File> fileList=new LinkedList<>();
        fileList.add(new File(JoanaSlicerTest.class.getClassLoader().getResource("java-vuln-sample-1.0-SNAPSHOT.jar").toURI()));
        ReportParser<BugInstance> spotbugXMLParser = new SpotbugXMLReportParser(
                new File(SpotbugXMLReportParserTest.class.getClassLoader().getResource("java-vuln-sample.xml").toURI()),
                fileList);
        TaintProject<BugInstance> taintProject = spotbugXMLParser.report2taintProject(null);
        TaintTreeNode firstNode= taintProject.getTaintTreeMap().get(taintProject.getBugInstances().get(0)).get(0);
        Slicer slicer=new JoanaSlicer();
        slicer.config(fileList, null, null);
        String slice=slicer.computeSlice(((MethodLocation)firstNode.location).func, firstNode.firstChildNode.location);
        assertTrue(slice.contains("getParameter"));
        assertTrue(slice.contains("doGet"));
        slice=slicer.computeSlice(((MethodLocation)firstNode.location).func, firstNode.firstChildNode.nextSibling.location);
        System.out.println(slice);
        assertTrue(slice.contains("getParameter"));
        assertTrue(slice.contains("doGet"));
    }
}