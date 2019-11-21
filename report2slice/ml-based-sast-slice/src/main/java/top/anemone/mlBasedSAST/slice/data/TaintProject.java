package top.anemone.mlBasedSAST.slice.data;


import lombok.Data;
import lombok.NonNull;

import java.util.List;

@Data
public class TaintProject {
    @NonNull
    private String projectName;
    @NonNull
    private List<TaintFlow> traces;
}
