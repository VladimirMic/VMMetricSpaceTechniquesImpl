/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.plot.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.SortedMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import vm.datatools.DataTypeConvertor;
import vm.mathtools.Tools;

/**
 *
 * @author au734419
 */
public class BarsPlotter extends LinesPlotter {

    public BarsPlotter() {
        super(true);
    }

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

    public static SortedMap<Float, Float> createHistogramOfValuesWithPlot(List<Float> values, boolean absoluteValues, boolean logYScale, String xAxisLabel, String filePath, boolean storeAlsoPNG, boolean printLogMinMax) {
        String suf = logYScale ? "_log" : "";
        BarsPlotter plotter = new BarsPlotter();
        plotter.setLogY(logYScale);
        plotter.setIncludeZeroForXAxis(false);
        PrintStream err = System.err;
        if (printLogMinMax) {
            try {
                File folder = new File(filePath);
                folder = folder.getParentFile();
                folder = new File(folder, "log");
                folder.mkdirs();
                System.setErr(new PrintStream(new FileOutputStream(new File(folder, "log.csv"), true)));
                System.err.print(xAxisLabel + ";");
            } catch (FileNotFoundException ex) {
                Logger.getLogger(BarsPlotter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        SortedMap<Float, Float> histogram = Tools.createHistogramOfValues(values, absoluteValues, printLogMinMax);
        float step = vm.mathtools.Tools.getStepOfAlreadyMadeHistogram(histogram);
        String xAxisName = ", bar width: " + step;
        String yAxisName = absoluteValues ? "Count" : "Relative count";
        JFreeChart histogramPlot = plotter.createHistogramPlot(null, xAxisLabel + xAxisName, yAxisName, null, histogram);
        File f = new File(filePath + suf);
        f.getParentFile().mkdirs();
        plotter.storePlotPDF(f.getAbsolutePath(), histogramPlot);
        if (storeAlsoPNG) {
            plotter.storePlotPNG(f.getAbsolutePath(), histogramPlot);
        }
        System.setErr(err);
        return histogram;
    }

}
