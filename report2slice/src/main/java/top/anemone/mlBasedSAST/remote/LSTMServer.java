package top.anemone.mlBasedSAST.remote;


import lombok.Data;
import top.anemone.mlBasedSAST.data.VO.Label;
import top.anemone.mlBasedSAST.data.VO.Response;
import top.anemone.mlBasedSAST.utils.OkHttp;

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
            e.printStackTrace();
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
}
