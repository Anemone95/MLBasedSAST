package top.anemone.mlsast.core;

public interface Monitor {
    void init(String stageName, int totalWork);

    void process(int idx, int totalWork, Object input, Object output, String error);
}
