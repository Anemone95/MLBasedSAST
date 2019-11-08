package top.anemone.mlBasedSAST.slice;

import org.junit.Test;
import top.anemone.mlBasedSAST.data.Trace;
import top.anemone.mlBasedSAST.data.TransformedJar;
import top.anemone.mlBasedSAST.exception.BCELParserException;
import top.anemone.mlBasedSAST.exception.NotFoundException;
import top.anemone.mlBasedSAST.parser.SpotbugParser;
import top.anemone.mlBasedSAST.utils.JarUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class Warning2SliceTest {

    @Test
    public void getEntryPackagesTest() throws IOException, BCELParserException, NotFoundException {
        File report = new File("src/test/resources/spotbugs.xml");
        List<File> appJars = Collections.singletonList(new File("src/test/resources/java-sec-code-1.0.0.jar"));
        SpotbugParser spotbugParser = new SpotbugParser();
        List<Trace> traces=spotbugParser.parse(report, appJars).getTraces();
        Set<String> entryPackages=Warning2Slice.getEntryPackages(traces);
        assertEquals(entryPackages.size(),1);
        assertTrue(entryPackages.contains("org"));
    }


    @Test
    public void transformJarTest() throws IOException, NotFoundException, BCELParserException, ClassNotFoundException, InterruptedException {
        File appJar = new File("src/test/resources/java-sec-code-1.0.0.jar");
        Path tempDirectory = Files.createTempDirectory("mlBasedSAST");
        File report = new File("src/test/resources/spotbugs.xml");
        List<File> appJars = Collections.singletonList(new File("src/test/resources/java-sec-code-1.0.0.jar"));
        SpotbugParser spotbugParser = new SpotbugParser();
        List<Trace> traces=spotbugParser.parse(report, appJars).getTraces();
        Set<String> entryPackages=Warning2Slice.getEntryPackages(traces);
        try {
            Warning2Slice.transformJar(appJar, tempDirectory, entryPackages);
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