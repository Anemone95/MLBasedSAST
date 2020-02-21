package top.anemone.mlsast.core.predict;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.anemone.mlsast.core.data.VO.Slice;
import top.anemone.mlsast.core.data.taintTree.TaintFlow;
import top.anemone.mlsast.core.data.taintTree.TaintTreeNode;
import top.anemone.mlsast.core.exception.PredictorRunnerException;
import top.anemone.mlsast.core.Monitor;
import top.anemone.mlsast.core.predict.exception.PredictorException;
import top.anemone.mlsast.core.slice.SliceProject;
import top.anemone.mlsast.core.exception.NotFoundException;
import top.anemone.mlsast.core.exception.ParserException;
import top.anemone.mlsast.core.exception.SliceRunnerException;
import top.anemone.mlsast.core.parser.ReportParser;
import top.anemone.mlsast.core.slice.SliceRunner;
import top.anemone.mlsast.core.slice.Slicer;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class PredictRunner<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PredictRunner.class);
    private Predictor predictor;
    final private SliceRunner<T> sliceRunner;
    private SliceProject<T> sliceProject;
    final private PredictProject<T> predictProject;
    public static String STAGE = "Prediction";

    public PredictRunner() {
        sliceRunner = new SliceRunner<>();
        predictProject = new PredictProject<>();
    }

    public PredictRunner(PredictProject<T> predictProject) {
        this.sliceRunner = new SliceRunner<>();
        this.predictProject = predictProject;
    }

    public PredictRunner(SliceProject<T> sliceProject) {
        this.sliceRunner = new SliceRunner<>();
        predictProject = new PredictProject<>();
        predictProject.setSliceProject(sliceProject);
    }

    public PredictRunner<T> setReportParser(ReportParser<T> reportParser) {
        this.sliceRunner.setReportParser(reportParser);
        return this;
    }

    public PredictRunner<T> setSlicer(Slicer slicer) {
        this.sliceRunner.setSlicer(slicer);
        return this;
    }

    public PredictRunner<T> setPredictor(Predictor predictor) {
        this.predictor = predictor;
        return this;
    }

    public PredictProject<T> run(Monitor monitor) throws ParserException, SliceRunnerException, NotFoundException, PredictorRunnerException {
        if (predictor == null) {
            throw new PredictorRunnerException("Predictor not set", new LinkedList<>());
        }
        sliceProject = predictProject.getSliceProject();
        if (sliceProject == null) {
            sliceProject = sliceRunner.run(monitor);
            predictProject.setSliceProject(sliceProject);
        } else {
        }
        monitor.init(STAGE, sliceProject.getBugInstances().size());
        // 对于每一个漏洞
        for (int i = 0; i < sliceProject.getBugInstances().size(); i++) {
            List<Exception> causedExceptions = new LinkedList<>();
            T buginstance = sliceProject.getBugInstance(i);
            List<TaintTreeNode> taintTrees = sliceProject.getTaintProject().getTaintTrees(buginstance);
            // 对于每一个入口
            boolean instanceIsSafe = true;
            Set<TaintFlow> safeEdges=new HashSet<>();
            predictProject.putProofs(buginstance, safeEdges);
            for (TaintTreeNode source : taintTrees) {
                boolean treeIsSafe = false;
                Set<TaintFlow> flow = sliceProject.getTaintFlows(source);
                if (flow == null) {
                    causedExceptions.add(new NotFoundException(source, sliceProject));
                    continue;
                }
                if (flow.isEmpty()){
                    treeIsSafe=true;
                } else {
                    // 对于每一个污染流， 需要每一条边都不是清洁函数
                    for (TaintFlow subFlow : flow) {
                        boolean edgeIsSafe = false;
                        try {
                            edgeIsSafe = edgeIsSafe(subFlow);
                        } catch (PredictorException e) {
                            causedExceptions.add(e);
                        }
                        if (edgeIsSafe){
                            treeIsSafe=true;
                            safeEdges.add(subFlow);
                            break;
                        }
                    }
                }
                instanceIsSafe = instanceIsSafe & treeIsSafe;
            }
            predictProject.putPrediction(sliceProject.getBugInstances().get(i), instanceIsSafe);
            if (causedExceptions.isEmpty()) {
                monitor.process(i + 1, sliceProject.getBugInstances().size(), buginstance, instanceIsSafe, null);
            } else {
                monitor.process(i + 1, sliceProject.getBugInstances().size(), buginstance, instanceIsSafe, new PredictorRunnerException("Dozens of Exceptions", causedExceptions));
                predictProject.putExceptions(buginstance, causedExceptions);
            }
        }
        return predictProject;
    }

    public boolean edgeIsSafe(TaintFlow edge) throws PredictorException {
        // 曾经计算过则直接返回
        Boolean result = predictProject.bugIsSafe(edge);
        if (result != null) {
            return result;
        }
        String sliceStr = sliceProject.getSlice(edge.entry, edge.point);
        if (sliceStr == null) {
            LOGGER.warn("BugInstance's slice not found, edge=" + edge.toString());
            return false;
        }
        Slice slice = new Slice(edge, sliceStr, sliceProject.getTaintProject().getProjectName());
        boolean edgeIsClean;
        // 预测漏洞存在则该路径是不安全的
        edgeIsClean = predictor.predictIsSafe(slice);
        predictProject.putPrediction(edge, edgeIsClean);
        return edgeIsClean;
    }
}