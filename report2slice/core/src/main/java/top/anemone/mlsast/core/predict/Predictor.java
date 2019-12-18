package top.anemone.mlsast.core.predict;

import top.anemone.mlsast.core.predict.exception.PredictorException;
import top.anemone.mlsast.core.data.VO.Slice;

public interface Predictor {
    boolean predict(Slice slice) throws PredictorException;
}
