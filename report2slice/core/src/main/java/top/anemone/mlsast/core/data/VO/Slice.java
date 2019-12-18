package top.anemone.mlsast.core.data.VO;

import lombok.Data;
import lombok.NonNull;
import top.anemone.mlsast.core.data.TaintFlow;

@Data
public class Slice {
    @NonNull
    private TaintFlow flow;
    @NonNull
    private String slice;
    @NonNull
    private String flowHash;
    @NonNull
    private String project;
}
