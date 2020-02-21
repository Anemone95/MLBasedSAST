package top.anemone.mlsast.core.predict.impl;


import lombok.Data;
import top.anemone.mlsast.core.data.VO.Label;
import top.anemone.mlsast.core.data.VO.Response;
import top.anemone.mlsast.core.data.VO.Slice;
import top.anemone.mlsast.core.predict.exception.PredictorException;
import top.anemone.mlsast.core.predict.Predictor;
import top.anemone.mlsast.core.utils.OkHttp;

import java.io.IOException;


@Data
public class BLSTMRemotePredictor implements Predictor {
    private String remoteServer;
    private String token = "testtest";

    public void setToken(String token) {
        this.token = token;
    }

    public BLSTMRemotePredictor(String serverURL) {
        if (!serverURL.startsWith("http")) {
            serverURL = "http://" + serverURL;
        }
        if (serverURL.endsWith("/")) {
            serverURL = serverURL.substring(0, serverURL.length() - 1);
        }
        this.remoteServer = serverURL;
    }

    public BLSTMRemotePredictor(String serverURL, String token) {
        if (!serverURL.startsWith("http")) {
            serverURL = "http://" + serverURL;
        }
        if (serverURL.endsWith("/")) {
            serverURL = serverURL.substring(0, serverURL.length() - 1);
        }
        this.remoteServer = serverURL;
        this.token=token;
    }

    public boolean isAlive() {
        Response serverStatus = null;
        try {
            serverStatus = OkHttp.get(this.remoteServer + "/alive/?token=" + token, Response.class);
        } catch (IOException e) {
            return false;
        }
        if (serverStatus != null) {
            return serverStatus.getMsg().equals("alive");
        } else {
            return false;
        }
    }

    @Override
    public boolean predictIsSafe(Slice slice) throws PredictorException {
        Response response = null;
        try {
            response = OkHttp.post(this.remoteServer + "/predict/?token=" + token, slice, Response.class);
        } catch (IOException e) {
            throw new PredictorException(e.getMessage(), e);
        }
        if (response == null) {
            throw new PredictorException("Remote return null", null);
        }
        // FIXME 更新模型端将取反取消
        return !response.getMsg().equals("True");
    }

    @Override
    public void label(Label label) throws PredictorException {
        Response response;
        try {
            response = OkHttp.post(this.remoteServer + "/label/?token=" + token, label, Response.class);
        } catch (IOException e) {
            throw new PredictorException(e.getMessage(), e);
        }
    }

}
