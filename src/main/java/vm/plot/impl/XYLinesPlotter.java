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
import java.util.Map;
import java.util.TreeSet;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import vm.datatools.DataTypeConvertor;
import vm.datatools.Tools;
import vm.plot.AbstractPlotter;

/**
 *
 * @author au734419
 */
public class XYLinesPlotter extends AbstractPlotter {

    @Override
    public JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, Object... data) {
        Object[] tracesNames = new Object[]{""};
        if (data[0] != null) {
            if (data[0] instanceof Object[]) {
                tracesNames = (Object[]) data[0];
            } else {
                tracesNames = (Object[]) DataTypeConvertor.objectToSingularArray(data[0]);
            }
        }
        COLOUR_NAMES[] tracesColours = null;
        if (data[1] != null) {
            if (data[1] instanceof COLOUR_NAMES[]) {
                tracesColours = (COLOUR_NAMES[]) data[1];
            } else {
                tracesColours = (COLOUR_NAMES[]) DataTypeConvertor.objectToSingularArray(data[1]);
            }
        }
        float[][] tracesXValues;
        if (data[2] instanceof float[][]) {
            tracesXValues = (float[][]) data[2];
        } else {
            tracesXValues = (float[][]) DataTypeConvertor.objectToSingularArray(data[2]);
        }
        float[][] tracesYValues;
        if (data[3] instanceof float[][]) {
            tracesYValues = (float[][]) data[3];
        } else {
            tracesYValues = (float[][]) DataTypeConvertor.objectToSingularArray(data[3]);
        }
        return createPlot(mainTitle, xAxisLabel, yAxisLabel, tracesNames, tracesColours, tracesXValues, tracesYValues);
    }

    public JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, Object[] tracesNames, COLOUR_NAMES[] tracesColours, float[][] tracesXValues, float[][] tracesYValues) {
        XYSeries[] traces = transformCoordinatesIntoTraces(tracesNames, tracesXValues, tracesYValues);
        XYSeriesCollection dataset = new XYSeriesCollection();
        for (XYSeries trace : traces) {
            dataset.addSeries(trace);
        }
        JFreeChart chart = ChartFactory.createXYLineChart(mainTitle, xAxisLabel, yAxisLabel, dataset);
        if (logY) {
            setMinAndMaxYValues(tracesYValues);
        }
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
        if (logY) {
            LogAxis yAxis = new LogAxis();
            setLabelsOfAxis(yAxis);
            yAxis.setAutoRange(true);
            yAxis.setSmallestValue(minMaxY[0]);
            plot.setRangeAxis(yAxis);
        } else {
            NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
            setLabelsOfAxis(yAxis);
            setTicksOfYNumericAxis(yAxis);
        }
        //legend        
        setLegendFont(chart.getLegend());
        if (traces.length == 1) {
            String traceName = traces[0].getKey().toString().toLowerCase();
            if (chart.getLegend() != null && (traceName.equals(yAxisLabel.toLowerCase()) || traceName.equals("") || traceName.equals(xAxisLabel.toLowerCase()))) {
                chart.removeLegend();
            }
        }

        // set traces strokes
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        AffineTransform resize = new AffineTransform();
        resize.scale(1000, 1000);
        if (traces.length == 1) {
            renderer.setSeriesStroke(0, new BasicStroke(SERIES_STROKE));
            renderer.setSeriesShapesVisible(0, true);
            Color color = tracesColours == null ? BOX_BLACK : getColor(tracesColours[0], false);
            renderer.setSeriesPaint(0, color);
        } else {
            for (int i = 0; i < traces.length; i++) {
                renderer.setSeriesStroke(i, new BasicStroke(SERIES_STROKE));
                renderer.setSeriesShapesVisible(i, true);
                Color color = tracesColours == null ? COLOURS[i % COLOURS.length] : getColor(tracesColours[i], false);
                renderer.setSeriesPaint(i, color);
            }
        }
        plot.setBackgroundAlpha(0);

        return chart;
    }

    @Override
    public String getSimpleName() {
        return "PlotXYLines";
    }

}
