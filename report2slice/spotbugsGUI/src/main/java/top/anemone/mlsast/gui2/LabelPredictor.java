package top.anemone.mlsast.gui2;

import edu.umd.cs.findbugs.BugInstance;
import top.anemone.mlsast.core.data.VO.Label;
import top.anemone.mlsast.core.data.VO.Slice;
import top.anemone.mlsast.core.data.taintTree.TaintEdge;
import top.anemone.mlsast.core.exception.NotFoundException;
import top.anemone.mlsast.core.predict.Predictor;
import top.anemone.mlsast.core.predict.exception.PredictorException;

import java.util.HashMap;
import java.util.Map;

public class LabelPredictor implements Predictor {
    public static String EXCEPTION_MESSAGE="LabelPredictorException";
    private Map<TaintEdge, Boolean> edgeIsSafe=new HashMap<>();
    @Override
    public boolean predictIsSafe(Slice slice) throws PredictorException {
        if (slice.getFlow()==null){
            throw new PredictorException(EXCEPTION_MESSAGE,new NullPointerException("slice doesn't have this taint edge"));
        }
        if (!edgeIsSafe.containsKey(slice.getFlow())){
            throw new PredictorException(EXCEPTION_MESSAGE, new NotFoundException(slice.getFlow(), edgeIsSafe));
        }
        return edgeIsSafe.get(slice.getFlow());
    }

    @Override
    public void label(Label label) throws PredictorException {
        edgeIsSafe.put(label.getTaintEdge(), label.isSafe());
    }
}
