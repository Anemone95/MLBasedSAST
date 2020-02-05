package top.anemone.mlsast.core.data.taintTree;

public class SinkLocation extends Location {
    public SinkLocation(String sourceFile, Integer startLine, Integer endLine) {
        super(sourceFile, startLine, endLine);
    }
}
