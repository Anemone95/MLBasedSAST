package top.anemone.mlsast.core.predict.exception;

public class PredictorException extends Exception {
    private Exception rawException;
    public PredictorException(String s, Exception rawException) {
        super(s);
        this.rawException=rawException;
    }

    public Exception getRawException() {
        return rawException;
    }
}
