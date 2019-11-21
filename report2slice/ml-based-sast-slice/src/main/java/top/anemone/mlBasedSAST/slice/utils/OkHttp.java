package top.anemone.mlBasedSAST.slice.utils;

import com.alibaba.fastjson.JSON;
import okhttp3.*;

import java.io.IOException;

public class OkHttp {
    private static OkHttpClient client = new OkHttpClient();
    private static MediaType jsonType = MediaType.parse("application/json");
    public static <T> T  get(String url, Class<T> clazz) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = null;
        response = client.newCall(request).execute();
        if (response.isSuccessful()){
            T obj = JSON.parseObject(response.body().string(), clazz);
            return obj;
        } else {
            return null;
        }
    }

    public static <T,V> T post(String url, V VO, Class<T> clazz) throws IOException {
        RequestBody requestBody = RequestBody.create(jsonType, JSON.toJSONString(VO));
        Request request = new Request.Builder()
                .post(requestBody)
                .url(url)
                .build();

        Response response = null;
        response = client.newCall(request).execute();
        if (response.isSuccessful()){
            T obj = JSON.parseObject(response.body().string(), clazz);
            return obj;
        } else {
            return null;
        }
    }
}
