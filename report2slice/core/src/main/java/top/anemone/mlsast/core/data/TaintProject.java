package top.anemone.mlsast.core.data;


import lombok.Data;
import lombok.NonNull;

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
    protected Map<T,List<TaintFlow>> taintFlowMap;
}
