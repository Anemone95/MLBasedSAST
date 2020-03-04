package top.anemone.mlsast.core.exception;

@Deprecated
public class SourceNotFoundExcetion extends NotFoundException {
    public SourceNotFoundExcetion(Object source) {
        super(source, "code");
    }
}
