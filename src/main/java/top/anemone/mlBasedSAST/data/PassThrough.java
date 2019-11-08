package top.anemone.mlBasedSAST.data;

import lombok.Data;

@Data
public class PassThrough extends Func{
    private Integer calledStartLine;
    private Integer calledEndLine;
    private String fileName;
}
