package top.anemone.mlsast.slice.remote;

import org.junit.Test;

public class LSTMServerTest {

    @Test
    public void isAlive() {
        LSTMServer lstmServer=new LSTMServer("http://localhost:8888/");
        System.out.println(lstmServer.isAlive());
    }
}