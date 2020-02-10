package top.anemone.mlsast.core.slice;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.anemone.mlsast.core.data.Func;
import top.anemone.mlsast.core.data.TaintProject;
import top.anemone.mlsast.core.data.taintTree.*;
import top.anemone.mlsast.core.exception.NotFoundException;
import top.anemone.mlsast.core.exception.ParserException;
import top.anemone.mlsast.core.exception.SliceRunnerException;
import top.anemone.mlsast.core.exception.SlicerException;
import top.anemone.mlsast.core.parser.ReportParser;
import top.anemone.mlsast.core.Monitor;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class SliceRunner<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SliceRunner.class);

    private ReportParser<T> reportParser;
    private Slicer slicer;
    TaintProject<T> taintProject;
    SliceProject<T> project;

    public SliceRunner<T> setReportParser(ReportParser<T> reportParser) {
        this.reportParser = reportParser;
        return this;
    }

    public SliceRunner<T> setSlicer(Slicer slicer) {
        this.slicer = slicer;
        return this;
    }

    public SliceProject<T> run(Monitor monitor) throws NotFoundException, ParserException, SliceRunnerException {
        if (reportParser == null) {
            throw new SliceRunnerException("Report parser not set");
        }
        if (slicer == null) {
            throw new SliceRunnerException("Slicer not set");
        }
        taintProject = reportParser.report2taintProject(monitor);
        if (monitor == null) {
            throw new SliceRunnerException("Monitor can't be null");
        }
        monitor.init("Config Slicer", 1);
        Exception exception = null;
        try {
            slicer.config(taintProject.getAppJars(), new LinkedList<>(), null);
        } catch (ClassHierarchyException | IOException e) {
            exception = e;
        } finally {
            monitor.process(1, 1, null, null, exception);
        }

        monitor.init("Slice", taintProject.getTaintTreeMap().size());
        project = new SliceProject<>(taintProject);
        for (int i = 0; i < taintProject.getBugInstances().size(); i++) {
            List<TaintTreeNode> taintTrees = taintProject.getTaintTrees(taintProject.getBugInstances().get(i));
            if (taintTrees == null) {
                monitor.process(i + 1, taintProject.getBugInstances().size(), taintTrees, null, new NotFoundException(taintProject.getBugInstances().get(i), taintProject.getBugInstances()));
                continue;
            } else if (taintTrees.isEmpty()){
                LOGGER.warn("No taint flow in this bugInstance");
                continue;
            }
            String slice = null;
            exception = null;
            for (TaintTreeNode node : taintTrees) {
                try {
                    sliceTaintFlow(node);
                } catch (SlicerException e) {
                    exception = e.getRawException();
                }
            }
            monitor.process(i + 1, taintProject.getBugInstances().size(), taintTrees, slice, exception);
        }
        return project;
    }

    /**
     * 计算所有需要的切片
     *
     * @param source 污点入口函数
     * @throws SlicerException
     */
    public void sliceTaintFlow(TaintTreeNode source) throws SlicerException {
        List<TaintFlow> flows = new DFSTaintTree().getTaintFlows(source);
        project.source2taintFlow.put(source, flows);
        for (TaintFlow taintFlow : flows) {
            for(TaintEdge edge: taintFlow){
                sliceFunc(edge.entry, edge.point);
            }
        }
    }

    public String sliceFunc(Func func, Location location) throws SlicerException {
        String slice = project.getSlice(func, location);
        // slice in cache
        if (slice != null) {
            return slice;
        } else {
            slice = slicer.computeSlice(func, location);
            project.putSlice(func, location, slice);
            return slice;
        }
    }

}
