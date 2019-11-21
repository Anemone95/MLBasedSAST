package top.anemone.mlBasedSAST.slice.utils;


import com.alibaba.fastjson.JSONWriter;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.io.FileWriter;
import java.io.IOException;

public class JsonUtil {
    public static<T> void dumpToFile(T obj, String output) throws IOException {
        JSONWriter writer = new JSONWriter(new FileWriter(output));
        writer.config(SerializerFeature.PrettyFormat, true);
        writer.writeObject(obj);
        writer.close();
    }
}
