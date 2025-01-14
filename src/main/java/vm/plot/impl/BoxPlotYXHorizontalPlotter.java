/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.plot.impl;

import java.util.List;
import java.util.Map;
import org.jfree.chart.JFreeChart;

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
    public JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, String traceName, COLOUR_NAMES traceColour, Map<Float, List<Float>> yToXListsValies) {
        return super.createPlot(mainTitle, xAxisLabel, yAxisLabel, traceName, traceColour, yToXListsValies);
    }
}
