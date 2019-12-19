package top.anemone.mlsast.core.slice;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import edu.umd.cs.findbugs.BugInstance;
import top.anemone.mlsast.core.data.TaintFlow;
import top.anemone.mlsast.core.data.TaintProject;
import top.anemone.mlsast.core.exception.NotFoundException;
import top.anemone.mlsast.core.exception.ParserException;
import top.anemone.mlsast.core.exception.SliceRunnerException;
import top.anemone.mlsast.core.exception.SlicerException;
import top.anemone.mlsast.core.parser.ReportParser;
import top.anemone.mlsast.core.Monitor;
import top.anemone.mlsast.core.parser.impl.SpotbugXMLReportParser;
import top.anemone.mlsast.core.slice.impl.JoanaSlicer;
import top.anemone.mlsast.core.utils.ExceptionUtil;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class SliceRunner<T> {
    private ReportParser<T> reportParser;
    private Slicer slicer;

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
            throw new SliceRunnerException("Report parser set");
        }
        if (slicer == null) {
            throw new SliceRunnerException("Slicer not set");
        }
        TaintProject<T> taintProject = reportParser.report2taintProject(monitor);
        monitor.init("Config Slicer", 1);
        String err = null;
        try {
            slicer.config(taintProject.getAppJars(), new LinkedList<>(), null);
        } catch (ClassHierarchyException | IOException e) {
            err = ExceptionUtil.getStackTrace(e);
        } finally {
            monitor.process(1, 1, null, null, err);
        }

        monitor.init("Slice", taintProject.getTaintFlowMap().size());
        SliceProject<T> project = new SliceProject<>(taintProject);
        for (int i = 0; i < taintProject.getTaintFlowMap().size(); i++) {
            List<TaintFlow> flow = taintProject.getTaintFlowMap().get(taintProject.getBugInstances().get(i));
            if (flow == null) {
                continue;
            }
            String slice = null;
            String error = null;
            try {
                slice = slicer.computeSlice(flow.get(0));
                project.getBugInstance2slice().put(taintProject.getBugInstances().get(i), slice);
            } catch (SlicerException e) {
                error = ExceptionUtil.getStackTrace(e.getRawException());
            }
            monitor.process(i + 1, taintProject.getBugInstances().size(), flow.get(0), slice, error);
        }
        return project;
    }

    public static void main(String[] args) throws NotFoundException, ParserException, SliceRunnerException {
        Monitor monitor = new Monitor() {
            @Override
            public void init(String stageName, int totalWork) {
                System.out.println(stageName + ", Total work: " + totalWork);
            }

            @Override
            public void process(int idx, int totalWork, Object input, Object output, String error) {
                System.out.println("Progress: " + idx + "/" + totalWork + ".");
                System.out.println(output);
            }
        };
        new SliceRunner<BugInstance>().setReportParser(new SpotbugXMLReportParser(
                new File("C:\\Users\\x5651\\Documents\\bishe\\MLBasedSAST\\report2slice\\core\\src\\test\\resources\\java-sec-code-1.0.0-spotbugs.xml"), null))
                .setSlicer(new JoanaSlicer()).run(monitor);
    }
}
