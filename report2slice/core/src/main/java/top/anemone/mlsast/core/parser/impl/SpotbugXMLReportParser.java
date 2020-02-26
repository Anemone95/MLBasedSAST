package top.anemone.mlsast.core.parser.impl;

import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.filter.Filter;
import edu.umd.cs.findbugs.filter.LastVersionMatcher;
import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.anemone.mlsast.core.Monitor;
import top.anemone.mlsast.core.data.*;
import top.anemone.mlsast.core.data.taintTree.TaintTreeNode;
import top.anemone.mlsast.core.exception.BCELParserException;
import top.anemone.mlsast.core.exception.NotFoundException;
import top.anemone.mlsast.core.exception.ParserException;
import top.anemone.mlsast.core.exception.SourceNotFoundExcetion;
import top.anemone.mlsast.core.parser.ReportParser;
import top.anemone.mlsast.core.utils.BCELParser;
import top.anemone.mlsast.core.utils.JarUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


public class SpotbugXMLReportParser implements ReportParser<BugInstance> {
    private static File findsecbugsPluginFile = new File(JarUtil.getPath() + "/contrib/findsecbugs-plugin.jar");
    private static final Logger LOGGER = LoggerFactory.getLogger(SpotbugXMLReportParser.class);
    // TODO 目前只考虑cmdi，URLRedirect，SSRF，XSS和SQLi (实验需要，增加LDAPi和XPATHi)
    public static List<String> caredVulns = Arrays.asList(
            "COMMAND_INJECTION",
            "XSS_JSP_PRINT",
            "XSS_REQUEST_WRAPPER",
            "XSS_SERVLET",
            "URLCONNECTION_SSRF_FD",
            "SQL_INJECTION_HIBERNATE",
            "SQL_INJECTION_JDO",
            "SQL_INJECTION_JPA",
            "SQL_INJECTION_JDBC",
            "SQL_INJECTION_SPRING_JDBC",
            "SCALA_SQL_INJECTION_SLICK",
            "SCALA_SQL_INJECTION_ANORM",
            "SQL_INJECTION_TURBINE",
            "UNVALIDATED_REDIRECT",
            "LDAP_INJECTION",
            "XPATH_INJECTION",
            "HTTP_PARAMETER_POLLUTION"
//            "PATH_TRAVERSAL_IN",
//            "PATH_TRAVERSAL_OUT"
    );

    private File xmlReport;
    private List<File> appJars;

    public SpotbugXMLReportParser(File report, List<File> appJars) {
        this.xmlReport = report;
        this.appJars = appJars;
    }


