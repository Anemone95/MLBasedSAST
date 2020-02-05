package top.anemone.mlsast.core.data.taintTree;

public class TaintTreeNode {
    public enum NodeType {
        METHOD, RETURN, SINK, UNK
    }

    public Location location;
    public NodeType type;
    public TaintTreeNode firstChildNode;
    public TaintTreeNode nextSibling;

    @Override
    public String toString() {
        return "TaintTreeNode{" +
                "location=" + location +
                ", type=" + type +
                '}';
    }

    public TaintTreeNode(Location location) {
        this.location = location;
        if (location instanceof SinkLocation) {
            type = NodeType.SINK;
        } else if (location instanceof ReturnLocation) {
            type = NodeType.RETURN;
        } else if (location instanceof MethodLocation) {
            type = NodeType.METHOD;
        } else {
            type = NodeType.UNK;
        }
    }
}
