package top.anemone.mlsast.slice;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.anemone.mlsast.slice.exception.NotFoundException;
import top.anemone.mlsast.slice.slice.JoanaLineSlicer;
import top.anemone.mlsast.slice.slice.JoanaSlicer;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

public class JoanaSlicerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JoanaSlicer.class);
    @Test
    public void computeSliceTest() throws GraphIntegrity.UnsoundGraphException, CancelException, ClassHierarchyException, IOException, NotFoundException {
        List<File> jarFiles= Arrays.asList(new File("src/test/resources/joana-target-1.0-SNAPSHOT.jar"));
        List<URL> libJars = new LinkedList<>();
        String exclusionsFile=null;
        String entryClass="Ltop/anemone/joana/target/Main";
        String entryMethod="main";
        String entryRef="([Ljava/lang/String;)V";
        JoanaLineSlicer.Line sink = new JoanaLineSlicer.Line("top/anemone/joana/target/Main.java", 17);
        String pdgFile=null;
        JoanaSlicer slicer=new JoanaSlicer();
        slicer.generateConfig(jarFiles, libJars, exclusionsFile);

        String result=slicer.computeSlice(entryClass, entryMethod, entryRef, sink, pdgFile);
        String expectedOutput="1 :: ENTR :: entry :: null :: top.anemone.joana.target.Main.main(java.lang.String[])::CD,8:CD,12" +
                "8 :: CALL :: call :: Ljava/lang/String :: v6 = replace(v4)::CL,203" +
                "12 :: CALL :: call :: V :: sink(v6)::CL,225" +
                "203 :: ENTR :: entry :: null :: top.anemone.joana.target.Main.replace(java.lang.String)::CE,204:CD,207:CD,213:CD,219" +
                "204 :: EXIT :: exit :: Ljava/lang/String :: top.anemone.joana.target.Main.replace(java.lang.String)::" +
                "207 :: CALL :: call :: Ljava/lang/String :: v6 = p1 $str .replace(#(a), #(b))::JM,219" +
                "213 :: CALL :: call :: Ljava/lang/String :: v10 = v6.replace(#(c), #(d))::JM,219" +
                "219 :: NORM :: compound :: Ljava/lang/String :: return v10::DD,204" +
                "225 :: ENTR :: entry :: null :: top.anemone.joana.target.Main.sink(java.lang.String)::CD,229:CD,231:CD,235:CD,239:CD,244:CD,249:CD,253:CD,255" +
                "229 :: EXPR :: reference :: Ljava/io/PrintStream :: v3 = java.lang.System.out::" +
                "231 :: CALL :: call :: V :: v3.println(p1 $cmd )::" +
                "235 :: NORM :: declaration :: Ljava/lang/StringBuilder :: v5 = new java.lang.StringBuilder::" +
                "239 :: CALL :: call :: Ljava/lang/StringBuilder :: v8 = v5.append(p1 $cmd )::" +
                "244 :: CALL :: call :: Ljava/lang/StringBuilder :: v11 = v8.append(#(1))::" +
                "249 :: CALL :: call :: Ljava/lang/String :: v13 = v11.toString()::" +
                "253 :: EXPR :: reference :: Ljava/io/PrintStream :: v14 = java.lang.System.out::" +
                "255 :: CALL :: call :: V :: v14.println(v13)::";
        assertEquals(result.replace("\n","").replace("\r", ""), expectedOutput);
    }


//    @Test
//    public void computeSliceTest2() throws GraphIntegrity.UnsoundGraphException, CancelException, ClassHierarchyException, IOException, NotFoundException, InterruptedException {
//        List<File> appJars= Arrays.asList(new File("src/test/resources/benchmark.war"));
//        List<URL> libJars = new LinkedList<>();
//        String exclusionsFile=null;
//        String entryClass="Lorg/owasp/benchmark/testcode/BenchmarkTest00113";
//        String entryMethod="doPost";
//        String entryRef="(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V";
//        JoanaLineSlicer.Line sink = new JoanaLineSlicer.Line("org/owasp/benchmark/testcode/BenchmarkTest00113.java", 75);
//        String pdgFile=null;
//
//        Path tempDirectory = Files.createTempDirectory("mlBasedSAST");
//
//        List<URL> morelibJars=new LinkedList<>();
//        List<File> transformedAppJars=new LinkedList<>();
//        Set<String> entres=new HashSet<>(Arrays.asList("org"));
//        for (File appJar : appJars) {
//            LOGGER.info("Transforming jar: "+appJar);
//            TransformedJar jar = transformJar(appJar, tempDirectory, entres);
//            transformedAppJars.add(jar.getAppJarPath());
//            for(File f: Objects.requireNonNull(jar.getLibPath().toFile().listFiles())){
//                libJars.add(f.toURL());
//            }
//        }
//        libJars.add(Report2Slice.class.getClassLoader().getResource("contrib/servlet-api.jar"));
//
//        JoanaSlicer slicer=new JoanaSlicer();
//        slicer.generateConfig(transformedAppJars, libJars, exclusionsFile);
//        String result=slicer.computeSlice( entryClass, entryMethod, entryRef, sink, pdgFile);
//        System.out.println(result);
////        String expectedOutput="1 :: ENTR :: entry :: null :: top.anemone.joana.target.Main.main(java.lang.String[])::CD,8:CD,12" +
////                "8 :: CALL :: call :: Ljava/lang/String :: v6 = replace(v4)::CL,203" +
////                "12 :: CALL :: call :: V :: sink(v6)::CL,225" +
////                "203 :: ENTR :: entry :: null :: top.anemone.joana.target.Main.replace(java.lang.String)::CE,204:CD,207:CD,213:CD,219" +
////                "204 :: EXIT :: exit :: Ljava/lang/String :: top.anemone.joana.target.Main.replace(java.lang.String)::" +
////                "207 :: CALL :: call :: Ljava/lang/String :: v6 = p1 $str .replace(#(a), #(b))::JM,219" +
////                "213 :: CALL :: call :: Ljava/lang/String :: v10 = v6.replace(#(c), #(d))::JM,219" +
////                "219 :: NORM :: compound :: Ljava/lang/String :: return v10::DD,204" +
////                "225 :: ENTR :: entry :: null :: top.anemone.joana.target.Main.sink(java.lang.String)::CD,229:CD,231:CD,235:CD,239:CD,244:CD,249:CD,253:CD,255" +
////                "229 :: EXPR :: reference :: Ljava/io/PrintStream :: v3 = java.lang.System.out::" +
////                "231 :: CALL :: call :: V :: v3.println(p1 $cmd )::" +
////                "235 :: NORM :: declaration :: Ljava/lang/StringBuilder :: v5 = new java.lang.StringBuilder::" +
////                "239 :: CALL :: call :: Ljava/lang/StringBuilder :: v8 = v5.append(p1 $cmd )::" +
////                "244 :: CALL :: call :: Ljava/lang/StringBuilder :: v11 = v8.append(#(1))::" +
////                "249 :: CALL :: call :: Ljava/lang/String :: v13 = v11.toString()::" +
////                "253 :: EXPR :: reference :: Ljava/io/PrintStream :: v14 = java.lang.System.out::" +
////                "255 :: CALL :: call :: V :: v14.println(v13)::";
////        assertEquals(result.replace("\n","").replace("\r", ""), expectedOutput);
//    }
}