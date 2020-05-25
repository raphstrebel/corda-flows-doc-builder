package com.github.lucacampanella.callgraphflows.staticanalyzer;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import com.github.lucacampanella.callgraphflows.staticanalyzer.instructions.StatementInterface;
import com.github.lucacampanella.callgraphflows.staticanalyzer.instructions.StatementWithCompanionInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CombinationsHolder {

    private static class LockingBranch extends Branch {
        private enum LockStatus {
            NO_LOCK,
            LOOP_BREAK_LOCKED, //break
            LOOP_CONTINUE_LOCKED, //continue
            METHOD_LOCKED //return or throw
        }

        private LockStatus lockStatus = LockStatus.NO_LOCK;

        public LockingBranch() {
            super();
        }

        public LockingBranch(LockingBranch toCopy) {
            this.statements = new ArrayList<>(toCopy.getStatements());
            this.lockStatus = toCopy.lockStatus;
        }

        public LockingBranch(StatementInterface singleInstr) {
            super(singleInstr);
            setLockStatusIfLockingInstr(singleInstr);
        }

        @Override
        public void add(StatementInterface instr) {
            if(instr != null && !isLocked()) { //add only if branch is not locked
                statements.add(instr);
                setLockStatusIfLockingInstr(instr);
            }
        }

        public void add(LockingBranch branch) {
            if(!isLocked()) {
                for (StatementInterface stmt : branch) {
                    add(stmt);
                }
                lockStatus = branch.lockStatus;
            }
        }

        private void setLockStatusIfLockingInstr(StatementInterface instr) {
            if(instr.isBreakLoopFlowBreak()) {
                setBreakLoopLock();
            }
            else if(instr.isContinueLoopFlowBreak()) {
                setContinueLoopLock();
            }
            else if(instr.isMethodFlowBreak()) {
                setMethodLock();
            }
        }

        public boolean isLocked() {
            return lockStatus != LockStatus.NO_LOCK;
        }

        public void removeContinuekLoopLock() {
            if(lockStatus == LockStatus.LOOP_CONTINUE_LOCKED) {
                lockStatus = LockStatus.NO_LOCK;
            }
        }

        public void setContinueLoopLock() {
            if(!isLocked())
                lockStatus = LockStatus.LOOP_CONTINUE_LOCKED;
        }

        public void removeBreakLoopLock() {
            if(lockStatus == LockStatus.LOOP_BREAK_LOCKED) {
                lockStatus = LockStatus.NO_LOCK;
            }
        }

        public void setBreakLoopLock() {
            if(!isLocked())
                lockStatus = LockStatus.LOOP_BREAK_LOCKED;
        }

        public void removeAnyLoopLock() {
            removeContinuekLoopLock();
            removeBreakLoopLock();
        }

        public void removeAnyLock() {
            lockStatus = LockStatus.NO_LOCK;
        }

        public void setMethodLock() {
            lockStatus = LockStatus.METHOD_LOCKED;
        }

        public boolean containsSameStatementsAndLockStatusAs(LockingBranch otherBranch) {
            return lockStatus == otherBranch.lockStatus && super.containsSameStatementsAs(otherBranch);
        }
    }
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CombinationsHolder.class);
    
    private List<LockingBranch> allCombinations = new LinkedList<>();

    public CombinationsHolder(boolean addEmptyCombination) {
        if(addEmptyCombination) {
            allCombinations.add(new LockingBranch());
        }
    }

    public static CombinationsHolder fromOtherCombination(CombinationsHolder toBeCopied) {
        CombinationsHolder res = new CombinationsHolder(false);
        for(LockingBranch comb : toBeCopied.allCombinations) {
            res.allCombinations.add(new LockingBranch(comb));
        }
        return res;
    }

    public static CombinationsHolder fromSingleStatement(StatementInterface singleStatement) {
        CombinationsHolder res = new CombinationsHolder(false);
        res.addCombination(new LockingBranch(singleStatement));
        return res;
    }

    private void addCombination(LockingBranch comb) {
        allCombinations.add(comb);
    }

    public void appendToAllCombinations(StatementInterface statement) {
        allCombinations.forEach(branch -> branch.add(statement));
    }

    public void appendToAllCombinations(Branch statements) {
        allCombinations.forEach(branch -> branch.add(statements));
    }

    public void combineWithBranch(Branch branch) {
        combineWith(fromBranch(branch));
    }

    public void mergeWith(CombinationsHolder otherHolder) {
        for(LockingBranch comb : otherHolder.allCombinations) {
            allCombinations.add(new LockingBranch(comb));
        }
    }

    public void combineWith(CombinationsHolder otherHolder) {
        if(allCombinations.isEmpty()) {
            for(LockingBranch comb : otherHolder.allCombinations) {
                allCombinations.add(new LockingBranch(comb));
            }
        }
        if(otherHolder.allCombinations.size() == 1) { //more efficient way if only one branch on other side
            otherHolder.allCombinations.get(0).forEach(this::appendToAllCombinations);
        }
        else if(!otherHolder.allCombinations.isEmpty()){
            List<LockingBranch> newAllCombinations = new LinkedList<>();
            for (LockingBranch currBranch : allCombinations) {
                if(!currBranch.isLocked()) {
                    for (LockingBranch newBranch : otherHolder.allCombinations) {
                        LockingBranch bothTogether = new LockingBranch(currBranch);
                        bothTogether.add(newBranch);
                        newAllCombinations.add(bothTogether);
                    }
                }
            }
            allCombinations = newAllCombinations;
        }
    }

    //starting from a desugared branch
    public static CombinationsHolder fromBranch(Branch instructions) {
        CombinationsHolder holder = new CombinationsHolder(true);

        for(StatementInterface instr : instructions) {
            if(instr.isContinueLoopFlowBreak()) {
                holder.setAllContinueLoopLocks();
                break;
            }
            if(instr.isBreakLoopFlowBreak()) {
                holder.setAllBreakLoopLocks();
                break;
            }
            if(instr.isMethodFlowBreak()) {
                holder.setAllMethodLocks();
                break;
            }
            holder.combineWith(instr.getResultingCombinations());
        }

        return holder;
    }

    public boolean isEmpty() {
        return allCombinations.isEmpty();
    }

    public boolean checkIfMatchesAndDraw(CombinationsHolder otherCombinationsHolder) {
        boolean foundOneMatch = false;
        for(Branch combLeft : this.allCombinations) {
            for(Branch combRight : otherCombinationsHolder.allCombinations) {
                final List<MatchingStatements> matchingStatements = twoCombinationsMatch(combLeft, combRight);
                if(matchingStatements != null) {
                    foundOneMatch = true;
                    matchingStatements.forEach(MatchingStatements::createGraphLink);
                }
            }
        }
        return foundOneMatch;
    }

    public void removeAllContinueLoopLocks() {
        allCombinations.forEach(LockingBranch::removeContinuekLoopLock);
    }

    public void removeAllLoopLocks() {
        allCombinations.forEach(LockingBranch::removeAnyLoopLock);
        filterOutDuplicates();
    }

    public void removeAllLocks() {
        allCombinations.forEach(LockingBranch::removeAnyLock);
        filterOutDuplicates();
    }

    public void setAllBreakLoopLocks() {
        allCombinations.forEach(LockingBranch::setBreakLoopLock);
    }

    public void setAllContinueLoopLocks() {
        allCombinations.forEach(LockingBranch::setContinueLoopLock);
    }

    public void setAllMethodLocks() {
        allCombinations.forEach(LockingBranch::setMethodLock);
    }

    private static class MatchingStatements {
        StatementWithCompanionInterface leftStatement;
        StatementWithCompanionInterface rightStatement;

        public MatchingStatements(StatementWithCompanionInterface leftStatement, StatementWithCompanionInterface rightStatement) {
            this.leftStatement = leftStatement;
            this.rightStatement = rightStatement;
        }

        public void createGraphLink() {
            leftStatement.createGraphLink(rightStatement);
        }
    }

    /**
     * Checks if two branches have a matching combination and returns a list of all the
     * matching statements if they match, an empty list if there is nothing to match (still a valid
     * protocol) or null if the two combinations don't match
     * @param combLeft initiating branch
     * @param combRight initiated branch
     * @return a list of all the
     *       matching statements if they match, an empty list if there is nothing to match (still a valid
     *       protocol) or null if the two combinations don't match
     */
    private static List<MatchingStatements> twoCombinationsMatch(Branch combLeft,
                                                Branch combRight) {
        Deque<StatementWithCompanionInterface> initiatingQueue =
                new LinkedList<>(combLeft.getOnlyStatementWithCompanionStatements()); //they should all be already fo this type
        Deque<StatementWithCompanionInterface> initiatedQueue =
                new LinkedList<>(combRight.getOnlyStatementWithCompanionStatements());

        List<MatchingStatements> matchingStatements = new ArrayList<>();
        int i = 0;

        while(!initiatingQueue.isEmpty() || !initiatedQueue.isEmpty()) {

            StatementWithCompanionInterface instrLeft = initiatingQueue.peek();
            StatementWithCompanionInterface instrRight = initiatedQueue.peek();
            if (instrLeft == null && instrRight == null) {
                return matchingStatements;
            }
            if(instrLeft == null || instrRight == null) {
                return null; //one fot the two queues still has elements, while the other doesn't
            }

            LOGGER.trace("\n Round {}", i++);
            LOGGER.trace("{}", instrLeft);
            LOGGER.trace("{}", instrRight);

            if(!instrLeft.acceptCompanion(instrRight)) {
                //LOGGER.info("error in this flow logic!");
                return null;
            }
            else {
                matchingStatements.add(new MatchingStatements(instrLeft, instrRight));
            }

            if(instrLeft.isConsumedForCompanionAnalysis()) {
                initiatingQueue.remove(); //we remove the statement of the queue
            }

            if(instrRight.isConsumedForCompanionAnalysis()) {
                initiatedQueue.remove(); //we remove the statement of the queue
            }
        }

        return matchingStatements;
    }

    public List<LockingBranch> getAllCombinations() {
        return allCombinations;
    }

    /**
     * Removes all the combinations that are a duplicate, expensive operation, use with care
     */
    public void filterOutDuplicates() {
        List<LockingBranch> newCombinations = new LinkedList<>();
        for(LockingBranch comb : allCombinations) {
            boolean alreadyAdded = false;
            for(LockingBranch newComb : newCombinations) {
                if(newComb.containsSameStatementsAndLockStatusAs(comb)) {
                    alreadyAdded = true;
                    break;
                }
            }
            if(!alreadyAdded) {
                newCombinations.add(comb);
            }
        }
        allCombinations = newCombinations;
    }
}
