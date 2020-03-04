package top.anemone.mlsast.core.parser;

import top.anemone.mlsast.core.Monitor;
import top.anemone.mlsast.core.exception.NotFoundException;
import top.anemone.mlsast.core.data.TaintProject;
import top.anemone.mlsast.core.exception.ParserException;


public interface ReportParser<T> {
    TaintProject<T> report2taintProject(Monitor monitor) throws ParserException, NotFoundException;
}
