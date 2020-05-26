package com.github.lucacampanella.callgraphflows.staticanalyzer.instructions;

import java.util.Optional;

import com.github.lucacampanella.callgraphflows.graphics.components2.GBaseSimpleComponent;
import com.github.lucacampanella.callgraphflows.staticanalyzer.AnalyzerWithModel;
import com.github.lucacampanella.callgraphflows.staticanalyzer.StaticAnalyzerUtils;
import com.github.lucacampanella.callgraphflows.staticanalyzer.matchers.MatcherHelper;
import com.github.lucacampanella.callgraphflows.utils.Utils;
import javassist.NotFoundException;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLambda;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.declaration.CtTypedElement;
import spoon.reflect.reference.CtTypeReference;

public class SendAndReceive extends InstructionStatement implements StatementWithCompanionInterface {

    String sentType;
    String receivedType;

    boolean isSentConsumed = false;

    protected SendAndReceive(CtStatement statement) {
        super(statement);
    }

    protected SendAndReceive() {
        super();
    }

    public static SendAndReceive fromCtStatement(CtStatement statement, AnalyzerWithModel analyzer) throws NotFoundException {
        SendAndReceive sendAndReceive = new SendAndReceive();
        sendAndReceive.line = statement.getPosition().getLine();
        sendAndReceive.internalMethodInvocations.add(StaticAnalyzerUtils.getAllRelevantMethodInvocations(statement,
                analyzer));

        CtInvocation invocation = (CtInvocation) MatcherHelper.getFirstMatchedExpression(statement,
                    "sendAndReceiveMatcher");
        if(invocation == null) {
            invocation = (CtInvocation) MatcherHelper.getFirstMatchedExpression(statement,
                    "sendAndReceiveWithBoolMatcher");
        }

            Object firstArgument = invocation.getArguments().get(0);

            //maybe there is a more rubust way to do this, for example with a while
            if(firstArgument instanceof CtFieldRead) {
                CtTypeAccess fieldRead = (CtTypeAccess) ((CtFieldRead) (firstArgument)).getTarget();
                sendAndReceive.receivedType = analyzer.getCurrClassCallStackHolder().resolveEventualGenerics(
                        fieldRead.getAccessedType())
                        .box().toString();
            }
            else if(firstArgument instanceof CtLambda) {
                invocation = (CtInvocation) invocation.getTarget();
                //receivedType = invocation.getArguments().get(0).getTarget().getAccessedType()
                sendAndReceive.receivedType = analyzer.getCurrClassCallStackHolder().resolveEventualGenerics(
                        (((CtTypeAccess) ((CtFieldRead) (invocation.getArguments().get(0))).getTarget()).getAccessedType()))
                        .box().toString();
            }

        final CtTypeReference secondArgument = ((CtTypedElement) invocation.getArguments().get(1)).getType();
        sendAndReceive.sentType = analyzer.getCurrClassCallStackHolder().resolveEventualGenerics(secondArgument)
                .box().toString();

        sendAndReceive.targetSessionName = Optional.ofNullable(invocation.getTarget().toString());

        sendAndReceive.buildGraphElem();

        return sendAndReceive;
    }

    @Override
    public boolean acceptCompanion(StatementWithCompanionInterface companion) {
        boolean accepted = false;

        if(!isSentConsumed) { // we treat it as a send
            accepted = Send.isAccepted(companion, accepted, sentType);
            isSentConsumed = true;
        }
        else {
            accepted = Receive.isAccepted(companion, accepted, receivedType);
            isSentConsumed = false;
        }

        return accepted;
    }

    @Override
    public void createGraphLink(StatementWithCompanionInterface companion) {
        final StatementWithCompanionInterface realCompanion = companion;
        if(!isSentConsumed) { // we treat it as a send
            if(realCompanion instanceof Receive) {
                graphElem.setBrotherSafely((GBaseSimpleComponent) realCompanion.getGraphElem());
            }
            else if(realCompanion instanceof SendAndReceive) {
                ((SendAndReceive) realCompanion).setSentConsumed(false); //we consumed the send state of SendAndReceive
                graphElem.addLink((GBaseSimpleComponent) realCompanion.getGraphElem());
            }
            isSentConsumed = true;
        }
        else { //we treat it as a receive
            if (realCompanion instanceof SendAndReceive) {
                ((SendAndReceive) realCompanion).setSentConsumed(true); //we consumed the send state of SendAndReceive
            }
            ((GBaseSimpleComponent) realCompanion.getGraphElem()).addLink(graphElem);
            isSentConsumed = false;
        }
    }

    public String getSentType() {
        return sentType;
    }

    public String getReceivedType() {
        return receivedType;
    }

    public boolean isSentConsumed() {
        return isSentConsumed;
    }

    public void setSentConsumed(boolean sentConsumed) {
        isSentConsumed = sentConsumed;
    }

    @Override
    public boolean hasSendOrReceiveAtThisLevel() {
        return true;
    }

    @Override
    public String addIconsToText(String displayText) {
        return getSYMBOL() + " " + displayText + " " + getSYMBOL();
    }


    public static String getSYMBOL() {
        return Send.getSYMBOL()+Receive.getSYMBOL();
    }

    @Override
    public String getStringDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(getSYMBOL());
        sb.append("sendAndReceive(");
        sb.append(Receive.getSYMBOL());
        sb.append(Utils.removePackageDescriptionIfWanted(receivedType));
        sb.append(", ");
        sb.append(Send.getSYMBOL());
        sb.append(Utils.removePackageDescriptionIfWanted(sentType));
        sb.append(")");
        sb.append(getSYMBOL());

        return sb.toString();
    }

    @Override
    protected void buildGraphElem() {
        super.buildGraphElem();
        graphElem.setDrawBox(true);
    }

    @Override
    public boolean isConsumedForCompanionAnalysis() {
        return !isSentConsumed();
    }
}
