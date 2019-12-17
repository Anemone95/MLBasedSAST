package top.anemone.mlsast.slice.parser;

import top.anemone.mlsast.slice.exception.NotFoundException;
import top.anemone.mlsast.slice.data.TaintProject;
import top.anemone.mlsast.slice.exception.ParserException;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface Parser {
    TaintProject report2taintProject(File xml, List<File> appJars) throws ParserException, NotFoundException;
}
