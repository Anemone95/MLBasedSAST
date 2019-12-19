package top.anemone.mlsast.core.parser.impl;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import javafx.scene.effect.Light;
import top.anemone.mlsast.core.Monitor;
import top.anemone.mlsast.core.data.TaintFlow;
import top.anemone.mlsast.core.data.TaintProject;
import top.anemone.mlsast.core.exception.BCELParserException;
import top.anemone.mlsast.core.exception.NotFoundException;
import top.anemone.mlsast.core.exception.ParserException;
import top.anemone.mlsast.core.parser.ReportParser;
import top.anemone.mlsast.core.utils.ExceptionUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static top.anemone.mlsast.core.parser.impl.SpotbugXMLReportParser.getBugInstanceTaintProject;

public class SpotbugBugCollectionParser implements ReportParser<BugInstance> {
    private BugCollection bugCollection;
    public SpotbugBugCollectionParser(BugCollection bugCollection){
        this.bugCollection=bugCollection;
    }
    @Override
    public TaintProject<BugInstance> report2taintProject(Monitor monitor) throws ParserException, NotFoundException {
        List<BugInstance> bugInstances = SpotbugXMLReportParser.secBugFilter(bugCollection);
        return getBugInstanceTaintProject(monitor, null,  bugCollection);
    }
}
