/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.plot.impl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Paint;
import java.awt.geom.AffineTransform;
import java.util.AbstractMap;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.GradientPaintTransformType;
import org.jfree.chart.ui.StandardGradientPaintTransformer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import vm.datatools.DataTypeConvertor;
import vm.datatools.Tools;
import vm.plot.AbstractPlotter;

/**
 *
 * @author au734419
 */
public class LinesPlotter extends AbstractPlotter {

    private final boolean linesVisible;
    private boolean isTimeSeries;

    public LinesPlotter() {
        this(true);
    }

    public LinesPlotter(boolean linesVisible) {
        this.linesVisible = linesVisible;
    }

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
            isTimeSeries = false;
        } else if (data[2] instanceof Date[][]) {
            long[][] tmp = DataTypeConvertor.datesArrayToLongs((Date[][]) data[2]);
            tracesXValues = DataTypeConvertor.longsArrayToFloats(tmp);
            isTimeSeries = true;
        } else if (data[2] instanceof Date[]) {
            long[] tmp = DataTypeConvertor.datesArrayToLongs((Date[]) data[2]);
            float[] tmp2 = DataTypeConvertor.longsArrayToFloats(tmp);
            tracesXValues = DataTypeConvertor.objectToSingularArray(tmp2);
            isTimeSeries = true;
        } else {
            tracesXValues = (float[][]) DataTypeConvertor.objectToSingularArray(data[2]);
            isTimeSeries = false;
        }
        float[][] tracesYValues;
        if (data[3] instanceof float[][]) {
            tracesYValues = (float[][]) data[3];
        } else {
            tracesYValues = (float[][]) DataTypeConvertor.objectToSingularArray(data[3]);
        }
        return createPlot(mainTitle, xAxisLabel, yAxisLabel, tracesNames, tracesColours, tracesXValues, tracesYValues);
    }

    public JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, COLOUR_NAMES traceColour, float[] tracesXValues, float[] tracesYValues) {
        return createPlot(mainTitle, xAxisLabel, yAxisLabel, null, traceColour, tracesXValues, tracesYValues);
    }

    protected JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, Object[] tracesNames, COLOUR_NAMES[] tracesColours, float[][] tracesXValues, float[][] tracesYValues) {
        XYSeries[] traces = transformCoordinatesIntoTraces(tracesNames, tracesXValues, tracesYValues);
        XYSeriesCollection dataset = new XYSeriesCollection();
        for (XYSeries trace : traces) {
            dataset.addSeries(trace);
        }
        JFreeChart chart;
        if (isTimeSeries) {
            chart = ChartFactory.createTimeSeriesChart(mainTitle, xAxisLabel, yAxisLabel, dataset);
        } else {
            chart = ChartFactory.createXYLineChart(mainTitle, xAxisLabel, yAxisLabel, dataset);
        }
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
                ret[i].add(DataTypeConvertor.floatToPreciseDouble(tracesXValues[i][idx]), DataTypeConvertor.floatToPreciseDouble(tracesYValues[i][idx]));
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

    protected JFreeChart setAppearence(JFreeChart chart, XYSeries[] traces, COLOUR_NAMES[] tracesColours, String xAxisLabel, String yAxisLabel) {
        XYPlot plot = (XYPlot) chart.getPlot();
        // chart colours
        setChartColor(chart, plot);

        // x axis settings
        ValueAxis xAxis = plot.getDomainAxis();
        setTicksOfXNumericAxis(xAxis);
        xAxis.setUpperMargin(0.1);
        setLabelsOfAxis(xAxis);

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
            boolean onlyIntegerYValues = isOnlyIntegerYValues(traces);
            setTicksOfYNumericAxis(yAxis, onlyIntegerYValues);
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
        XYItemRenderer renderer = plot.getRenderer();
        XYLineAndShapeRenderer lineAndShapeRenderer = null;
        XYBarRenderer barRenderer = null;
        if (renderer instanceof XYLineAndShapeRenderer) {
            lineAndShapeRenderer = (XYLineAndShapeRenderer) renderer;
        }
        if (renderer instanceof XYBarRenderer) {
            barRenderer = new MyBarRenderer();
            plot.setRenderer(barRenderer);
            renderer = barRenderer;
        }
        AffineTransform resize = new AffineTransform();
        resize.scale(1000, 1000);
        if (barRenderer != null) {
            barRenderer.setDrawBarOutline(true); // border of the columns
            int barCount = traces[0].getItemCount();
            Logger.getLogger(LinesPlotter.class.getName()).log(Level.INFO, "Creating bars-plot with X axis named {0} and {1} bars", new Object[]{xAxisLabel, barCount});
            barRenderer.setMargin(0);
            barRenderer.setGradientPaintTransformer(new StandardGradientPaintTransformer(GradientPaintTransformType.HORIZONTAL));
            barRenderer.setBarPainter(new MyBarPainter(0, 0, 0));
        }
        for (int i = 0; i < traces.length; i++) {
            if (lineAndShapeRenderer != null) {
                if (!linesVisible) {
                    lineAndShapeRenderer.setSeriesLinesVisible(i, false);
                }
                lineAndShapeRenderer.setSeriesShapesVisible(i, true);
            }
            Color darkColor = tracesColours == null ? COLOURS[i % COLOURS.length] : getColor(tracesColours[i], false);
            Color lightColor = tracesColours == null ? LIGHT_COLOURS[i % LIGHT_COLOURS.length] : getColor(tracesColours[i], true);
            if (traces.length == 1 && barRenderer == null && tracesColours == null) {
                darkColor = BOX_BLACK;
                lightColor = LIGHT_BOX_BLACK;
            }
            if (renderer != null) {
                renderer.setSeriesStroke(i, new BasicStroke(SERIES_STROKE));
                if (barRenderer == null) {
                    renderer.setSeriesPaint(i, darkColor);
                } else {
                    Paint gradientPaint = new GradientPaint(0.0f, 0.0f, lightColor, Float.MAX_VALUE, Float.MAX_VALUE, lightColor);
                    barRenderer.setSeriesPaint(i, gradientPaint);
                    barRenderer.setSeriesOutlinePaint(i, darkColor);
                }
            }

        }
        plot.setBackgroundAlpha(0);
        plot.setRenderer(renderer);
        return chart;
    }

    @Override
    public String getSimpleName() {
        return "PlotXYLines";
    }

    private boolean isOnlyIntegerYValues(XYSeries[] traces) {
        for (XYSeries trace : traces) {
            int itemCount = trace.getItemCount();
            for (int i = 0; i < itemCount; i++) {
                Number y = trace.getY(i);
                if (y.floatValue() != y.intValue()) {
                    return false;
                }
            }
        }
        return true;
    }
}
