package top.anemone.mlBasedSAST.utils;

import org.apache.bcel.classfile.*;
import top.anemone.mlBasedSAST.data.Func;
import top.anemone.mlBasedSAST.exception.BCELParserException;
import top.anemone.mlBasedSAST.exception.NotFoundException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

// TODO singleton
public class BCELParser {
    public static void main(String[] args) throws IOException, NotFoundException, BCELParserException {
        String clazzName="org.joychou.controller.CommandInject";
        String zip_file="bugreports/java-sec-code-1.0.0.jar";
        Func f=findMethodByClassAndLineNumber(Arrays.asList(new File(zip_file)), clazzName, 45);
        System.out.println(f);
    }
    public static Func findMethodByClassAndLineNumber(List<File> jars, String clazz, int lineNumber) throws IOException, BCELParserException, NotFoundException {
        String fileName=null;
        File successorJar=null;
        for (File jar: jars) {
            try {
                fileName=getClassFileInJar(jar, clazz);
                successorJar=jar;
                break;
            } catch (IOException | BCELParserException e) {
                throw e;
            } catch (NotFoundException e) {
                continue;
            }
        }
        if (fileName==null){
            throw new NotFoundException(String.format("Not found %s in %s", clazz, jars));
        }
        ClassParser classParser = new ClassParser(successorJar.getAbsolutePath(), fileName);
        JavaClass javaClass=classParser.parse();
        Method successorMethod=null;
        for(Method method: javaClass.getMethods()){
            LineNumberTable lineNumberTable=method.getLineNumberTable();
            int startLine=lineNumberTable.getLineNumberTable()[0].getLineNumber();
            int endLine=lineNumberTable.getLineNumberTable()[lineNumberTable.getTableLength()-1].getLineNumber();
            if (startLine<=lineNumber && lineNumber<=endLine){
                successorMethod=method;
                break;
            }
        }
        if (successorMethod==null){
            throw new NotFoundException(String.format("Not found line %s in %s", lineNumber, fileName ));
        }
        Func f=new Func(clazz,successorMethod.getName(), successorMethod.getSignature());
        return f;

    }

    public static String getClassFileInJar(File jar, String clazz) throws IOException, BCELParserException, NotFoundException {
        ZipFile zip = new ZipFile(jar);
        String filepath=clazz.replace('.','/')+".class";
        List<? extends ZipEntry> entries= zip.stream().filter(e-> e.getName().endsWith(filepath)).collect(Collectors.toList());
        if(entries.size()<1){
            throw new NotFoundException("Expect 1 class file but got "+entries.size()+".");
        }
        if(entries.size()!=1){
            throw new BCELParserException("Expect 1 class file but got "+entries.size()+".");
        }
        return entries.get(0).getName();
    }
}
