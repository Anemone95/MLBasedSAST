package top.anemone.mlsast.slice.slice;


import com.ibm.wala.cfg.exc.intra.MethodState;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.JarStreamModule;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.pruned.ApplicationLoaderPolicy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.Atom;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.mhpoptimization.CSDGPreprocessor;
import edu.kit.joana.wala.core.ExternalCallCheck;
import edu.kit.joana.wala.core.Main;
import edu.kit.joana.wala.core.SDGBuilder;
import lombok.Data;
import top.anemone.mlsast.slice.data.PassThrough;
import top.anemone.mlsast.slice.data.TaintFlow;
import top.anemone.mlsast.slice.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.anemone.mlsast.slice.exception.RootNodeNotFoundException;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Data
public class JoanaSlicer {
    private static final Logger LOGGER = LoggerFactory.getLogger(JoanaSlicer.class);
    private SDGBuilder.SDGBuilderConfig config;

    public SDGBuilder.SDGBuilderConfig generateConfig(List<File> appJars, List<URL> libJars, String exclusionsFile) throws ClassHierarchyException, IOException {
        SDGBuilder.SDGBuilderConfig config = getSDGBuilderConfig(appJars, libJars, exclusionsFile);
        config.doParallel = false;
        this.config = config;
        return config;
    }

    public String computeSlice(TaintFlow trace) throws GraphIntegrity.UnsoundGraphException, CancelException, ClassHierarchyException, NotFoundException, IOException {
        PassThrough lastPassThrough = trace.getPassThroughs().get(trace.getPassThroughs().size() - 1);
        String entryClass = "L" + lastPassThrough.getClazz().replace('.', '/');
        String entryMethod = lastPassThrough.getMethod();
        String entryRef = lastPassThrough.getSig();
        String[] tmp = lastPassThrough.getClazz().split("\\.");
        tmp[tmp.length - 1] = lastPassThrough.getFileName();
        String joanaFilename = String.join("/", tmp);
        JoanaLineSlicer.Line sink = new JoanaLineSlicer.Line(joanaFilename, lastPassThrough.getCalledStartLine());
        String slice = this.computeSlice(entryClass, entryMethod, entryRef, sink, null);
        return slice;
    }

    public String computeSlice(String entryClass, String entryMethod, String entryRef,
                               JoanaLineSlicer.Line sink, String pdgFile)
            throws IOException, GraphIntegrity.UnsoundGraphException, CancelException, NotFoundException {
        SDG localSdg = null;
        LOGGER.info("Building SDG... ");
        // 根据class, method, ref在classloader中找入口函数
        config.entry = findMethod(this.config, entryClass, entryMethod, entryRef);
        // 构造SDG
        try {
            localSdg = SDGBuilder.build(this.config);
        } catch (NoSuchElementException e) {
            StackTraceElement stackTraceElement = e.getStackTrace()[2];
            if (stackTraceElement.getClassName().equals("edu.kit.joana.wala.core.CallGraph")
                    && stackTraceElement.getMethodName().equals("<init>")) {
                throw new RootNodeNotFoundException("Entry class not found in call-graph (or it was in primordial jar)");
            }
        }
        LOGGER.info("SDG build done! Optimizing...");
        // 剪枝
        CSDGPreprocessor.preprocessSDG(localSdg);
        // 保存SDG
        if (pdgFile != null) {
            LOGGER.info("Done! Saving to: " + pdgFile);
            SDGSerializer.toPDGFormat(localSdg, new PrintWriter(pdgFile));
        } else {
            LOGGER.info("Done!");
        }

        LOGGER.info("Computing Slice...");
        // 利用SDG切片
        JoanaLineSlicer jSlicer = new JoanaLineSlicer(localSdg);
        LOGGER.info("Done...");
        // 根据sink点查找sinknodes
        HashSet<SDGNode> sinkNodes = jSlicer.getNodesAtLine(sink);
        Collection<SDGNode> slice = jSlicer.slice(sinkNodes);
        // 这里relatedNodesStr没搞懂是啥
//        String relatedNodesStr = "[";
//        for (SDGNode sdgNode : sinkNodes) {
//            if (!jSlicer.toAbstract.contains(sdgNode) && !JoanaLineSlicer.isRemoveNode(sdgNode)) {
//                relatedNodesStr += sdgNode.getId() + ", ";
//            }
//        }
//        relatedNodesStr = relatedNodesStr.substring(0, relatedNodesStr.lastIndexOf(",")) + "]\n";
        String result = Formater.prepareSliceForEncoding(localSdg, slice, jSlicer.toAbstract);
        return result;
    }

