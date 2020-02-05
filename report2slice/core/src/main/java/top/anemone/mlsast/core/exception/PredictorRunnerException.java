package top.anemone.mlsast.core.exception;

import java.util.LinkedList;
import java.util.List;

public class PredictorRunnerException extends Exception {
    final private List<Exception> exceptions;
    public PredictorRunnerException(String s, List<Exception> exceptions) {
        super(s);
        this.exceptions=exceptions;
    }

    public List<Exception> getExceptions() {
        return exceptions;
    }

    public int getExceptionsNum(){
        return exceptions.size();
    }

    public void addException(Exception exception) {
        exceptions.add(exception);
    }
}
