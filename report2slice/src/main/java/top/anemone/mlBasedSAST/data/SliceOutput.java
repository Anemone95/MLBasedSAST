package top.anemone.mlBasedSAST.data;

import lombok.Data;
import lombok.NonNull;

@Data
public class SliceOutput {
    @NonNull
    private Trace trace;
    @NonNull
    private String slice;
}