    private static AnalysisScope makeMinimalScope(List<File> appJars, List<URL> libJars,
                                                  String exclusionsFile, ClassLoader classLoader) throws IOException {

        String scopeFile = "RtScopeFile.txt";
        String exclusionFile = "Java60RegressionExclusions.txt";
        final AnalysisScope scope = AnalysisScopeReader.readJavaScope(
                scopeFile, (new FileProvider()).getFile(exclusionFile), classLoader);
        for (File appJar : appJars) {
            scope.addToScope(ClassLoaderReference.Application, new JarStreamModule(new FileInputStream(appJar)));
        }
        for (URL lib : libJars) {
            if (appJars.contains(new File(lib.getFile()))) {
                LOGGER.warn(lib + "in app scope.");
                continue;
            }
            if (lib.getProtocol().equals("file")) {
                scope.addToScope(ClassLoaderReference.Primordial, new JarStreamModule(new FileInputStream(lib.getFile())));
            } else {
                scope.addToScope(ClassLoaderReference.Primordial, new JarStreamModule(JoanaSlicer.class.getResourceAsStream(String.valueOf(lib))));
            }
        }

        if (exclusionsFile != null) {
            List<String> exclusions = Files.readAllLines(Paths.get(exclusionsFile));
            for (String exc : exclusions) {
                scope.getExclusions().add(exc);
            }
        }
        return scope;
    }

    public static SDGBuilder.SDGBuilderConfig getSDGBuilderConfig(List<File> appJars,
                                                                  List<URL> libJars, String exclusionsFile)
            throws ClassHierarchyException, IOException {
        SDGBuilder.SDGBuilderConfig scfg = new SDGBuilder.SDGBuilderConfig();
        scfg.out = System.out;
        scfg.nativeSpecClassLoader = new URLClassLoader(new URL[]{});
        scfg.scope = makeMinimalScope(appJars, libJars, exclusionsFile, scfg.nativeSpecClassLoader);
        scfg.cache = new AnalysisCacheImpl();
        scfg.cha = ClassHierarchyFactory.make(scfg.scope);
        scfg.ext = ExternalCallCheck.EMPTY;
        scfg.immutableNoOut = Main.IMMUTABLE_NO_OUT;
        scfg.immutableStubs = Main.IMMUTABLE_STUBS;
        scfg.ignoreStaticFields = Main.IGNORE_STATIC_FIELDS;
        scfg.exceptions = SDGBuilder.ExceptionAnalysis.IGNORE_ALL;
        scfg.pruneDDEdgesToDanglingExceptionNodes = true;
        scfg.defaultExceptionMethodState = MethodState.DEFAULT;
        scfg.accessPath = false;
        scfg.sideEffects = null;
        scfg.prunecg = 0;
        scfg.pruningPolicy = ApplicationLoaderPolicy.INSTANCE;
        scfg.pts = SDGBuilder.PointsToPrecision.INSTANCE_BASED;
        scfg.customCGBFactory = null;
        scfg.staticInitializers = SDGBuilder.StaticInitializationTreatment.SIMPLE;
        scfg.fieldPropagation = SDGBuilder.FieldPropagation.OBJ_GRAPH;
        scfg.computeInterference = false;
        scfg.computeAllocationSites = false;
        scfg.computeSummary = false;
        scfg.cgConsumer = null;
        scfg.additionalContextSelector = null;
        scfg.dynDisp = SDGBuilder.DynamicDispatchHandling.IGNORE;
        scfg.debugCallGraphDotOutput = false;
        scfg.debugManyGraphsDotOutput = false;
        scfg.debugAccessPath = false;
        scfg.debugStaticInitializers = false;
        return scfg;
    }

    static IMethod findMethod(SDGBuilder.SDGBuilderConfig scfg, final String entryClazz, final String entryMethod, String methodRef) throws NotFoundException {
        // debugPrint(scfg.cha);
        final IClass cl = scfg.cha.lookupClass(TypeReference.findOrCreate(ClassLoaderReference.Application, entryClazz));
        if (cl == null) {
            throw new NotFoundException("Class not found: " + entryClazz);
        }
        // final IMethod m = cl.getMethod(Selector.make(entryMethod));
        final IMethod m = cl.getMethod(new Selector(Atom.findOrCreateUnicodeAtom(entryMethod),
                Descriptor.findOrCreateUTF8(Language.JAVA, methodRef)));
        if (m == null) {
            throw new NotFoundException("Func not found:" + cl + "." + entryMethod);
        }
        return m;
    }

}
