package com.github.lucacampanella.callgraphflows.staticanalyzer.instructions;

import java.awt.*;

import com.github.lucacampanella.callgraphflows.graphics.components2.GBaseText;
import com.github.lucacampanella.callgraphflows.staticanalyzer.AnalyzerWithModel;
import com.github.lucacampanella.callgraphflows.staticanalyzer.StaticAnalyzerUtils;
import javassist.NotFoundException;
import spoon.reflect.code.CtStatement;

public class TransactionBuilder extends InstructionStatement {

    protected TransactionBuilder(CtStatement statement) {
        super(statement);
    }

    public static TransactionBuilder fromStatement(CtStatement statement, AnalyzerWithModel analyzer) throws NotFoundException {
        TransactionBuilder transactionBuilder = new TransactionBuilder(statement);
        transactionBuilder.internalMethodInvocations.add(
                StaticAnalyzerUtils.getAllRelevantMethodInvocations(statement, analyzer));

        return transactionBuilder;
    }

    @Override
    protected Color getTextColor() { return GBaseText.LESS_IMPORTANT_TEXT_COLOR; }

    @Override
    public boolean toBePainted() {
        return false;
    }
}