    /**
     * @param bugCollection 过滤使用污点传播模型，并且未被修复，并且有source点的问题，
     * @return
     */
    public static List<BugInstance> secBugFilter(BugCollection bugCollection) {
        Collection<BugInstance> c = bugCollection.getCollection();

        List<BugInstance> bugInstances = c.stream()
                .filter(e -> caredVulns.contains(e.getType()) && (!e.isDead()) && e.getPriority() != Priorities.LOW_PRIORITY ) //过滤问题类型，优先级超低问题和未被修复的问题
                .filter(e -> {
                    // 过滤掉没有source点的问题
                    for (BugAnnotation annotation : e.getAnnotations()) {
                        if (annotation.toString().equals("Method usage not detected")) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
        return bugInstances;
    }

    public SortedBugCollection loadBugs(File source) throws PluginException, IOException, DocumentException {
        Project project = new Project();
        // 加载插件
        if (!findsecbugsPluginFile.exists()) {
            throw new FileNotFoundException(findsecbugsPluginFile + " not found");
        }
        Plugin.loadCustomPlugin(findsecbugsPluginFile, project);
        SortedBugCollection col = new SortedBugCollection(project);
        col.readXML(source);
        if (col.hasDeadBugs()) {
            addDeadBugMatcher(col);
        }
        return col;
    }

    static void addDeadBugMatcher(BugCollection bugCollection) {
        if (bugCollection == null || !bugCollection.hasDeadBugs()) {
            return;
        }

        Filter suppressionMatcher = bugCollection.getProject().getSuppressionFilter();
        suppressionMatcher.softAdd(LastVersionMatcher.DEAD_BUG_MATCHER);
    }

    @Override
    public TaintProject<BugInstance> report2taintProject(Monitor monitor) throws ParserException, NotFoundException {
        SortedBugCollection sortedBugCollection;
        try {
            sortedBugCollection = this.loadBugs(this.xmlReport);
        } catch (IOException  e) {
            throw new NotFoundException(this.xmlReport, System.getProperty("user.dir"));
        } catch (DocumentException e){
            e.printStackTrace();
            throw new ParserException(e.toString(), e);
        } catch (PluginException e) {
            e.printStackTrace();
            throw new ParserException(e.toString(), e);
        }
//        List<File> libJarsinReport = sortedBugCollection.getProject().getAuxClasspathEntryList().stream().map(File::new).filter(File::exists).collect(Collectors.toList());
        return getBugInstanceTaintProject(monitor, appJars, sortedBugCollection);
    }

    public static TaintProject<BugInstance> getBugInstanceTaintProject(Monitor monitor, List<File> appJars, BugCollection bugCollection) throws NotFoundException {
        List<BugInstance> bugInstances = secBugFilter(bugCollection);
        List<String> analysisTargets = bugCollection.getProject().getFileList();
        List<File> appJarsinReport = analysisTargets.stream().map(File::new).filter(File::exists).collect(Collectors.toList());
        // 获取报告中的jar包地址，但是如果分析过程与报告产生过程不在一起，地址会很找不到，这时只能从appJars中获取
        if (appJars == null) {
            appJars = appJarsinReport;
        }
        if (monitor != null) monitor.init("Parse to taint project", bugInstances.size());
        Map<BugInstance, List<TaintTreeNode>> traces = new HashMap<>();
        for (int i = 0; i < bugInstances.size(); i++) {
            BugInstance bugInstance = bugInstances.get(i);
            Exception err = null;
            try {
                SpotbugsBugInstanceParser parser = new SpotbugsBugInstanceParser(bugInstance);
                traces.put(bugInstance, parser.parse());
            } finally {
                if (monitor != null) monitor.process(i, bugInstances.size(), bugInstance, traces.get(i), err);
            }
        }
        return new TaintProject<>(bugCollection.getProject().getProjectName(), appJars, bugInstances, traces);
    }


    @Deprecated
    public static List<TaintFlow> bugInstance2Flow(BugInstance bugInstance, List<File> appJars) throws NotFoundException, BCELParserException, IOException {
        LinkedList<TaintFlow> traces = new LinkedList<>();
        List<? extends BugAnnotation> annotations = bugInstance.getAnnotations();
        PassThrough caller = new PassThrough();

        // annotation[1] represents caller method
        caller.setClazz(((MethodAnnotation) annotations.get(1)).getClassName());
        caller.setMethod(((MethodAnnotation) annotations.get(1)).getMethodName());
        caller.setSig(((MethodAnnotation) annotations.get(1)).getMethodSignature());
        caller.setFileName(((MethodAnnotation) annotations.get(1)).getSourceFileName());

        // annotation[2] represents calledLine
        caller.setCalledStartLine(((SourceLineAnnotation) annotations.get(2)).getStartLine());
        caller.setCalledEndLine(((SourceLineAnnotation) annotations.get(2)).getStartLine());

        // annotation[3] represents sink method
        String annotation3 = ((StringAnnotation) annotations.get(3)).getValue();
        String clazz = annotation3.substring(0, annotation3.lastIndexOf('.'));
        String method = annotation3.substring(annotation3.lastIndexOf('.') + 1, annotation3.lastIndexOf('('));
        String sig = annotation3.substring(annotation3.lastIndexOf('('));

        Sink sink = new Sink();
        sink.setClazz(clazz);
        sink.setMethod(method);
        sink.setSig(sig);

        // TODO report2taintProject multi source
        Source source = new Source();
        SourceLineAnnotation sourceLineAnnotation = null;
        for (int i = 4; i < annotations.size(); i++) {
            if (annotations.get(i).getDescription().equals("SOURCE_LINE_DEFAULT")) {
                sourceLineAnnotation = (SourceLineAnnotation) annotations.get(i);
                break;
            }
        }
        if (sourceLineAnnotation == null) {
            throw new SourceNotFoundExcetion(bugInstance);
        }
        source.setClazz(sourceLineAnnotation.getClassName());
        source.setFileName(sourceLineAnnotation.getSourceFile());
        source.setCalledStartLine(sourceLineAnnotation.getStartLine());
        source.setCalledEndLine(sourceLineAnnotation.getStartLine());
        Func sourceFunc = BCELParser.findMethodByClassAndLineNumber(appJars, source.getClazz(), source.getCalledStartLine());
        source.setMethod(sourceFunc.getMethod());
        source.setSig(sourceFunc.getSig());

        TaintFlow trace = new TaintFlow();
        trace.setSource(source);
        LinkedList<PassThrough> passThroughs = new LinkedList<>();
        passThroughs.add(caller);
        trace.setPassThroughs(passThroughs);
        trace.setSink(sink);

        traces.add(trace);
        return traces;
    }
}
