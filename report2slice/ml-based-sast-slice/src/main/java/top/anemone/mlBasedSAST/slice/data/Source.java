package top.anemone.mlBasedSAST.slice.data;


import lombok.Data;

@Data
public class Source extends Func {
    private Integer calledStartLine;
    private Integer calledEndLine;
    private String fileName;
}