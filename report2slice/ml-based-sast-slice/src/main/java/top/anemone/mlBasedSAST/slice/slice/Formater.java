package top.anemone.mlBasedSAST.slice.slice;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Formater {
    public static String prepareSliceForEncoding(final SDG sdg, Collection<SDGNode> slice, ArrayList<SDGNode> prune) {
        HashMap<Integer, HashSet<SDGNode>> lookupTable = new HashMap<>();
        HashSet<Integer> seenAll = new HashSet<>();
        prune.stream().forEach(node -> lookupTable.put(node.getId(), new HashSet<SDGNode>()));
        // TODO 这里一个BFS不是很懂
        for (SDGNode node : prune) {
            if (seenAll.contains(node.getId())) continue;
            Queue<SDGNode> absQueue = new ConcurrentLinkedQueue<>();
            HashSet<Integer> seenNew = new HashSet<>();
            seenNew.add(node.getId());
            absQueue.add(node);
            while (!absQueue.isEmpty()) {
                SDGNode head = absQueue.poll();
                sdg.outgoingEdgesOf(head).stream().filter(e -> slice.contains(e.getTarget())).forEach(e -> {
                    SDGNode target = e.getTarget();
                    if (prune.contains(target)) {
                        if (!seenNew.contains(target.getId())) {
                            absQueue.add(target);
                            seenNew.add(target.getId());
                        }
                    } else {
                        seenNew.stream().forEach(id -> lookupTable.get(id).add(target));
                    }
                });
            }
            seenAll.addAll(seenNew);
        }
        // node_id::node_kind::node_operation::ret_type::node_Label(类java的语句)::跳转到其他块（CD, CE, ...）
        StringBuilder result = new StringBuilder("");
        for (SDGNode node : slice) {
            if (prune.contains(node)) continue;
            result.append("\n"+node+" :: "+node.getKind() + " :: " + node.getOperation() + " :: " + node.getType() + " :: "
                    + node.getLabel() + "::");
            HashSet<SDGNode> visited = new HashSet<>();
            String edgesStr = "";
            HashSet<SDGNode> successors = new HashSet<>();
            Set<SDGEdge> outgoingEdges = sdg.outgoingEdgesOf(node);
            outgoingEdges.stream().forEach(e -> successors.add(e.getTarget()));
            for (SDGEdge edge : outgoingEdges) {
                SDGNode target = edge.getTarget();
                if (!visited.contains(target) && slice.contains(target)) {
                    if (prune.contains(target)) {
                        for (SDGNode target2 : lookupTable.get(target.getId())) {
                            String estr = "JM," + target2.getId() + ":";
                            if (!successors.contains(target2) && !edgesStr.contains(estr)) {
                                edgesStr += estr;
                            }
                        }
                    } else {
                        edgesStr += edge.getKind() + "," + target.getId() + ":";
                    }
                    visited.add(target);
                }
            }
            if (!edgesStr.equals("")) {
                result.append(edgesStr.substring(0, edgesStr.length() - 1));
            }
        }
        return result.toString();
    }
}
