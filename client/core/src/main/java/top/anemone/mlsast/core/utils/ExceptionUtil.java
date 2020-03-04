package top.anemone.mlsast.core.utils;

import top.anemone.mlsast.core.exception.PredictorRunnerException;
import top.anemone.mlsast.core.exception.SliceRunnerException;
import top.anemone.mlsast.core.exception.SlicerException;
import top.anemone.mlsast.core.predict.exception.PredictorException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public class ExceptionUtil {
    public static String getStackTrace(Throwable e){
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

}
