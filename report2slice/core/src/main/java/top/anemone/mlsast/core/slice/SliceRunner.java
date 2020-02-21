package top.anemone.mlsast.core.slice;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import org.apache.commons.lang.exception.ExceptionUtils;
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
import top.anemone.mlsast.core.slice.impl.JoanaSlicer;
import top.anemone.mlsast.core.utils.ExceptionUtil;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SliceRunner<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SliceRunner.class);

    private ReportParser<T> reportParser;
    private Slicer slicer;
    TaintProject<T> taintProject;
    SliceProject<T> project;
    private int poolSize = (int) (Runtime.getRuntime().availableProcessors() * 0.8);

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
        final List<Exception> exception = new LinkedList<>();
        try {
            slicer.config(taintProject.getAppJars(), new LinkedList<>(), null);
            monitor.process(1, 1, null, null, null);
        } catch (ClassHierarchyException | IOException e) {
            monitor.process(1, 1, null, null, e);
        }

        monitor.init("Slice", taintProject.getTaintTreeMap().size());
        project = new SliceProject<>(taintProject);
        for (int i = 0; i < taintProject.getBugInstances().size(); i++) {
//            ExecutorService ex = Executors.newFixedThreadPool(poolSize);
            List<TaintTreeNode> taintTrees = taintProject.getTaintTrees(taintProject.getBugInstances().get(i));
            if (taintTrees == null) {
                monitor.process(i + 1, taintProject.getBugInstances().size(), taintTrees, null, new NotFoundException(taintProject.getBugInstances().get(i), taintProject.getBugInstances()));
                continue;
            } else if (taintTrees.isEmpty()) {
                LOGGER.warn("No taint flow in this bugInstance");
                continue;
            }
            String slice = null;
            for (TaintTreeNode node : taintTrees) {
                int finalI = i;
                try {
                    sliceTaintTree(node);
                } catch (SlicerException e) {
                    exception.add(e.getRawException());
                    LOGGER.error(ExceptionUtil.getStackTrace(e.getRawException()));
                }
//                ex.execute(() -> {
//                });
            }
//            ex.shutdown();
            if (exception.isEmpty()) {
                monitor.process(i + 1, taintProject.getBugInstances().size(), taintTrees, slice, null);
            } else {
                monitor.process(i + 1, taintProject.getBugInstances().size(), taintTrees, slice, exception.get(0));
            }
            if (slicer instanceof JoanaSlicer) {
                ((JoanaSlicer) slicer).clearCache();
            }
        }
        return project;
    }

    /**
     * 计算所有需要的切片
     *
     * @param source 污点入口函数
     * @throws SlicerException
     */
    public void sliceTaintTree(TaintTreeNode source) throws SlicerException {
        List<TaintFlow> flows = new DFSTaintTree().getTaintFlows(source);
        project.source2taintFlow.put(source, flows);
        for (TaintFlow taintFlow : flows) {
            for (TaintEdge edge : taintFlow) {
                sliceFlow(edge);
            }
        }
    }

    public String sliceFlow(TaintEdge edge) throws SlicerException {
        String slice = project.getSlice(edge.entry, edge.point);
        // slice in cache
        if (slice != null) {
            return slice;
        } else {
            slice = slicer.computeSlice(edge.entry, edge.point);
            project.putSlice(edge.entry, edge.point, slice);
            return slice;
        }
    }

}
