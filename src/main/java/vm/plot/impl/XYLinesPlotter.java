/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.plot.impl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import vm.datatools.Tools;
import vm.plot.AbstractPlotter;

/**
 *
 * @author au734419
 */
public class XYLinesPlotter extends AbstractPlotter {

    @Override
    public JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, Object[] tracesNames, COLOUR_NAMES[] tracesColours, float[][] tracesXValues, float[][] tracesYValues) {
        XYSeries[] traces = transformCoordinatesIntoTraces(tracesNames, tracesXValues, tracesYValues);
        XYSeriesCollection dataset = new XYSeriesCollection();
        for (XYSeries trace : traces) {
            dataset.addSeries(trace);
        }
        JFreeChart chart = ChartFactory.createXYLineChart(mainTitle, xAxisLabel, yAxisLabel, dataset);
        return setAppearence(chart, traces, tracesColours, xAxisLabel, yAxisLabel);
    }

    protected XYSeries[] transformCoordinatesIntoTraces(Object[] tracesNames, float[][] tracesXValues, float[][] tracesYValues) {
        if (tracesNames.length != tracesXValues.length || tracesNames.length != tracesYValues.length) {
            throw new IllegalArgumentException("Inconsistent number of traces in data. Names count: " + tracesNames.length + ", x count: " + tracesXValues.length + ", y count: " + tracesYValues.length);
        }
        XYSeries[] ret = new XYSeries[tracesNames.length];
        for (int i = 0; i < tracesNames.length; i++) {
            ret[i] = new XYSeries(tracesNames[i].toString());
            if (tracesXValues[i].length != tracesYValues[i].length) {
                throw new IllegalArgumentException("Inconsistent number of point in x and y coordinates. Trace: " + i + ", X coords: " + tracesXValues[i].length + ", y coords: " + tracesYValues[i].length);
            }
            int[] idxs = permutationOfIndexesToMakeXIncreasing(tracesXValues[i]);
            for (int idx : idxs) {
                ret[i].add(tracesXValues[i][idx], tracesYValues[i][idx]);
            }
        }
        return ret;
    }

    private int[] permutationOfIndexesToMakeXIncreasing(float[] traceXValues) {
        TreeSet<AbstractMap.Entry<Integer, Float>> set = new TreeSet<>(new Tools.MapByFloatValueComparator<>());
        for (int i = 0; i < traceXValues.length; i++) {
            AbstractMap.Entry<Integer, Float> entry = new AbstractMap.SimpleEntry<>(i, traceXValues[i]);
            set.add(entry);
        }
        int[] ret = new int[traceXValues.length];
        Iterator<Map.Entry<Integer, Float>> it = set.iterator();
        for (int i = 0; i < ret.length; i++) {
            ret[i] = it.next().getKey();
        }
        return ret;
    }

    private JFreeChart setAppearence(JFreeChart chart, XYSeries[] traces, COLOUR_NAMES[] tracesColours, String xAxisLabel, String yAxisLabel) {
        XYPlot plot = (XYPlot) chart.getPlot();
        // chart colours
        setChartColor(chart, plot);

        // x axis settings
        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        setLabelsOfAxis(xAxis);
        xAxis.setUpperMargin(0.15);
        setTicksOfXNumericAxis(xAxis);

        // y axis settings
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        setLabelsOfAxis(yAxis);
        setTicksOfYNumericAxis(yAxis);

        //legend        
        setLegendFont(chart.getLegend());
        if (traces.length == 1) {
            String traceName = traces[0].getKey().toString().toLowerCase();
            if (chart.getLegend() != null && (traceName.equals(yAxisLabel.toLowerCase()) || traceName.equals(xAxisLabel.toLowerCase()))) {
                chart.removeLegend();
            }
        }

        // set traces strokes
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        AffineTransform resize = new AffineTransform();
        resize.scale(1000, 1000);
        for (int i = 0; i < traces.length; i++) {
            renderer.setSeriesStroke(i, new BasicStroke(SERIES_STROKE));
            renderer.setSeriesShapesVisible(i, true);
            Color color = tracesColours == null ? COLOURS[i % COLOURS.length] : getColor(tracesColours[i], false);
            renderer.setSeriesPaint(i, color);
        }
        plot.setBackgroundAlpha(0);

        return chart;
    }

    @Override
    @Deprecated
    public JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, String[] tracesNames, COLOUR_NAMES[] tracesColours, Object[] groupsNames, List<Float>[][] values) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public String getSimpleName() {
        return "PlotXYLines";
    }

}
