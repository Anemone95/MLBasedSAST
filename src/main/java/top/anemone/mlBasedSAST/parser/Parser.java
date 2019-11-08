package top.anemone.mlBasedSAST.parser;

import top.anemone.mlBasedSAST.data.TaintProject;
import top.anemone.mlBasedSAST.exception.BCELParserException;
import top.anemone.mlBasedSAST.exception.NotFoundException;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface Parser {
    TaintProject parse(File xml, List<File> appJars) throws NotFoundException, IOException, BCELParserException;
}
