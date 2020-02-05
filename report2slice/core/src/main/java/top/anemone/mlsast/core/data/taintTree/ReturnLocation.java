package top.anemone.mlsast.core.data.taintTree;

public class ReturnLocation extends Location {
    public ReturnLocation(String sourceFile, Integer startLine, Integer endLine) {
        super(sourceFile, startLine, endLine);
    }
}
