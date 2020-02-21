package top.anemone.mlsast.core.slice.impl;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Formatter {
    public static String prepareSliceForEncoding(Collection<SDGNode> slice) {
        List<SDGNode> sortedSlice = new ArrayList<>(slice);
        sortedSlice.sort(Comparator.comparingInt(SDGNode::getId));
        StringBuilder result = new StringBuilder();
        for (SDGNode node : sortedSlice) {
            result.append(node).append(" :: ").append(node.getKind()).append(" :: ").append(node.getOperation()).append(" :: ").append(node.getType()).append(" :: ").append(node.getLabel()).append("\n");
        }
        return result.toString();
    }
}
