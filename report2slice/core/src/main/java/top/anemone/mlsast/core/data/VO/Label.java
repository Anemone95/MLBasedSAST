package top.anemone.mlsast.core.data.VO;

import lombok.Data;
import lombok.NonNull;
import top.anemone.mlsast.core.data.taintTree.TaintEdge;

@Data
public class Label {
    @NonNull
    private String project;
    @NonNull
    private String flowHash;
    private TaintEdge taintEdge;
    @NonNull
    private boolean isSafe;
}
