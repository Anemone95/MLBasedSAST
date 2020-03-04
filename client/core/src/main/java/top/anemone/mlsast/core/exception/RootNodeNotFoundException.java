package top.anemone.mlsast.core.exception;

public class RootNodeNotFoundException extends NotFoundException{
    public RootNodeNotFoundException(Object element, Object container) {
        super(element, container);
    }
}
