package top.anemone.mlsast.core.slice.impl;


import com.ibm.wala.cfg.exc.intra.MethodState;
import com.ibm.wala.classLoader.*;
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
import edu.kit.joana.wala.core.prune.NodeLimitPruner;
import lombok.Data;
import top.anemone.mlsast.core.classloader.AppClassloader;
import top.anemone.mlsast.core.classloader.AppWalaClassLoaderFactory;
import top.anemone.mlsast.core.data.Func;
import top.anemone.mlsast.core.data.taintTree.Location;
import top.anemone.mlsast.core.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.anemone.mlsast.core.exception.RootNodeNotFoundException;
import top.anemone.mlsast.core.exception.SlicerException;
import top.anemone.mlsast.core.joana.*;
import top.anemone.mlsast.core.slice.Slicer;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static top.anemone.mlsast.core.joana.LoggingOutputStream.LogLevel.INFO;

@Data
public class JoanaSlicer implements Slicer {
    private static final Logger LOGGER = LoggerFactory.getLogger(JoanaSlicer.class);
    private SDGBuilder.SDGBuilderConfig config;
    private Map<Func, SDG> sdgCache;

    public void config(List<File> appJars, List<URL> libJars, String exclusionsFile) throws ClassHierarchyException, IOException {
        SDGBuilder.SDGBuilderConfig config = getSDGBuilderConfig(appJars, libJars, exclusionsFile);
        config.doParallel = false;
        this.config = config;
        this.sdgCache=new HashMap<>();
    }

    public String computeSlice(Func func, Location line) throws SlicerException {
        String entryClass = "L" + func.getClazz().replace('.', '/');
        String entryMethod = func.getMethod();
        String entryRef = func.getSig();
        SDG sdg=null;
        if (sdgCache.containsKey(func)){
            sdg=sdgCache.get(func);
        } else{
            try {
                sdg = this.computeSlice(entryClass, entryMethod, entryRef, null);
                LOGGER.info("Computing Slice...");
                // 利用SDG切片
            } catch (IOException|GraphIntegrity.UnsoundGraphException|CancelException|NotFoundException|ClassCastException e) {
                throw new SlicerException(e.getMessage(), e);
            }
            sdgCache.put(func,sdg);
        }

        JoanaLineSlicer jSlicer = new JoanaLineSlicer(sdg);
        LOGGER.info("Done...");
        // 根据sink点查找sinknodes
        HashSet<SDGNode> sinkNodes = null;
        try {
            sinkNodes = jSlicer.getNodesAtLine(line);
        } catch (NotFoundException e) {
            throw new SlicerException(e.getMessage(), e);
        }
        Collection<SDGNode> slice = jSlicer.slice(sinkNodes);
        // 这里relatedNodesStr没搞懂是啥
//            String relatedNodesStr = "[";
//            for (SDGNode sdgNode : sinkNodes) {
//                if (!jSlicer.toAbstract.contains(sdgNode) && !JoanaLineSlicer.isRemoveNode(sdgNode)) {
//                    relatedNodesStr += sdgNode.getId() + ", ";
//
//            }
//            relatedNodesStr = relatedNodesStr.substring(0, relatedNodesStr.lastIndexOf(",")) + "]\n";
        String result = Formater.prepareSliceForEncoding(sdg, slice, jSlicer.toAbstract);
        return result;
    }

    public SDG computeSlice(String entryClass, String entryMethod, String entryRef, String pdgFile)
            throws IOException, GraphIntegrity.UnsoundGraphException, CancelException, NotFoundException {
        SDG localSdg = null;
        LOGGER.info("Building SDG... ");
        // 根据class, method, ref在classloader中找入口函数
        config.entry = findMethod(this.config, entryClass, entryMethod, entryRef);
        // 构造SDG
        try {
            localSdg = SDGBuilder.build(this.config, new JoanaMonitor());
        } catch (NoSuchElementException e) {
            StackTraceElement stackTraceElement = e.getStackTrace()[2];
            if (stackTraceElement.getClassName().equals("edu.kit.joana.wala.core.CallGraph")
                    && stackTraceElement.getMethodName().equals("<init>")) {
                throw new RootNodeNotFoundException(entryClass+"."+entryMethod+entryRef,"call-graph (or it was in primordial jar)");
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
        return localSdg;
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
        if (libJars!=null){
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


        scfg.out = new PrintStream(new LoggingOutputStream(LOGGER, INFO));
        scfg.nativeSpecClassLoader = new AppClassloader(new File[]{});
        scfg.scope = makeMinimalScope(appJars, libJars, exclusionsFile, scfg.nativeSpecClassLoader);
        scfg.cache = new AnalysisCacheImpl();
        scfg.cha = ClassHierarchyFactory.makeWithRoot(scfg.scope, new AppWalaClassLoaderFactory(scfg.scope.getExclusions()));
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
        scfg.entrypointFactory=new AppEntrypointFactory();
        scfg.cgPruner=new NodeLimitPruner(300);
        return scfg;
    }

    static IMethod findMethod(SDGBuilder.SDGBuilderConfig scfg, final String entryClazz, final String entryMethod, String methodRef) throws NotFoundException {
        // debugPrint(scfg.cha);
        final IClass cl = scfg.cha.lookupClass(TypeReference.findOrCreate(ClassLoaderReference.Application, entryClazz));
        if (cl == null) {
            throw new NotFoundException(entryClazz, scfg.cha);
        }
        // final IMethod m = cl.getMethod(Selector.make(entryMethod));
        final IMethod m = cl.getMethod(new Selector(Atom.findOrCreateUnicodeAtom(entryMethod),
                Descriptor.findOrCreateUTF8(Language.JAVA, methodRef)));
        if (m == null) {
            throw new NotFoundException( cl + "." + entryMethod, cl);
        }
        return m;
    }

}
