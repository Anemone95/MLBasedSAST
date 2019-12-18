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
public class LSTMRemotePredictor implements Predictor {
    private String remoteServer;

    public LSTMRemotePredictor(String serverURL) {
        if (!serverURL.startsWith("http")) {
            serverURL = "http://" + serverURL;
        }
        if (serverURL.endsWith("/")) {
            serverURL = serverURL.substring(0, serverURL.length() - 1);
        }
        this.remoteServer = serverURL;
    }

    public boolean isAlive() {
        Response serverStatus = null;
        try {
            serverStatus = OkHttp.get(this.remoteServer + "/alive", Response.class);
        } catch (IOException e) {
            return false;
        }
        if (serverStatus != null) {
            return serverStatus.getMsg().equals("alive");
        } else {
            return false;
        }
    }

    public Response postLabel(Label label) throws IOException {
        Response response = OkHttp.post(this.remoteServer + "/label", label, Response.class);
        return response;
    }

    @Override
    public boolean predict(Slice slice) throws PredictorException {
        Response response = null;
        try {
            response = OkHttp.post(this.remoteServer + "/predict", slice, Response.class);
        } catch (IOException e) {
            throw new PredictorException(e.getMessage(), e);
        }
        if (response == null) {
            throw new PredictorException("Remote return null", null);
        }
        return response.getMsg().equals("True");
    }

    public Response label(Label label) throws PredictorException {
        Response response;
        try {
            response = OkHttp.post(this.remoteServer + "/label", label, Response.class);
        } catch (IOException e) {
            throw new PredictorException(e.getMessage(), e);
        }
        return response;
    }

}
