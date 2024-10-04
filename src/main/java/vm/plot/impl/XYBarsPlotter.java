/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.plot.impl;

import java.util.SortedMap;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import vm.datatools.DataTypeConvertor;

/**
 *
 * @author au734419
 */
public class XYBarsPlotter extends XYLinesPlotter {

    @Override
    public JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, Object[] tracesNames, COLOUR_NAMES[] tracesColours, float[][] tracesXValues, float[][] tracesYValues) {
        XYSeries[] traces = transformCoordinatesIntoTraces(tracesNames, tracesXValues, tracesYValues);
        XYSeriesCollection dataset = new XYSeriesCollection();
        for (XYSeries trace : traces) {
            dataset.addSeries(trace);
        }
        JFreeChart chart = ChartFactory.createXYBarChart(mainTitle, xAxisLabel, false, yAxisLabel, dataset);
        if (logY) {
            setMinAndMaxYValues(tracesYValues);
        }
        return setAppearence(chart, traces, tracesColours, xAxisLabel, yAxisLabel);
    }

    public JFreeChart createHistogramPlot(String mainTitle, String xAxisLabel, String yAxisLabel, COLOUR_NAMES traceColour, SortedMap<Float, Float> histogram) {
        if (traceColour == null) {
            traceColour = COLOUR_NAMES.C1_BLUE;
        }
        Object[] xValues = histogram.keySet().toArray();
        Object[] yValues = histogram.values().toArray();
        float[] xFloats = DataTypeConvertor.objectsToPrimitiveFloats(xValues);
        float[] yFloats = DataTypeConvertor.objectsToPrimitiveFloats(yValues);
        float[][] xTracesValues = DataTypeConvertor.objectToSingularArray(xFloats);
        float[][] yTracesValues = DataTypeConvertor.objectToSingularArray(yFloats);
        COLOUR_NAMES[] colours = DataTypeConvertor.objectToSingularArray(traceColour);
        return createPlot(mainTitle, xAxisLabel, yAxisLabel, mainTitle, colours, xTracesValues, yTracesValues);
    }

}
