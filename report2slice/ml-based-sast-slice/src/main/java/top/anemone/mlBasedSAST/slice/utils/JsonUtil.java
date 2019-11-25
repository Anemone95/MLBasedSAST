package top.anemone.mlBasedSAST.slice.utils;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;

public class JsonUtil {

    public static Gson gson = new GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting().create();
    public static<T> String toJson(T obj){
        return gson.toJson(obj);
    }
    public static<T> void dumpToFile(T obj, String output) throws IOException {
        gson.toJson(obj, new FileWriter(output));
    }
}
