package top.anemone.mlsast.core.exception;


public class SlicerException extends Exception {
    private Exception rawException;
    public SlicerException(String s, Exception rawException) {
        super(s);
        this.rawException=rawException;
    }
}
