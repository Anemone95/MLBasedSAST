package top.anemone.mlsast.core.predict;

public enum PredictEnum {
    TRUE("True"),
    FALSE("False"),
    ERROR("Error");

    private final String result;
    PredictEnum(String s) {
        this.result=s;
    }

    @Override
    public String toString() {
        return this.result;
    }
}
