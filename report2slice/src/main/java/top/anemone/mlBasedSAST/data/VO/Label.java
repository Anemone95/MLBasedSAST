package top.anemone.mlBasedSAST.data.VO;

import lombok.Data;
import lombok.NonNull;
import top.anemone.mlBasedSAST.data.Trace;

@Data
public class Label {
    @NonNull
    private String project;
    @NonNull
    private String traceHash;
    private Trace trace;
    @NonNull
    private boolean isReal;
}
