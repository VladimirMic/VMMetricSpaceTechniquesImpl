/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.plot.impl;

import java.io.File;
import java.util.List;
import java.util.SortedMap;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import vm.datatools.DataTypeConvertor;
import vm.math.Tools;

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

    public static SortedMap<Float, Float> createHistogramOfValuesWithPlot(List<Float> values, boolean absoluteValues, boolean logYScale, String xAxisLabel, String filePath, boolean storeAlsoPNG) {
        String suf = logYScale ? "_log" : "";
        XYBarsPlotter plotter = new XYBarsPlotter();
        plotter.setLogY(logYScale);
        plotter.setIncludeZeroForXAxis(false);
        SortedMap<Float, Float> histogram = Tools.createHistogramOfValues(values, absoluteValues);
        float step = vm.math.Tools.getStepOfAlreadyMadeHistogram(histogram);
        String name = ", bar width: " + step;
        JFreeChart histogramPlot = plotter.createHistogramPlot(null, xAxisLabel + name, "Count", null, histogram);
        File f = new File(filePath + suf);
        plotter.storePlotPDF(f.getAbsolutePath(), histogramPlot);
        if (storeAlsoPNG) {
            plotter.storePlotPNG(f.getAbsolutePath(), histogramPlot);
        }
        return histogram;
    }

}
