package top.anemone.mlBasedSAST.slice;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity;
import edu.kit.joana.wala.core.SDGBuilder;
import lombok.Data;
import org.apache.bcel.classfile.ClassParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.loader.LaunchedURLClassLoader;
import top.anemone.mlBasedSAST.data.*;
import top.anemone.mlBasedSAST.exception.BCELParserException;
import top.anemone.mlBasedSAST.exception.NotFoundException;
import top.anemone.mlBasedSAST.parser.SpotbugParser;
import top.anemone.mlBasedSAST.utils.JarUtil;
import top.anemone.mlBasedSAST.utils.JsonUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Warning2Slice {
    private static final Logger LOG = LoggerFactory.getLogger(JoanaSlicer.class);
    public static void main(String[] args) throws NotFoundException, BCELParserException, IOException, ClassHierarchyException, CancelException, GraphIntegrity.UnsoundGraphException, ClassNotFoundException, InterruptedException {
        File report = new File("src/test/resources/spotbugs.xml");
        List<File> appJars = Collections.singletonList(new File("src/test/resources/java-sec-code-1.0.0.jar"));

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
    public static void dumpSliceOutput(SliceOutput sliceOutput){

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
        List<File> transformedAppJars=new LinkedList<>();
//        transformedAppJars.add(new File("C:/Users/x5651/AppData/Local/Temp/mlBasedSAST6930275908120100044/java-sec-code-1.0.0.jar/counter.jar"));
//        transformedAppJars.add(new File("src/test/resources/classpath.zip"));
        List<URL> libJars=new LinkedList<>();
        String exclusionsFile=null;
        for (File appJar : appJars) {
            TransformedJar jar = transformJar(appJar, tempDirectory, entryPackages);
            transformedAppJars.add(jar.getAppJarPath());
            for(File f: Objects.requireNonNull(jar.getLibPath().toFile().listFiles())){
                libJars.add(f.toURL());
            }
        }
        libJars.add(Warning2Slice.class.getClassLoader().getResource("contrib/servlet-api.jar"));
        SDGBuilder.SDGBuilderConfig config = JoanaSlicer.generateConfig(transformedAppJars, libJars, exclusionsFile);

        List<SliceOutput> outputs=new LinkedList<>();
        for (Trace trace : traces) {
            LOG.info("Slice: "+trace);
            String entryClass = "L" + trace.getSource().getClazz().replace('.', '/');
            String entryMethod = trace.getSource().getMethod();
            String entryRef = trace.getSource().getSig();
            PassThrough lastPassThrough = trace.getPassThroughs().get(trace.getPassThroughs().size() - 1);
            String[] tmp = lastPassThrough.getClazz().split("\\.");
            tmp[tmp.length - 1] = lastPassThrough.getFileName();
            String joanaFilename = String.join("/", tmp);
            JoanaLineSlicer.Line sink = new JoanaLineSlicer.Line(joanaFilename, lastPassThrough.getCalledStartLine());
            String pdgFile = null;
            String slice = JoanaSlicer.computeSlice(config, entryClass, entryMethod, entryRef, sink, pdgFile);
            SliceOutput output=new SliceOutput(trace, slice);
            JsonUtil.dumpToFile(output,outputPath+String.format("/slice-%d.json",Math.abs(trace.hashCode())));
            outputs.add(output);
        }

        return outputs;
    }
}
