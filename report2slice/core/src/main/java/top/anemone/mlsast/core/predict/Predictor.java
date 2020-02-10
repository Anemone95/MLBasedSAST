package top.anemone.mlsast.core.predict;

import top.anemone.mlsast.core.data.VO.Label;
import top.anemone.mlsast.core.predict.exception.PredictorException;
import top.anemone.mlsast.core.data.VO.Slice;

public interface Predictor {
    boolean predictIsSafe(Slice slice) throws PredictorException;
    void label(Label label) throws PredictorException;
}
