package top.anemone.mlsast.core.slice;

import lombok.Data;
import top.anemone.mlsast.core.data.Func;
import top.anemone.mlsast.core.data.TaintProject;
import top.anemone.mlsast.core.data.taintTree.*;
import top.anemone.mlsast.core.utils.SHA1;

import java.util.*;

@Data
public class SliceProject<T> {
    @Deprecated
    protected Map<T, String> bugInstance2slice;
    protected Map<TaintEdge, String> taintEdge2slice;
    // 一个taintflowtree包含多条flow
    protected Map<TaintTreeNode, List<TaintFlow>> source2taintFlow;
    protected TaintProject<T> taintProject;

    public SliceProject(TaintProject<T> taintProject) {
        this.taintProject = taintProject;
        bugInstance2slice = new HashMap<>();
        taintEdge2slice = new HashMap<>();
        source2taintFlow = new HashMap<>();
    }

    public String getProjectName() {
        return taintProject.getProjectName();
    }

    public List<TaintFlow> getTaintFlows(TaintTreeNode source) {
        return source2taintFlow.get(source);
    }

    public Set<TaintEdge> getTaintEdges(T buginstance) {
        Set<TaintEdge> edges = new HashSet<>();
        for (TaintTreeNode taintTreeNode : getTaintTrees(buginstance)) {
            for (TaintFlow taintFlow : getTaintFlows(taintTreeNode)) {
                edges.addAll(taintFlow);
            }
        }
        return edges;
    }

    public void putSlice(Func func, Location point, String slice) {
        TaintEdge edge = new TaintEdge(func, point);
        taintEdge2slice.put(edge, slice);
    }

    public String getSlice(TaintEdge edge) {
        return taintEdge2slice.get(edge);
    }
    public String getSliceHash(TaintEdge edge){
        if (getSlice(edge)!=null){
            return SHA1.shaEncode(getSlice(edge));
        } else {
            return SHA1.shaEncode(edge.toString());
        }
    }

    public String getSlice(Func func, Location point) {
        TaintEdge edge = new TaintEdge(func, point);
        return taintEdge2slice.get(edge);
    }

    public List<T> getBugInstances() {
        return taintProject.getBugInstances();
    }

    public T getBugInstance(int idx) {
        return taintProject.getBugInstances().get(idx);
    }

    public List<TaintTreeNode> getTaintTrees(T bug) {
        return taintProject.getTaintTrees(bug);
    }
}
