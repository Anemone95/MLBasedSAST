package top.anemone.mlsast.slice;

import edu.umd.cs.findbugs.PluginException;
import org.junit.Test;
import top.anemone.mlsast.slice.data.TaintFlow;
import top.anemone.mlsast.slice.exception.BCELParserException;
import top.anemone.mlsast.slice.exception.NotFoundException;
import top.anemone.mlsast.slice.slice.Report2Slice;
import top.anemone.mlsast.slice.spotbugs.SpotbugParser;
import top.anemone.mlsast.slice.utils.JarUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class Report2SliceTest {

    @Test
    public void getEntryPackagesTest() throws IOException, BCELParserException, NotFoundException, PluginException {
        File report = new File("src/test/resources/spotbugs.xml");
        List<File> appJars = Collections.singletonList(new File("src/test/resources/java-sec-code-1.0.0.jar"));
        SpotbugParser spotbugParser = new SpotbugParser();
        List<TaintFlow> traces=spotbugParser.parse(report, appJars).getTraces();
        Set<String> entryPackages= Report2Slice.getEntryPackages(traces);
        assertEquals(entryPackages.size(),1);
        assertTrue(entryPackages.contains("org"));
    }


    @Test
    public void transformJarTest() throws IOException, NotFoundException, BCELParserException, ClassNotFoundException, InterruptedException, PluginException {
        File appJar = new File("src/test/resources/java-sec-code-1.0.0.jar");
        Path tempDirectory = Files.createTempDirectory("mlBasedSAST");
        File report = new File("src/test/resources/spotbugs.xml");
        List<File> appJars = Collections.singletonList(new File("src/test/resources/java-sec-code-1.0.0.jar"));
        SpotbugParser spotbugParser = new SpotbugParser();
        List<TaintFlow> traces=spotbugParser.parse(report, appJars).getTraces();
        Set<String> entryPackages= Report2Slice.getEntryPackages(traces);
        try {
            Report2Slice.transformJar(appJar, tempDirectory, entryPackages);
        } catch (IOException e){
            throw e;
        } finally {
            JarUtil.deleteDirectory(tempDirectory);
        }
    }


    @Test
    public void toSliceTest() {
    }
}