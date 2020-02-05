package top.anemone.mlsast.core.data;


import lombok.Data;
import lombok.NonNull;
import top.anemone.mlsast.core.data.taintTree.TaintTreeNode;

import java.io.File;
import java.util.List;
import java.util.Map;

@Data
public class TaintProject<T> {
    @NonNull
    protected String projectName;
    @NonNull
    protected List<File> appJars;
    protected List<File> libJars;
    @NonNull
    protected List<T> bugInstances;
    @NonNull
    protected Map<T,List<TaintTreeNode>> taintTreeMap;

    public List<TaintTreeNode> getTaintTrees(T bug){
        return taintTreeMap.get(bug);
    }
}
