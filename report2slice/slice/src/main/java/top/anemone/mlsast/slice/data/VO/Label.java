package top.anemone.mlsast.slice.data.VO;

import lombok.Data;
import lombok.NonNull;
import top.anemone.mlsast.slice.data.TaintFlow;

@Data
public class Label {
    @NonNull
    private String project;
    @NonNull
    private String flowHash;
    private TaintFlow taintFlow;
    @NonNull
    private boolean isReal;
}
