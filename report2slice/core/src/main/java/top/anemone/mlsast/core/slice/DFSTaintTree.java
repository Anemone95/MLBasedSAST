package top.anemone.mlsast.core.slice;

import top.anemone.mlsast.core.data.taintTree.MethodLocation;
import top.anemone.mlsast.core.data.taintTree.TaintFlow;
import top.anemone.mlsast.core.data.taintTree.TaintTreeNode;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class DFSTaintTree {

    /**
     * 一个source可能返回多个taintflow
     * @param source 一个source函数节点
     * @return 多个taintflow，TaintEdge的集合
     */
    public Set<TaintFlow> getTaintFlows(TaintTreeNode source){
        Set<TaintFlow> taintFlow=new HashSet<>();
        boolean hasNext=dfs(source, taintFlow);
        return taintFlow;
    }

    /**
     * 返回source函数内是否包含sink
     *
     * @param source
     * @return
     */
    public boolean dfs(TaintTreeNode source, Set<TaintFlow> taintFlow) {
        boolean hasSink=false;
        TaintTreeNode nextNode = source.firstChildNode;
        while (nextNode!=null) {
            if (nextNode.type == TaintTreeNode.NodeType.METHOD) {
                MethodLocation callee = (MethodLocation) nextNode.location;
                hasSink=hasSink|dfs(nextNode, taintFlow);
                if (hasSink){
                    taintFlow.add(new TaintFlow(((MethodLocation) source.location).func, nextNode.location));
                    return hasSink;
                }

            }
            if (nextNode.type == TaintTreeNode.NodeType.SINK) {
                taintFlow.add(new TaintFlow(((MethodLocation) source.location).func, nextNode.location));
                return true;
            }
            if (nextNode.type== TaintTreeNode.NodeType.RETURN){
                // 针对一个函数有多个return情况，只考虑最后一个return
                if (nextNode.nextSibling==null){
                    taintFlow.add(new TaintFlow(((MethodLocation) source.location).func, nextNode.location));
                    return hasSink;
                }
            }
            nextNode=nextNode.nextSibling;
        }
        return hasSink;
    }
}
