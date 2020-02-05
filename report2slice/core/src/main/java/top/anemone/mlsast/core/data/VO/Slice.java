package top.anemone.mlsast.core.data.VO;

import edu.umd.cs.findbugs.StringAnnotation;
import lombok.Data;
import lombok.NonNull;
import top.anemone.mlsast.core.data.taintTree.TaintEdge;

@Data
public class Slice {

    private TaintEdge flow;
    @NonNull
    private String slice;
    @NonNull
    private String flowHash;
    @NonNull
    private String project;

    public Slice(TaintEdge flow, String slice, String project){
        this.flow=flow;
        this.slice=slice;
        this.flowHash=flow.sha1();
        this.project=project;
    }
}
