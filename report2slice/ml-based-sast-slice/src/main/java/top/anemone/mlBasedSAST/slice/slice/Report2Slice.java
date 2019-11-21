package top.anemone.mlBasedSAST.slice.slice;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity;
import top.anemone.mlBasedSAST.slice.data.TaintFlow;
import top.anemone.mlBasedSAST.slice.data.VO.Slice;
import top.anemone.mlBasedSAST.slice.exception.BCELParserException;
import top.anemone.mlBasedSAST.slice.exception.NotFoundException;
import top.anemone.mlBasedSAST.slice.utils.JarUtil;
import top.anemone.mlBasedSAST.slice.utils.JsonUtil;
import top.anemone.mlBasedSAST.slice.data.TaintProject;
import top.anemone.mlBasedSAST.slice.data.TransformedJar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.anemone.mlBasedSAST.slice.spotbugs.SpotbugParser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@SuppressWarnings("Duplicates")
public class Report2Slice {
    private static final Logger LOGGER = LoggerFactory.getLogger(Report2Slice.class);
    public static void main(String[] args) throws NotFoundException, BCELParserException, IOException, ClassHierarchyException, CancelException, GraphIntegrity.UnsoundGraphException, ClassNotFoundException, InterruptedException {
        File report = new File("bugreports/benchmark1.1.xml");
        List<File> appJars = Collections.singletonList(new File("bugreports/benchmark1.1.war"));

        toSlice(appJars, report, "kb");
    }

    public static TransformedJar transformJar(File appJar, Path baseDir, Set<String> entryPacakges) throws IOException, InterruptedException {

        Path outputPath = Paths.get(baseDir.toString(), appJar.getName());
        Path classesPath = Paths.get(outputPath.toString(), "classes");
        classesPath.toFile().mkdirs();
        Path libPath = Paths.get(outputPath.toString(), "lib");
        libPath.toFile().mkdirs();
        JarUtil.unJar(appJar, classesPath, libPath, entryPacakges);
        File newJar = JarUtil.jar(classesPath);
        return new TransformedJar(classesPath, libPath, newJar);
    }

    public static Set<String> getEntryPackages(List<TaintFlow> flows) {
        Set<String> entryPackages = new HashSet<>();
        for (TaintFlow flow : flows) {
            String entryPackage = flow.getSource().getClazz().substring(0, flow.getSource().getClazz().indexOf('.'));
            entryPackages.add(entryPackage);
        }
        return entryPackages;
    }
    // TODO 拆分函数，转移到按钮上
    public static List<Slice> toSlice(List<File> appJars, File report, String knowledgeBase) throws IOException, BCELParserException, NotFoundException, ClassHierarchyException, CancelException, GraphIntegrity.UnsoundGraphException, InterruptedException {

        SpotbugParser spotbugParser = new SpotbugParser();
        TaintProject project = spotbugParser.parse(report, appJars);

        // make kb/slice/{project} dir
        Paths.get(knowledgeBase).toFile().mkdirs();
        Paths.get(knowledgeBase, "slice").toFile().mkdirs();
        Path outputPath=Paths.get(knowledgeBase, "slice", project.getProjectName());
        outputPath.toFile().mkdirs();

        List<TaintFlow> traces=project.getTraces();
        Set<String> entryPackages = getEntryPackages(traces);
        Path tempDirectory = Files.createTempDirectory("mlBasedSAST/slice");
        // prepare
        LOGGER.info("Extracting jar to "+tempDirectory);
        List<File> transformedAppJars=new LinkedList<>();
        List<URL> libJars=new LinkedList<>();
        String exclusionsFile=null;
        for (File appJar : appJars) {
            LOGGER.info("Transforming jar: "+appJar);
            TransformedJar jar = transformJar(appJar, tempDirectory, entryPackages);
            transformedAppJars.add(jar.getAppJarPath());
            for(File f: Objects.requireNonNull(jar.getLibPath().toFile().listFiles())){
                libJars.add(f.toURL());
            }
        }
        libJars.add(Report2Slice.class.getClassLoader().getResource("contrib/servlet-api.jar"));
        JoanaSlicer slicer=new JoanaSlicer();
        slicer.generateConfig(transformedAppJars, libJars, exclusionsFile);

        List<Slice> outputs=new LinkedList<>();
        for (TaintFlow trace : traces) {
            LOGGER.info("Slice: "+trace);
            String slice;
            try {
                slice = slicer.computeSlice(trace);
            } catch (NotFoundException e){
                System.err.println("Err when processing this taintFlow: "+ trace);
                e.printStackTrace();
                continue;
            }
            Slice output=new Slice(trace, slice, trace.getHash(), project.getProjectName());
            JsonUtil.dumpToFile(output,outputPath+String.format("/slice-%d.json",Math.abs(trace.hashCode())));
            outputs.add(output);
        }

        LOGGER.warn("Please delete temp dir: "+tempDirectory);
        return outputs;
    }
}
