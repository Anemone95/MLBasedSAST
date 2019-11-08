package top.anemone.mlBasedSAST.slice;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity;
import org.junit.Test;

import javax.print.DocFlavor;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.*;
import static top.anemone.mlBasedSAST.slice.JoanaSlicer.computeSlice;

public class JoanaSlicerTest {

    @Test
    public void computeSliceTest() throws GraphIntegrity.UnsoundGraphException, CancelException, ClassHierarchyException, IOException {
        List<File> jarFiles= Arrays.asList(new File("joana-target/target/joana-target-1.0-SNAPSHOT.jar"));
        List<URL> libJars = new LinkedList<>();
        String exclusionsFile=null;
        String entryClass="Ltop/anemone/joana/target/Main";
        String entryMethod="main";
        String entryRef="([Ljava/lang/String;)V";
        JoanaLineSlicer.Line sink = new JoanaLineSlicer.Line("top/anemone/joana/target/Main.java", 17);
        String pdgFile=null;

        String result=computeSlice(jarFiles, libJars, exclusionsFile, entryClass, entryMethod, entryRef, sink, pdgFile);
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
}