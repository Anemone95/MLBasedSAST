package top.anemone.mlsast.core.data.taintTree;

import lombok.Data;
import lombok.NonNull;
import top.anemone.mlsast.core.data.Func;
import top.anemone.mlsast.core.utils.SHA1;

@Data
public class TaintEdge {
    @NonNull
    public Func entry;
    @NonNull
    public Location point;

    public String sha1() {
        StringBuilder sb = new StringBuilder();
        sb.append(entry.getClass());
        sb.append(entry.getMethod());
        sb.append(entry.getSig());
        sb.append(getPoint().sourceFile);
        sb.append("S");
        sb.append(getPoint().startLine);
        sb.append("E");
        sb.append(getPoint().endLine);
        return SHA1.shaEncode(sb.toString());
    }

    @Override
    public String toString() {
        if (point.startLine.equals(point.endLine)) {
            return entry.getClazz() + "#" + entry.getMethod() + "→" + "L:" + point.startLine;
        } else {
            return entry.getClazz() + "#" + entry.getMethod() + "→" + "L:" + point.startLine;
        }
    }
}
