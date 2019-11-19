package top.anemone.mlBasedSAST.slice;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity;
import edu.kit.joana.wala.core.SDGBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.anemone.mlBasedSAST.data.*;
import top.anemone.mlBasedSAST.data.VO.SliceOutput;
import top.anemone.mlBasedSAST.exception.BCELParserException;
import top.anemone.mlBasedSAST.exception.NotFoundException;
import top.anemone.mlBasedSAST.spotbugs.SpotbugParser;
import top.anemone.mlBasedSAST.utils.JarUtil;
import top.anemone.mlBasedSAST.utils.JsonUtil;

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

    public static Set<String> getEntryPackages(List<Trace> traces) {
        Set<String> entryPackages = new HashSet<>();
        for (Trace trace : traces) {
            String entryPackage = trace.getSource().getClazz().substring(0, trace.getSource().getClazz().indexOf('.'));
            entryPackages.add(entryPackage);
        }
        return entryPackages;
    }
    // TODO 拆分函数，转移到按钮上
    public static List<SliceOutput> toSlice(List<File> appJars, String project, List<Trace> traces, String knowledgeBase) throws IOException, InterruptedException, ClassHierarchyException {
        Set<String> entryPackages = getEntryPackages(traces);
        Path tempDirectory = Files.createTempDirectory("mlBasedSAST");
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
        SDGBuilder.SDGBuilderConfig config = JoanaSlicer.generateConfig(transformedAppJars, libJars, exclusionsFile);

        List<SliceOutput> outputs=new LinkedList<>();
        for (Trace trace : traces) {
            LOGGER.info("Slice: "+trace);
            String entryClass = "L" + trace.getSource().getClazz().replace('.', '/');
            String entryMethod = trace.getSource().getMethod();
            String entryRef = trace.getSource().getSig();
            PassThrough lastPassThrough = trace.getPassThroughs().get(trace.getPassThroughs().size() - 1);
            String[] tmp = lastPassThrough.getClazz().split("\\.");
            tmp[tmp.length - 1] = lastPassThrough.getFileName();
            String joanaFilename = String.join("/", tmp);
            JoanaLineSlicer.Line sink = new JoanaLineSlicer.Line(joanaFilename, lastPassThrough.getCalledStartLine());
            String pdgFile = null;
            String slice=null;
            try {
                slice = JoanaSlicer.computeSlice(config, entryClass, entryMethod, entryRef, sink, pdgFile);
            } catch (NotFoundException e){
                System.err.println("Err when processing this trace: "+ trace);
                e.printStackTrace();
                continue;
            } catch (CancelException | GraphIntegrity.UnsoundGraphException e) {
                e.printStackTrace();
            }
            SliceOutput output=new SliceOutput(trace, slice, Integer.toString(slice.hashCode()), project);
            outputs.add(output);
        }
        LOGGER.warn("Please delete temp dir: "+tempDirectory);
        return outputs;
    }

    public static List<SliceOutput> toSlice(List<File> appJars, File report, String knowledgeBase) throws IOException, BCELParserException, NotFoundException, ClassHierarchyException, CancelException, GraphIntegrity.UnsoundGraphException, InterruptedException {

        SpotbugParser spotbugParser = new SpotbugParser();
        TaintProject project = spotbugParser.parse(report, appJars);

        // make kb/slice/{project} dir
        Paths.get(knowledgeBase).toFile().mkdirs();
        Paths.get(knowledgeBase, "slice").toFile().mkdirs();
        Path outputPath=Paths.get(knowledgeBase, "slice", project.getProjectName());
        outputPath.toFile().mkdirs();

        List<Trace> traces=project.getTraces();
        Set<String> entryPackages = getEntryPackages(traces);
        Path tempDirectory = Files.createTempDirectory("mlBasedSAST");
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
        SDGBuilder.SDGBuilderConfig config = JoanaSlicer.generateConfig(transformedAppJars, libJars, exclusionsFile);

        List<SliceOutput> outputs=new LinkedList<>();
        for (Trace trace : traces) {
            LOGGER.info("Slice: "+trace);
            String entryClass = "L" + trace.getSource().getClazz().replace('.', '/');
            String entryMethod = trace.getSource().getMethod();
            String entryRef = trace.getSource().getSig();
            PassThrough lastPassThrough = trace.getPassThroughs().get(trace.getPassThroughs().size() - 1);
            String[] tmp = lastPassThrough.getClazz().split("\\.");
            tmp[tmp.length - 1] = lastPassThrough.getFileName();
            String joanaFilename = String.join("/", tmp);
            JoanaLineSlicer.Line sink = new JoanaLineSlicer.Line(joanaFilename, lastPassThrough.getCalledStartLine());
            String pdgFile = null;
            String slice;
            try {
                slice = JoanaSlicer.computeSlice(config, entryClass, entryMethod, entryRef, sink, pdgFile);
            } catch (NotFoundException e){
                System.err.println("Err when processing this trace: "+ trace);
                e.printStackTrace();
                continue;
            }
            SliceOutput output=new SliceOutput(trace, slice, Integer.toString(slice.hashCode()), project.getProjectName());
            JsonUtil.dumpToFile(output,outputPath+String.format("/slice-%d.json",Math.abs(trace.hashCode())));
            outputs.add(output);
        }

        LOGGER.warn("Please delete temp dir: "+tempDirectory);
        return outputs;
    }
}
