package com.github.lucacampanella.callgraphflows.graphics.components2;

import com.github.lucacampanella.callgraphflows.graphics.utils.GUtils;
import com.github.lucacampanella.callgraphflows.staticanalyzer.AnalysisResult;
import com.github.lucacampanella.callgraphflows.staticanalyzer.ClassDescriptionContainer;
import org.jfree.graphics2d.svg.SVGGraphics2D;

import java.awt.*;

/**
 * A wrapper for {@link GTwoSidedContainer}, which also has titles
 */
public class GTwoSidedContainerWithTitles extends GBaseComponent {

    private static final int SPACE_BETWEEN_TITLE_AND_CONTENT = 15;

    private GTwoSidedContainer twoSidedContainer = new GTwoSidedContainer();

    private GBaseText mainSubFlowTitle = null;
    private GBaseText counterpartySubFlowTitle = null;

    @Override
    public int computeHeight(SVGGraphics2D g2) {
        return twoSidedContainer.getHeight(g2) + getTitleSectionHeight(g2);
    }

    @Override
    protected int computeWidth(SVGGraphics2D g2) {
        return twoSidedContainer.computeWidth(g2);
    }

    @Override
    public void drawBrothersAndLinks(SVGGraphics2D g2) {
        twoSidedContainer.drawBrothersAndLinks(g2);
    }

    @Override
    public String toString() {
        return twoSidedContainer.toString();
    }

    private static GBaseText getTitleBox(String title) {
        GBaseText titleBox = new GBaseText(title);
        titleBox.setDrawBox(true);
        titleBox.setBackgroundColor(GUtils.VERY_LIGHT_GRAY);
        return titleBox;
    }

    private int getTitleSectionHeight(SVGGraphics2D g2) {
        if(!twoSidedContainer.hasCounterpartySubFlow()) {
            return mainSubFlowTitle.getHeight(g2) + SPACE_BETWEEN_TITLE_AND_CONTENT;
        }
        else {
            int mainFlowSpaceNeeded = mainSubFlowTitle.getHeight(g2) + SPACE_BETWEEN_TITLE_AND_CONTENT
                    - twoSidedContainer.getMainSubFlow().getRealStart();
            int counterpartyFlowSpaceNeeded = counterpartySubFlowTitle.getHeight(g2) + SPACE_BETWEEN_TITLE_AND_CONTENT
                    - twoSidedContainer.getCounterpartySubFlow().getRealStart();

            return Math.max(mainFlowSpaceNeeded, counterpartyFlowSpaceNeeded);
        }
    }

    @Override
    public void draw(SVGGraphics2D g2, int x, int y) {
        mainSubFlowTitle.draw(g2, x, y);
        GUtils.drawLineWithOptions(g2, x + GSubFlowIndented.WIDTH/2, y + mainSubFlowTitle.getHeight(g2),
                x + GSubFlowIndented.WIDTH/2,
                y + mainSubFlowTitle.getHeight(g2) + twoSidedContainer.getMainSubFlow().getRectStartOffset(g2)
                        + SPACE_BETWEEN_TITLE_AND_CONTENT,
                Color.GRAY, GUtils.DASHED_STROKE);
        if(twoSidedContainer.hasCounterpartySubFlow()) {
            int counterpartyStartX = x + twoSidedContainer.getMainSubFlow().getWidth(g2)
                    + GTwoSidedContainer.SPACE_BETWEEN_FLOWS;
            counterpartySubFlowTitle.draw(g2, counterpartyStartX, y);
            GUtils.drawLineWithOptions(g2, counterpartyStartX + GSubFlowIndented.WIDTH/2,
                    y + counterpartySubFlowTitle.getHeight(g2),
                    counterpartyStartX + GSubFlowIndented.WIDTH/2,
                    y  + counterpartySubFlowTitle.getHeight(g2) + twoSidedContainer.getCounterpartyStartY() +
                            twoSidedContainer.getCounterpartySubFlow().getRectStartOffset(g2) + SPACE_BETWEEN_TITLE_AND_CONTENT,
                    Color.GRAY, GUtils.DASHED_STROKE);
        }
        twoSidedContainer.draw(g2, x, y + getTitleSectionHeight(g2), false);

        g2.drawLine(x, y + getTitleSectionHeight(g2),
                x + GSubFlowIndented.WIDTH/2,
                y + getTitleSectionHeight(g2));
    }

    public static GTwoSidedContainerWithTitles fromAnalysisResult(AnalysisResult analysisResult) {
        GTwoSidedContainerWithTitles twoSidedContainerWithTitles = new GTwoSidedContainerWithTitles();
        final ClassDescriptionContainer classDescription = analysisResult.getClassDescription();

        twoSidedContainerWithTitles.twoSidedContainer = analysisResult.getGraphicRepresentationNoTitles();

        twoSidedContainerWithTitles.mainSubFlowTitle = getTitleBox(classDescription.getSimpleName());

        final AnalysisResult initiatedClassResult = analysisResult.getCounterpartyClassResult();
        if(initiatedClassResult != null) {
            twoSidedContainerWithTitles.counterpartySubFlowTitle =
                    getTitleBox(initiatedClassResult.getClassDescription().getSimpleName());
        }

        return twoSidedContainerWithTitles;
    }

    /**
     * Sets the parent of the flow to null, so that the colors are stared from the first one
     * instead of from the one that was lastly used because this was already part of a bigger flow
     */
    public void setParentsForFileDrawing() {
        twoSidedContainer.setParent(null);
    }
}
