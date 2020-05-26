package com.github.lucacampanella.callgraphflows;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import com.github.lucacampanella.callgraphflows.asciidoc.AsciiDocBuilder;
import com.github.lucacampanella.callgraphflows.asciidoc.AsciiDocIndexBuilder;
import com.github.lucacampanella.callgraphflows.graphics.components2.GGraphBuilder;
import com.github.lucacampanella.callgraphflows.graphics.components2.GInstruction;
import com.github.lucacampanella.callgraphflows.graphics.components2.GTwoSidedContainer;
import com.github.lucacampanella.callgraphflows.staticanalyzer.AnalysisErrorException;
import com.github.lucacampanella.callgraphflows.staticanalyzer.AnalysisResult;
import com.github.lucacampanella.callgraphflows.staticanalyzer.AnalyzerWithModel;
import com.github.lucacampanella.callgraphflows.staticanalyzer.ClassDescriptionContainer;
import com.github.lucacampanella.callgraphflows.staticanalyzer.instructions.CodeFlowBreak;
import com.github.lucacampanella.callgraphflows.staticanalyzer.instructions.StatementWithRelevantMethods;
import javassist.CtClass;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DrawerUtil {

    private DrawerUtil() {
        //private constuctor to hide public one
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DrawerUtil.class);

    public static final String DEFAULT_OUT_DIR = Paths.get("build", "graphs").toString();

    private static final String IMAGES_FOLDER_NAME = "images";

    public static void drawAllStartableClasses(AnalyzerWithModel analyzerWithModel, String outPath)
            throws IOException, NotFoundException {
        if(outPath == null) {
            outPath = DEFAULT_OUT_DIR;
        }

        AsciiDocIndexBuilder asciiDocIndexBuilder = new AsciiDocIndexBuilder(analyzerWithModel.getAnalysisName());

        final List<CtClass> startableByRPCClasses = analyzerWithModel.getClassesToBeAnalyzed();
        LOGGER.info("Found these classes annotated with @StartableByRPC: ");
        for(CtClass c: startableByRPCClasses) {
            LOGGER.info(c.getSimpleName());
        }
        Paths.get(outPath, IMAGES_FOLDER_NAME).toFile().mkdirs(); //create all directories necessary for the output
        for (CtClass klass : startableByRPCClasses) {
            //LOGGER.info("**** Analyzing class {} ", klass.getQualifiedName());
            drawFromClass(analyzerWithModel, klass, outPath);
            asciiDocIndexBuilder.addFile(klass.getName() + ".adoc");
        }
        asciiDocIndexBuilder.writeToFile(Paths.get(outPath, "index.adoc").toString());
    }

    public static void drawAllStartableClasses(AnalyzerWithModel analyzerWithModel) throws IOException, NotFoundException {
        drawAllStartableClasses(analyzerWithModel, DEFAULT_OUT_DIR);
    }

    public static void drawFromClass(AnalyzerWithModel analyzerWithModel, CtClass klass, String outPath) throws IOException {
        final AnalysisResult analysisResult;
        try {
            analysisResult = analyzerWithModel.analyzeFlowLogicClass(klass);
        } catch (AnalysisErrorException | NotFoundException e) {
            LOGGER.error("Couldn't analyze class {}, skipping this class", klass.getName(), e);
            return;
        }

        drawFromAnalysis(analysisResult, outPath);
    }

    public static void drawFromAnalysis(AnalysisResult analysisResult, String outPath) throws IOException {
        final ClassDescriptionContainer classDescription = analysisResult.getClassDescription();

        GGraphBuilder graphBuilder = GGraphBuilder.fromAnalysisResult(analysisResult);
        Paths.get(outPath, IMAGES_FOLDER_NAME).toFile().mkdirs();
        graphBuilder.drawToFile(Paths.get(outPath, IMAGES_FOLDER_NAME, classDescription.getFullyQualifiedName() + ".svg").toString());

        AsciiDocBuilder asciiDocBuilder = AsciiDocBuilder.fromAnalysisResult(analysisResult);
        asciiDocBuilder.writeToFile(Paths.get(outPath, classDescription.getFullyQualifiedName() + ".adoc").toString());
    }

    public static void setDrawLineNumbers(boolean drawLineNumbers) {
        GInstruction.setDrawLineNumbers(drawLineNumbers);
    }

    public static void setDrawBoxAroundSubFlows(boolean drawBox) {
        GTwoSidedContainer.setDrawBoxAround(drawBox);
    }

    public static void setDrawArrows(boolean drawArrows) {
        AnalyzerWithModel.setDrawArrows(drawArrows);
    }

    public static void setPathToSrc(String[] pathToSrc) {
        AnalyzerWithModel.setPathToSrc(pathToSrc);
    }

    public static void setDrawReturn(boolean drawReturn) {
        CodeFlowBreak.setDrawReturn(drawReturn);
    }

    public static void setDrawThrow(boolean drawThrow) {
        CodeFlowBreak.setDrawThrow(drawThrow);
    }

    public static void setDrawStatementsWithRelevantMethods(boolean drawStatementsWithRelevantMethods) {
        StatementWithRelevantMethods.setToBePainted(drawStatementsWithRelevantMethods);
    }

    public static void setDrawBreakContinue(boolean drawBreakContinue) {
        CodeFlowBreak.setDrawBreakContinue(drawBreakContinue);
    }
}
