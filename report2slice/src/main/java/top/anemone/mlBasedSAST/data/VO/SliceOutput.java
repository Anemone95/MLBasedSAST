package top.anemone.mlBasedSAST.data.VO;

import lombok.Data;
import lombok.NonNull;
import top.anemone.mlBasedSAST.data.Trace;

@Data
public class SliceOutput {
    @NonNull
    private Trace trace;
    @NonNull
    private String slice;
    @NonNull
    private String sliceHash;
    @NonNull
    private String project;
}
