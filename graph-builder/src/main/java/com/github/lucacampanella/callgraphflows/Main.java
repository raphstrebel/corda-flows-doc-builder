package com.github.lucacampanella.callgraphflows;

import java.io.IOException;
import java.util.concurrent.Callable;

import com.github.lucacampanella.callgraphflows.staticanalyzer.DecompilerEnum;
import com.github.lucacampanella.callgraphflows.staticanalyzer.SourceAndJarAnalyzer;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;


public class Main implements Callable<Void> {

    private Logger LOGGER;

    @CommandLine.Parameters(arity = "1..*", index="0..*", paramLabel = "filesToAnalyze",
            description = "The paths to the files that need to be analyzed, they can be " +
                    ".java files, folders or .jar files")
    private String[] filesPaths;

    @CommandLine.Option(names = {"-o", "--output"}, defaultValue = "graphs", description = "Output folder path")
    private String outputPath;

    @CommandLine.Option(names = {"-d", "--decompiler"}, defaultValue = "CFR", description = "Decompiler, choose between CFR and Fernflower")
    private String decompilerName;

    @CommandLine.Option(names = {"-l", "--draw-line-numbers"}, description = "draw the line numbers")
    boolean drawLineNumbers = false;

    @CommandLine.Option(names = {"--no-box-subflows"}, description = "don't draw a box around the subflows")
    boolean noDrawBoxAroundSubflow = false;

    @CommandLine.Option(names = {"-s", "--only-source-files"}, description = "analyze only the source files and not " +
            "the decompiled code")
    boolean analyzeOnlySources = false;

    @CommandLine.Option(names = {"--no-arrows"}, description = "Don't draw arrows between send and receive")
    boolean noArrows = false;

    @CommandLine.Option(names = {"--draw-return"}, description = "Draw the return statements")
    boolean drawReturn = false;

    @CommandLine.Option(names = {"--no-draw-throw"}, description = "Don't draw throw statements")
    boolean noDrawThrow = false;

    @CommandLine.Option(names = {"--no-draw-break-continue"}, description = "Don't draw break or continue statements")
    boolean noBreakContinue = false;

    @CommandLine.Option(names = {"--draw-statements-with-relevant-methods"},
            description = "Not only draw the relevant methods contained inside a statement, but also the " +
                    " statement itself, placed after all the relevant methods.")
    boolean drawStatementsWithRelevantMethods = false;

    public static void main(String []args) throws IOException, NotFoundException {

        final Main app = CommandLine.populateCommand(new Main(), args);
        app.call();
    }

    @Override
    public Void call() throws IOException, NotFoundException {
        final String loggerLevel = System.getProperty("org.slf4j.simpleLogger.defaultLogLevel");
        if(loggerLevel == null) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "error");
        }
        LOGGER = LoggerFactory.getLogger(Main.class);

        LOGGER.trace("Logger level = {}", loggerLevel);
        SourceAndJarAnalyzer analyzer = new SourceAndJarAnalyzer(filesPaths,
                DecompilerEnum.fromStringOrDefault(decompilerName), analyzeOnlySources);

        LOGGER.trace("drawLineNumbers = {}", drawLineNumbers);

        DrawerUtil.setDrawLineNumbers(drawLineNumbers);
        DrawerUtil.setDrawBoxAroundSubFlows(!noDrawBoxAroundSubflow);
        DrawerUtil.setDrawArrows(!noArrows);
        DrawerUtil.setPathToSrc(filesPaths);
        DrawerUtil.setDrawReturn(drawReturn);
        DrawerUtil.setDrawThrow(!noDrawThrow);
        DrawerUtil.setDrawBreakContinue(!noBreakContinue);
        DrawerUtil.setDrawStatementsWithRelevantMethods(drawStatementsWithRelevantMethods);

        DrawerUtil.drawAllStartableClasses(analyzer, outputPath);
        return null;
    }
}
