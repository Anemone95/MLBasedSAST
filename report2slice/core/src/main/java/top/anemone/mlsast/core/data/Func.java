package top.anemone.mlsast.core.data;

import lombok.Data;

@Data
public class Func {
    private String clazz;
    private String method;
    private String sig;
    public Func(){}
    public Func(String clazz, String method, String sig){
        this.clazz=clazz;
        this.method=method;
        this.sig=sig;
    }
}
