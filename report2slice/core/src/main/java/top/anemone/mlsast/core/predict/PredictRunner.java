package top.anemone.mlsast.core.predict;

import top.anemone.mlsast.core.exception.PredictorRunnerException;
import top.anemone.mlsast.core.predict.exception.PredictorException;
import top.anemone.mlsast.core.Monitor;
import top.anemone.mlsast.core.slice.SliceProject;
import top.anemone.mlsast.core.data.TaintFlow;
import top.anemone.mlsast.core.data.VO.Slice;
import top.anemone.mlsast.core.exception.NotFoundException;
import top.anemone.mlsast.core.exception.ParserException;
import top.anemone.mlsast.core.exception.SliceRunnerException;
import top.anemone.mlsast.core.parser.ReportParser;
import top.anemone.mlsast.core.slice.SliceRunner;
import top.anemone.mlsast.core.slice.Slicer;
import top.anemone.mlsast.core.utils.ExceptionUtil;

import java.util.List;

public class PredictRunner<T> {
    private Predictor predictor;
    private SliceRunner<T> sliceRunner;

    public PredictRunner() {
        sliceRunner = new SliceRunner<>();
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

    public PredictProject<T> run(Monitor monitor) throws ParserException, NotFoundException, SliceRunnerException, PredictorRunnerException {
        return run(monitor, new PredictProject<>());
    }
    public PredictProject<T> run(Monitor monitor, PredictProject<T> predictProject) throws ParserException, NotFoundException, SliceRunnerException, PredictorRunnerException {
        if (predictor==null){
            throw new PredictorRunnerException("Predictor not set");
        }
        SliceProject<T> sliceProject;
        if (predictProject.getSliceProject()==null){
            sliceProject = sliceRunner.run(monitor);
            predictProject.setSliceProject(sliceProject);
        } else {
            sliceProject=predictProject.getSliceProject();
        }
        monitor.init("Prediction", sliceProject.getBugInstances().size());
        for (int i = 0; i < sliceProject.getBugInstances().size(); i++) {
            List<TaintFlow> flows = sliceProject.getTaintFlow(sliceProject.getBugInstances().get(i));
            PredictEnum isTP = PredictEnum.ERROR;
            if (flows==null){
                predictProject.putPrediction(sliceProject.getBugInstances().get(i),isTP);
                continue;
            }
            TaintFlow flow=flows.get(0);
            String sliceStr = sliceProject.getBugInstance2slice().get(sliceProject.getBugInstances().get(i));
            if (sliceStr==null){
                predictProject.putPrediction(sliceProject.getBugInstances().get(i),isTP);
                continue;
            }
            Slice slice = new Slice(flow, sliceStr, flow.getHash(), sliceProject.getTaintProject().getProjectName());
            String err=null;
            try {
                if (predictor.predict(slice)) {
                    isTP = PredictEnum.TRUE;
                } else {
                    isTP = PredictEnum.FALSE;
                }
            } catch (PredictorException e) {
                err = ExceptionUtil.getStackTrace(e);
            }
            predictProject.putPrediction(sliceProject.getBugInstances().get(i),isTP);
            monitor.process(i+1, sliceProject.getBugInstances().size(), slice, isTP, err);
        }
        return predictProject;
    }
}