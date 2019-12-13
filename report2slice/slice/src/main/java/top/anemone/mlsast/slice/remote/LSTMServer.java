package top.anemone.mlsast.slice.remote;


import lombok.Data;
import top.anemone.mlsast.slice.data.VO.Label;
import top.anemone.mlsast.slice.data.VO.Response;
import top.anemone.mlsast.slice.data.VO.Slice;
import top.anemone.mlsast.slice.exception.RemoteException;
import top.anemone.mlsast.slice.utils.OkHttp;

import java.io.IOException;


@Data
public class LSTMServer {
    private String remoteServer;

    public LSTMServer(String serverURL) {
        if (!serverURL.startsWith("http")) {
            serverURL = "http://" + serverURL;
        }
        if (serverURL.endsWith("/")) {
            serverURL = serverURL.substring(0,serverURL.length()-1);
        }
        this.remoteServer = serverURL;
    }

    public boolean isAlive() {
        Response serverStatus= null;
        try {
            serverStatus = OkHttp.get(this.remoteServer+"/alive", Response.class);
        } catch (IOException e) {
            return false;
        }
        if(serverStatus !=null){
            return serverStatus.getMsg().equals("alive");
        } else {
            return false;
        }
    }

    public Response postLabel(Label label) throws IOException {
        Response response = OkHttp.post(this.remoteServer+"/label", label, Response.class);
        return response;
    }

    public boolean predict(Slice slice) throws RemoteException {
        Response response = null;
        try {
            response = OkHttp.post(this.remoteServer+"/predict", slice, Response.class);
        } catch (IOException e) {
            throw new RemoteException(e.getMessage());
        }
        if (response==null){
            throw new RemoteException("Remote return null");
        }
        return response.getMsg().equals("True");
    }
}
