package top.anemone.mlsast.core.slice;

import top.anemone.mlsast.core.data.taintTree.MethodLocation;
import top.anemone.mlsast.core.data.taintTree.TaintEdge;
import top.anemone.mlsast.core.data.taintTree.TaintFlow;
import top.anemone.mlsast.core.data.taintTree.TaintTreeNode;

import java.util.LinkedList;
import java.util.List;

public class DFSTaintTree {
    private TaintTreeNode source;
    private List<TaintFlow> taintFlows;
    private List<MethodLocation> sinkCallerLocations;

    /**
     * 一个source可能返回多个taintflow
     * @param source 一个source函数节点
     * @return 多个taintflow，TaintEdge的集合
     */
    public List<TaintEdge> getTaintFlows(TaintTreeNode source){
        taintFlows=new LinkedList<>();
        sinkCallerLocations=new LinkedList<>();
        TaintFlow taintFlow=new TaintFlow();
        boolean hasNext=dfs(source, taintFlow);
        if (hasNext){
            return taintFlow;
        }
        return null;
    }

    /**
     * 返回source函数内是否包含sink
     *
     * @param source
     * @return
     */
    public boolean dfs(TaintTreeNode source, TaintFlow taintFlow) {
        if (source.type == TaintTreeNode.NodeType.METHOD && sinkCallerLocations.contains((MethodLocation) source.location)){
            return false;
        }
        boolean hasSink=false;
        TaintTreeNode nextNode = source.firstChildNode;
        while (nextNode!=null) {
            if (nextNode.type == TaintTreeNode.NodeType.METHOD) {
                MethodLocation callee = (MethodLocation) nextNode.location;
                if (sinkCallerLocations.contains(callee)) {
                    nextNode=nextNode.nextSibling;
                    continue;
                }
                hasSink=hasSink|dfs(nextNode, taintFlow);
                if (hasSink){
                    taintFlow.add(new TaintEdge(((MethodLocation) source.location).func, nextNode.location));
                    return hasSink;
                }

            }
            if (nextNode.type == TaintTreeNode.NodeType.SINK) {
                sinkCallerLocations.add((MethodLocation) source.location);
                taintFlow.add(new TaintEdge(((MethodLocation) source.location).func, nextNode.location));
                return true;
            }
            if (nextNode.type== TaintTreeNode.NodeType.RETURN){
                // 针对一个函数有多个return情况，只考虑最后一个return
                if (nextNode.nextSibling==null){
                    taintFlow.add(new TaintEdge(((MethodLocation) source.location).func, nextNode.location));
                    return hasSink;
                }
            }
            nextNode=nextNode.nextSibling;
        }
        return hasSink;
    }

}
