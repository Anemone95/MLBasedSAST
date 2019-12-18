package top.anemone.mlsast.core.remote;

import org.junit.Test;
import top.anemone.mlsast.core.predict.impl.LSTMRemotePredictor;

public class LSTMRemotePredictorTest {

    @Test
    public void isAlive() {
        LSTMRemotePredictor lstmServer=new LSTMRemotePredictor("http://localhost:8888/");
        System.out.println(lstmServer.isAlive());
    }
}