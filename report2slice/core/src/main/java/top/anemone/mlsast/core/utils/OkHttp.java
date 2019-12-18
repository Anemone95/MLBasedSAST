package top.anemone.mlsast.core.utils;

import com.google.gson.Gson;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class OkHttp {
    private static OkHttpClient client = new OkHttpClient();
    {
        client.newBuilder()
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    private static MediaType jsonType = MediaType.parse("application/json");
    private static <T> T request(Request request, Class<T> clazz) throws IOException {
        Response response = null;
        response = client.newCall(request).execute();
        if (response.isSuccessful() && response.body() != null) {
            T obj = new Gson().fromJson(response.body().string(), clazz);
            return obj;
        } else {
            return null;
        }
    }

    public static <T> T get(String url, Class<T> clazz) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        return request(request, clazz);
    }

    public static <T, V> T post(String url, V VO, Class<T> clazz) throws IOException {
        RequestBody requestBody = RequestBody.create(jsonType, new Gson().toJson(VO));
        Request request = new Request.Builder()
                .post(requestBody)
                .url(url)
                .build();
        return request(request, clazz);
    }
}
