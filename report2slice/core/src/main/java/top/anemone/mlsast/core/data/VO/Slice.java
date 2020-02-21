package top.anemone.mlsast.core.data.VO;

import edu.umd.cs.findbugs.StringAnnotation;
import lombok.Data;
import lombok.NonNull;
import top.anemone.mlsast.core.data.taintTree.TaintEdge;
import top.anemone.mlsast.core.utils.SHA1;

@Data
public class Slice {

    private TaintEdge flow;
    @NonNull
    private String slice;
    @NonNull
    private String flowHash;
    @NonNull
    private String project;

    public Slice(TaintEdge flow, String slice, String project) {
        this.flow = flow;
        this.slice = slice;
        if (slice != null) {
            this.flowHash = SHA1.shaEncode(slice);
        } else if (flow != null) {
            this.flowHash = SHA1.shaEncode(flow.toString());
        } else {
            this.flowHash = "nonce";
        }
        this.project = project;
    }
}
