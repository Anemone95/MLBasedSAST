package top.anemone.mlsast.slice.parser;

import edu.umd.cs.findbugs.PluginException;
import top.anemone.mlsast.slice.exception.BCELParserException;
import top.anemone.mlsast.slice.exception.NotFoundException;
import top.anemone.mlsast.slice.data.TaintProject;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface Parser {
    TaintProject parse(File xml, List<File> appJars) throws NotFoundException, IOException, BCELParserException, PluginException;
}
