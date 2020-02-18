package top.anemone.mlsast.core.slice.impl;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Formater {
    public static String prepareSliceForEncoding(final SDG sdg, Collection<SDGNode> slice, ArrayList<SDGNode> prune) {
        List<SDGNode> sortedSlice = new ArrayList<>(slice);
        Collections.sort(sortedSlice, Comparator.comparingInt(SDGNode::getId));
        StringBuilder result = new StringBuilder();
        for (SDGNode node : sortedSlice) {
            if (prune.contains(node)) continue;
            result.append(node + " :: " + node.getKind() + " :: " + node.getOperation() + " :: " + node.getType() + " :: " + node.getLabel() + "\n");
        }
        return result.toString();
    }
}
