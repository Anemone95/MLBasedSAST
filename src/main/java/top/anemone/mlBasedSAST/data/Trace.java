package top.anemone.mlBasedSAST.data;

import lombok.Data;

import java.util.List;

@Data
public class Trace {
    private Source source;
    private Sink sink;
    private List<PassThrough> passThroughs;
}
