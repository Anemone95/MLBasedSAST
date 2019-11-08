package top.anemone.mlBasedSAST.data;

import lombok.Data;

import java.util.List;

@Data
public class Trace {
    private Source source;
    private Sink sink;
    private List<PassThrough> passThroughs;
//    public int hashCode(){
//        int h = source.hashCode();
//        h=31*h+sink.hashCode();
//        for (PassThrough passThrough: passThroughs){
//            h=31*h+passThrough.hashCode();
//        }
//        return h;
//    }
}
