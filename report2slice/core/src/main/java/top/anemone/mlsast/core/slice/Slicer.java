package top.anemone.mlsast.core.slice;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import top.anemone.mlsast.core.data.Func;
import top.anemone.mlsast.core.data.TaintFlow;
import top.anemone.mlsast.core.data.taintTree.Location;
import top.anemone.mlsast.core.exception.SlicerException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public interface Slicer {
    void config(List<File> appJars, List<URL> libJars, String exclusionsFile) throws ClassHierarchyException, IOException;
    String computeSlice(Func func, Location line) throws SlicerException;
}
