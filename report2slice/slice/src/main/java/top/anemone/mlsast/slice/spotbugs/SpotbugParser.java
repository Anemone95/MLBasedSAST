package top.anemone.mlsast.slice.spotbugs;

import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.filter.Filter;
import edu.umd.cs.findbugs.filter.LastVersionMatcher;
import lombok.Data;
import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.anemone.mlsast.slice.data.*;
import top.anemone.mlsast.slice.exception.BCELParserException;
import top.anemone.mlsast.slice.exception.NotFoundException;
import top.anemone.mlsast.slice.exception.ParserException;
import top.anemone.mlsast.slice.parser.Parser;
import top.anemone.mlsast.slice.utils.BCELParser;
import top.anemone.mlsast.slice.utils.JarUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


@Data
public class SpotbugParser implements Parser {
    private static String findsecbugsPluginPath = JarUtil.getPath()+ "/contrib/findsecbugs-plugin-1.10.1.jar";
    private static final Logger LOGGER = LoggerFactory.getLogger(SpotbugParser.class);
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
            "XPATH_INJECTION"
    );

    public static void main(String[] args) throws NotFoundException, ParserException {
//        System.out.println(JarUtil.getPath());
        SpotbugParser spotbugParser = new SpotbugParser();
        TaintProject taintProject=spotbugParser.report2taintProject(new File("bugreports/spotbugs.xml"),null);
        System.out.println(taintProject);
    }

    /**
     * @param bugCollection 过滤使用污点传播模型，并且未被修复，并且有source点的问题，
     * @return
     */
    public static List<BugInstance> secBugFilter(BugCollection bugCollection) {
        Collection<BugInstance> c = bugCollection.getCollection();

        List<BugInstance> bugInstances = c.stream()
                .filter(e -> caredVulns.contains(e.getType()) && !e.isDead()) //过滤问题类型，和未被修复的问题
                .filter(e -> {
                    // 过滤掉没有source点的问题
                    for (BugAnnotation annotation:e.getAnnotations()){
                        if(annotation.toString().equals("Method usage not detected")){
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
        Plugin.loadCustomPlugin(new File(findsecbugsPluginPath), project);
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
    public TaintProject report2taintProject(File xml, List<File> appJars) throws ParserException, NotFoundException {
        SortedBugCollection sortedBugCollection;
        try {
            sortedBugCollection = this.loadBugs(xml);
        } catch (IOException|DocumentException e) {
            throw new NotFoundException(xml + " not found");
        } catch (PluginException e) {
            throw new ParserException(e.toString(), e);
        }
        // 获取报告中的jar包地址，但是如果分析过程与报告产生过程不在一起，地址会很找不到，这时只能从appJars中获取
        List<String> analysisTargets;
        analysisTargets=sortedBugCollection.getProject().getFileList();
        List<File> appJarsinReport=analysisTargets.stream().map(File::new).filter(File::exists).collect(Collectors.toList());
        appJarsinReport.addAll(appJars);
        List<BugInstance> bugInstances = secBugFilter(sortedBugCollection);
        List<TaintFlow> traces = new LinkedList<>();
        for (BugInstance bugInstance : bugInstances) {
            try {
                traces.addAll(bugInstance2Flow(bugInstance, appJarsinReport));
            } catch (BCELParserException | IOException e) {
                throw new ParserException(e.toString(), e);
            }
        }
        TaintProject taintProject=new TaintProject(sortedBugCollection.getProject().getProjectName(), traces);
        return taintProject;
    }


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
            throw new NotFoundException(bugInstance.toString()+" can't find source");
        }
        source.setClazz(sourceLineAnnotation.getClassName());
        source.setFileName(sourceLineAnnotation.getSourceFile());
        source.setCalledStartLine(sourceLineAnnotation.getStartLine());
        source.setCalledEndLine(sourceLineAnnotation.getStartLine());
        Func sourceFunc= BCELParser.findMethodByClassAndLineNumber(appJars, source.getClazz(), source.getCalledStartLine());
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
