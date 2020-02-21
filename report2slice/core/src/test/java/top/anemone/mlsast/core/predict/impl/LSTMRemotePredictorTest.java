package top.anemone.mlsast.core.predict.impl;

import org.junit.Ignore;
import org.junit.Test;
import top.anemone.mlsast.core.data.Func;
import top.anemone.mlsast.core.data.VO.Slice;
import top.anemone.mlsast.core.data.taintTree.Location;
import top.anemone.mlsast.core.data.taintTree.TaintFlow;
import top.anemone.mlsast.core.predict.Predictor;
import top.anemone.mlsast.core.predict.exception.PredictorException;

public class LSTMRemotePredictorTest {

    @Ignore
    @Test
    public void predict() throws PredictorException {
        Predictor lstmPredictor=new BLSTMRemotePredictor("http://localhost:8000/");
        boolean isTrue=lstmPredictor.predictIsSafe(new Slice(new TaintFlow(new Func("A","B","C"),new Location("A",0,0))
                ,"asd efg","test"));
        System.out.println(isTrue);
    }

    @Ignore
    @Test
    public void isAlive() {
        BLSTMRemotePredictor lstmServer = new BLSTMRemotePredictor("http://localhost:8000/");
        System.out.println(lstmServer.isAlive());
    }
}