package top.anemone.mlsast.core.slice;

import lombok.Data;
import top.anemone.mlsast.core.data.TaintFlow;
import top.anemone.mlsast.core.data.TaintProject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class SliceProject<T> {
    protected Map<T, String> bugInstance2slice;
    protected TaintProject<T> taintProject;
    public SliceProject(TaintProject<T> taintProject){
        this.taintProject=taintProject;
        bugInstance2slice=new HashMap<>();
    }

    public String getProjectName() {
        return taintProject.getProjectName();
    }

    public List<TaintFlow> getTaintFlow(T bugInstance) {
        return taintProject.getTaintFlowMap().get(bugInstance);
    }
    public List<T> getBugInstances(){
        return taintProject.getBugInstances();
    }
    public T getBugInstance(int idx){
        return taintProject.getBugInstances().get(idx);
    }
}
