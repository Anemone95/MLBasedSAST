package top.anemone.mlsast.core.parser.impl;

import com.h3xstream.findsecbugs.injection.taintdata.LocationNodeAnnotation;
import com.h3xstream.findsecbugs.injection.taintdata.MethodNodeAnnotation;
import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.StringAnnotation;
import top.anemone.mlsast.core.data.taintTree.*;

import java.beans.MethodDescriptor;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SpotbugsBugInstanceParser {
    private Map<Integer, TaintTreeNode> id2TaintNode; // id到treenode的映射，用于重建tree
    private BugInstance bugInstance;

    class TinyTreeNode {
        int id;
        int childId;
        int nextId;

        public TinyTreeNode(int id, int childId, int nextId) {
            this.id = id;
            this.childId = childId;
            this.nextId = nextId;
        }
    }

    private List<TinyTreeNode> tinyTreeNodes;// 用于重建tree，只记录索引

    public SpotbugsBugInstanceParser(BugInstance bugInstance) {
        this.bugInstance = bugInstance;
        id2TaintNode = new HashMap<>();
        tinyTreeNodes = new LinkedList<>();
    }

    public List<TaintTreeNode> parse() {
        List<? extends BugAnnotation> annotations = bugInstance.getAnnotations();
        List<TaintTreeNode> entries = new LinkedList<>();
        boolean isEntry = false;
        for (BugAnnotation annotation : annotations) {
            if (annotation instanceof StringAnnotation && annotation.getDescription().equals("Taint entry")){
                // 上一入口, 根据id重建tree
                for (TinyTreeNode node : tinyTreeNodes) {
                    if (node.childId > 0) {
                        id2TaintNode.get(node.id).firstChildNode = id2TaintNode.get(node.childId);
                    }
                    if (node.nextId > 0) {
                        id2TaintNode.get(node.id).nextSibling = id2TaintNode.get(node.nextId);
                    }
                }
                id2TaintNode = new HashMap<>();
                tinyTreeNodes = new LinkedList<>();
            }
            if (annotation instanceof StringAnnotation && annotation.getDescription().equals("Taintflow tree")) {
                isEntry = true;
                continue;
            }
            Location location = null;
            int id = -1;
            if (annotation instanceof MethodNodeAnnotation) {
                MethodNodeAnnotation methodNodeAnnotation = (MethodNodeAnnotation) annotation;
                if (methodNodeAnnotation.getSourceLines() != null) {
                    if (methodNodeAnnotation.type.equals("SINK")) {
                        location = new SinkLocation(
                                methodNodeAnnotation.getSourceLines().getSourcePath(),
                                methodNodeAnnotation.getSourceLines().getStartLine(),
                                methodNodeAnnotation.getSourceLines().getEndLine()
                        );
                    } else {
                        location = new MethodLocation(
                                methodNodeAnnotation.getClassName(),
                                methodNodeAnnotation.getMethodName(),
                                methodNodeAnnotation.getMethodSignature(),
                                methodNodeAnnotation.getSourceLines().getSourcePath(),
                                methodNodeAnnotation.getSourceLines().getStartLine(),
                                methodNodeAnnotation.getSourceLines().getEndLine()
                        );
                    }
                } else {
                    location = new MethodLocation(
                            methodNodeAnnotation.getClassName(),
                            methodNodeAnnotation.getMethodName(),
                            methodNodeAnnotation.getMethodSignature(),
                            null,
                            null,
                            null
                    );
                }
                id = methodNodeAnnotation.id;
                TaintTreeNode node = new TaintTreeNode(location);
                int childId = -1;
                int nextId = -1;
                if (methodNodeAnnotation.firstId != null) {
                    childId = methodNodeAnnotation.firstId;
                }
                if (methodNodeAnnotation.nextId != null) {
                    nextId = methodNodeAnnotation.nextId;
                }
                tinyTreeNodes.add(new TinyTreeNode(id, childId, nextId));
                id2TaintNode.put(id, node);
                if (isEntry) {
                    entries.add(node);
                    isEntry = false;
                }
            } else if (annotation instanceof LocationNodeAnnotation) {
                LocationNodeAnnotation nodeAnnotation = (LocationNodeAnnotation) annotation;
                if (nodeAnnotation.type.equals("RETURN")) {
                    location = new ReturnLocation(nodeAnnotation.getSourceLines().getSourcePath(),
                            nodeAnnotation.getSourceLines().getStartLine(),
                            nodeAnnotation.getSourceLines().getEndLine());
                } else {
                    location = new Location(nodeAnnotation.getSourceLines().getSourcePath(),
                            nodeAnnotation.getSourceLines().getStartLine(),
                            nodeAnnotation.getSourceLines().getEndLine());
                }
                id = nodeAnnotation.id;
                TaintTreeNode node = new TaintTreeNode(location);
                if (nodeAnnotation.nextId != null) {
                    node.nextSibling = id2TaintNode.get(nodeAnnotation.nextId);
                }
                id2TaintNode.put(id, node);

                int childId = -1;
                int nextId = -1;
                if (nodeAnnotation.nextId != null) {
                    nextId = nodeAnnotation.nextId;
                }
                tinyTreeNodes.add(new TinyTreeNode(id, childId, nextId));
            }
        }
        // 最后入口, 根据id重建tree
        for (TinyTreeNode node : tinyTreeNodes) {
            if (node.childId > 0) {
                id2TaintNode.get(node.id).firstChildNode = id2TaintNode.get(node.childId);
            }
            if (node.nextId > 0) {
                id2TaintNode.get(node.id).nextSibling = id2TaintNode.get(node.nextId);
            }
        }
        return entries;
    }
}
