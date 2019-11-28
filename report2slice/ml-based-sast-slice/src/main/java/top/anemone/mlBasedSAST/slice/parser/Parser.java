package top.anemone.mlBasedSAST.slice.parser;

import edu.umd.cs.findbugs.PluginException;
import top.anemone.mlBasedSAST.slice.exception.BCELParserException;
import top.anemone.mlBasedSAST.slice.exception.NotFoundException;
import top.anemone.mlBasedSAST.slice.data.TaintProject;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface Parser {
    TaintProject parse(File xml, List<File> appJars) throws NotFoundException, IOException, BCELParserException, PluginException;
}
