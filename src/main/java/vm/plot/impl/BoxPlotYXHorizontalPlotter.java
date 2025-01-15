/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.plot.impl;

import java.util.List;
import java.util.Map;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;

/**
 *
 * @author au734419
 */
public class BoxPlotYXHorizontalPlotter extends BoxPlotXYPlotter {

    public BoxPlotYXHorizontalPlotter() {
        super(true);
    }

    @Override
    public JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, String[] tracesNames, COLOUR_NAMES[] tracesColours, Object[] yValues, List<Float>[][] valuesYX) {
        return super.createPlot(mainTitle, xAxisLabel, yAxisLabel, tracesNames, tracesColours, yValues, valuesYX);
    }

    @Override
    protected int getNumberOrderOfShownXLabel(int numberOfXLabels) {
        int preserveEachTh = 1;
        int countShown = numberOfXLabels;
        while (countShown > Y_TICKS_IMPLICIT_NUMBER) {
            preserveEachTh++;
            countShown = (int) Math.ceil(numberOfXLabels / ((float) preserveEachTh));
        }
        return preserveEachTh;
    }

    @Override
    public JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, String traceName, COLOUR_NAMES traceColour, Map<Float, List<Float>> yToXListsValies) {
        return super.createPlot(mainTitle, xAxisLabel, yAxisLabel, traceName, traceColour, yToXListsValies);
    }

    @Override
    protected void setRotationOfXAxisCategoriesFont(CategoryAxis xAxis, Object[] groupsNames, int tracesPerGroup) {
        // this is correct
    }

}
