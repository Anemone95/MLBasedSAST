package top.anemone.mlBasedSAST.data;


import lombok.Data;
import lombok.NonNull;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

@Data
public class TaintProject {
    @NonNull
    private String projectName;
    @NonNull
    private List<Trace> traces;
}
