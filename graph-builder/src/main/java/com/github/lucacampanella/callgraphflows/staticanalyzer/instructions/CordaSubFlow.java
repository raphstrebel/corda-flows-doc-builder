package com.github.lucacampanella.callgraphflows.staticanalyzer.instructions;


import com.github.lucacampanella.callgraphflows.graphics.components2.GSubFlowIndented;
import com.github.lucacampanella.callgraphflows.utils.Utils;


public class CordaSubFlow extends SubFlowBase implements StatementWithCompanionInterface {

    GSubFlowIndented graphElem = new GSubFlowIndented();

    protected CordaSubFlow() {

    }

    @Override
    public boolean isCordaSubFlow() {
        return true;
    }

    @Override
    public GSubFlowIndented getGraphElem() {
        return toBePainted() ? graphElem : null;
    }

    @Override
    public boolean acceptCompanion(StatementWithCompanionInterface companion) {
        if(companion instanceof CordaSubFlow) {
            CordaSubFlow otherFlow = (CordaSubFlow) companion;

            Boolean isEqualInitFlow = false;

            try {
                isEqualInitFlow = isInitiatingFlow().equals(otherFlow.isInitiatingFlow());
            } catch (NullPointerException e) {}

            if (isEqualInitFlow) {
                return false; //they are both either initiating or initiated
            }



            CordaSubFlow initiatingFlow = this;
            CordaSubFlow initiatedFlow = this;

            try {
                initiatingFlow = this.isInitiatingFlow() ? this : otherFlow;
            } catch(NullPointerException e) {}

            try {
                initiatedFlow = otherFlow.isInitiatingFlow() ? this : otherFlow;
            } catch (NullPointerException e){}

            if(SubFlowBuilder.areMatchingSpecialCordaFlow(
                    initiatingFlow.getSubFlowType(), initiatedFlow.getSubFlowType())) {
                return true;
            }
        }
        return false;
    }

    protected void buildGraphElem() {
        graphElem.setEnteringArrowText(initiatingInstruction);

        if(returnType.isPresent() && !returnType.get().equals("java.lang.Void")) {
            graphElem.setExitingArrowText(Utils.removePackageDescriptionIfWanted(returnType.get()));
        }
    }

    @Override
    public void createGraphLink(StatementWithCompanionInterface companion) {
        if(companion instanceof CordaSubFlow) {
            if (isInitiatingFlow() != null && isInitiatingFlow()) {
                this.getInitiatingInstruction().setBrotherSafely(((CordaSubFlow) companion).getInitiatingInstruction());
            } else {
                ((CordaSubFlow) companion).getInitiatingInstruction().setBrotherSafely(this.getInitiatingInstruction());
            }
        }
    }

    @Override
    public String toString() {
        return "CordaSubFlow<<" + subFlowType.toString() + ">>";
    }
}





