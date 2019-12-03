package top.anemone.mlsast.slice.data;

import lombok.Data;

import java.util.List;

@Data
public class TaintFlow {
    private Source source;
    private Sink sink;
    private List<PassThrough> passThroughs;
    public String getHash(){
        return Integer.toString(Math.abs(hashCode()));
    }
//    public int hashCode(){
//        int h = source.hashCode();
//        h=31*h+sink.hashCode();
//        for (PassThrough passThrough: passThroughs){
//            h=31*h+passThrough.hashCode();
//        }
//        return h;
//    }
}
