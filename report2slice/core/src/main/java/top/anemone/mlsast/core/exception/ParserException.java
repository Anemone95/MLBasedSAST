package top.anemone.mlsast.core.exception;

public class ParserException extends Exception {
    private Exception rawException;

    public ParserException(String s, Exception rawException) {
        super(s);
        this.rawException=rawException;
    }
}
