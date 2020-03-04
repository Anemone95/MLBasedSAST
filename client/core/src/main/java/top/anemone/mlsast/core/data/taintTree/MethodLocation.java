package top.anemone.mlsast.core.data.taintTree;

import top.anemone.mlsast.core.data.Func;

import java.util.Objects;

public class MethodLocation extends Location {
    public Func func;

    public MethodLocation(String clazz, String method, String sig,
                          String sourceFile,
                          Integer startLine,
                          Integer endLine) {
        super(sourceFile, startLine, endLine);
        func = new Func(clazz, method, sig);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodLocation)) return false;
        if (!super.equals(o)) return false;
        MethodLocation that = (MethodLocation) o;
        return Objects.equals(func, that.func);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), func);
    }
}
