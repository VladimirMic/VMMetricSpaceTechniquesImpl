/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.plot.impl;

import java.text.DateFormat;
import java.util.Date;
import org.jfree.chart.JFreeChart;

/**
 *
 * @author au734419
 */
public class TimeSeriesLinesPlotter extends LinesPlotter {

    public TimeSeriesLinesPlotter(DateFormat dateFormat, boolean linesVisible) {
        super(linesVisible);
        this.dateFormat = dateFormat;
    }

    public JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, COLOUR_NAMES traceColour, Date[] tracesXValues, float[] tracesYValues) {
        return createPlot(mainTitle, xAxisLabel, yAxisLabel, null, traceColour, tracesXValues, tracesYValues);
    }

    @Override
    public void storePlotPDF(String path, JFreeChart plot) {
        super.storePlotPDF(path, plot);
    }

}
