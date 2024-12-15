/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.plot.impl;

import java.text.DateFormat;
import java.util.Date;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateTickUnitType;

/**
 *
 * @author au734419
 */
public class TimeSeriesLinesPlotter extends LinesPlotter {

    public TimeSeriesLinesPlotter(DateFormat dateFormat, DateTickUnitType timeTickType, int timeUnitInterval, boolean linesVisible) {
        super(linesVisible);
        this.dateFormat = dateFormat;
        this.timeTickType = timeTickType;
        this.timeUnitInterval = timeUnitInterval;
    }

    public JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, COLOUR_NAMES traceColour, Date[] tracesXValues, float[] tracesYValues) {
        return createPlot(mainTitle, xAxisLabel, yAxisLabel, null, traceColour, tracesXValues, tracesYValues);
    }

}
