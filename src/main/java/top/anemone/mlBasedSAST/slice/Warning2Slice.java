package top.anemone.mlBasedSAST.slice;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity;
import org.apache.bcel.classfile.ClassParser;
import org.springframework.boot.loader.LaunchedURLClassLoader;
import top.anemone.mlBasedSAST.data.PassThrough;
import top.anemone.mlBasedSAST.data.Trace;
import top.anemone.mlBasedSAST.exception.BCELParserException;
import top.anemone.mlBasedSAST.exception.NotFoundException;
import top.anemone.mlBasedSAST.parser.SpotbugParser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Warning2Slice {
    public static void main(String[] args) throws NotFoundException, BCELParserException, IOException, ClassHierarchyException, CancelException, GraphIntegrity.UnsoundGraphException, ClassNotFoundException {
        File report = new File("src/test/resources/spotbugs.xml");
        List<File> appJars = Collections.singletonList(new File("src/test/resources/java-sec-code-1.0.0.zip"));

        List<URL> appJarURLs=appJars.stream().map(JoanaSlicer::apply).collect(Collectors.toList());

        ClassLoader classLoader=new URLClassLoader(appJarURLs.toArray(new URL[0]), ClassLoader.getSystemClassLoader());
//        classLoader.loadClass("org.joychou.controller.CommandInject");
        toSlice(appJars, report);
    }

    public static String toSlice(List<File> appJars, File report) throws IOException, BCELParserException, NotFoundException, ClassHierarchyException, CancelException, GraphIntegrity.UnsoundGraphException {

        SpotbugParser spotbugParser = new SpotbugParser();
        List<Trace> traces=spotbugParser.parse(report, appJars);
        System.out.println(traces);

        for (Trace trace:traces){
            List<URL> libJars = new LinkedList<>();
            libJars.add(Warning2Slice.class.getClassLoader().getResource("contrib/servlet-api.jar"));
            String exclusionsFile=null;
            String entryClass="L"+trace.getSource().getClazz().replace('.','/');
            String entryMethod=trace.getSource().getMethod();
            String entryRef=trace.getSource().getSig();
            PassThrough lastPassThrough=trace.getPassThroughs().get(trace.getPassThroughs().size()-1);
            String[] tmp=lastPassThrough.getClazz().split("\\.");
            tmp[tmp.length-1]=lastPassThrough.getFileName();
            String joanaFilename=String.join("/",tmp);
            JoanaLineSlicer.Line sink = new JoanaLineSlicer.Line(joanaFilename, lastPassThrough.getCalledStartLine());
            String pdgFile=null;
            String result=JoanaSlicer.computeSlice(appJars, libJars, exclusionsFile, entryClass, entryMethod, entryRef, sink, pdgFile);
            System.out.println(result);
        }

        return "";
    }
}
