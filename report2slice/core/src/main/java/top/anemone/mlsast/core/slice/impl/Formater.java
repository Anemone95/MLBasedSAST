package top.anemone.mlsast.core.slice.impl;

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
        StringBuilder result = new StringBuilder();
        for (SDGNode node : slice) {
            if (prune.contains(node)) continue;
            result.append(node+" :: "+node.getKind() + " :: " + node.getOperation() + " :: " + node.getType() + " :: " + node.getLabel() + "\n");
        }
        return result.toString();
    }
}
