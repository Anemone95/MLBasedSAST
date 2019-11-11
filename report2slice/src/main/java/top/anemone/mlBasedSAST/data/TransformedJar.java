package top.anemone.mlBasedSAST.data;

import lombok.Data;

import java.io.File;
import java.nio.file.Path;

@Data
public class TransformedJar {
    private Path classPath;
    private Path libPath;
    private File appJarPath;
    public TransformedJar(Path classpath, Path libpath, File appJarPath){
        this.classPath=classpath;
        this.libPath=libpath;
        this.appJarPath=appJarPath;
    }
}
