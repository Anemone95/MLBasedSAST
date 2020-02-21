package top.anemone.mlsast.core.data.taintTree;

import lombok.Data;
import lombok.NonNull;
import top.anemone.mlsast.core.data.Func;

@Data
public class TaintEdge {
    @NonNull
    public Func entry;
    @NonNull
    public Location point;

    @Override
    public String toString() {
        if (point.startLine.equals(point.endLine)) {
            return entry.getClazz() + "#" + entry.getMethod() + "→" + "L:" + point.startLine;
        } else {
            return entry.getClazz() + "#" + entry.getMethod() + "→" + "L:" + point.startLine;
        }
    }
}
