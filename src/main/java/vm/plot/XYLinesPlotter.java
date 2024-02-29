/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.plot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.text.NumberFormat;
import java.util.Locale;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 *
 * @author au734419
 */
public class XYLinesPlotter extends AbstractPlotter {

    public static final Integer FONT_SIZE_AXIS_LABEL = 30;
    public static final Integer FONT_SIZE_AXIS_TICKS = 28;

    public static final Integer X_TICKS_IMPLICIT_NUMBER_FOR_SHORT_DESC = 15;
    public static final Integer X_TICKS_IMPLICIT_NUMBER_FOR_LONG_DESC = 8;

    public static final Integer Y_TICKS_IMPLICIT_NUMBER = 14;
    public static final Float SERIES_STROKE = 2f;
    public static final Float GRID_STROKE = 0.4f;

    @Override
    public JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, Double xAxisStep, Double yAxisStep, XYSeries... traces) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        for (XYSeries trace : traces) {
            dataset.addSeries(trace);
        }
        JFreeChart chart = ChartFactory.createXYLineChart(mainTitle, xAxisLabel, yAxisLabel, dataset);
        chart.setBackgroundPaint(Color.WHITE);
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundAlpha(0);
        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        xAxis.setUpperMargin(0.2);

        Font fontAxisTitle = new Font("Arial", Font.PLAIN, FONT_SIZE_AXIS_LABEL);
        yAxis.setLabelFont(fontAxisTitle);
        xAxis.setLabelFont(fontAxisTitle);
        chart.getLegend().setItemFont(fontAxisTitle);
        Font fontAxisMarkers = new Font("Arial", Font.PLAIN, FONT_SIZE_AXIS_TICKS);
        yAxis.setTickLabelFont(fontAxisMarkers);
        xAxis.setTickLabelFont(fontAxisMarkers);
        plot.setDomainGridlinePaint(Color.BLACK);
        plot.setDomainGridlineStroke(new BasicStroke(GRID_STROKE));
        plot.setRangeGridlineStroke(new BasicStroke(GRID_STROKE));

        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        for (int i = 0; i < traces.length; i++) {
            renderer.setSeriesStroke(i, new BasicStroke(SERIES_STROKE));
            renderer.setSeriesShapesVisible(i, Boolean.TRUE);
        }
        plot.setRangeGridlinePaint(Color.BLACK);

        // step of tick labels
        // y
        Double yStep = setAxisUnits(yAxisStep, yAxis, Y_TICKS_IMPLICIT_NUMBER);
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        NumberFormat nfBig = NumberFormat.getCompactNumberInstance(Locale.US, NumberFormat.Style.SHORT);
        nfBig.setMinimumFractionDigits(1);
        if (yStep >= 1000) {
            yAxis.setNumberFormatOverride(nfBig);
        } else {
            yAxis.setNumberFormatOverride(nf);
        }

        // x
        Double xStep = setAxisUnits(xAxisStep, xAxis, X_TICKS_IMPLICIT_NUMBER_FOR_SHORT_DESC);
        if (xStep >= 50 && xStep <= 1000) {
            xStep = setAxisUnits(xAxisStep, xAxis, X_TICKS_IMPLICIT_NUMBER_FOR_LONG_DESC);
            nf = nfBig;
        }
        if (xStep >= 1000) {
            nf = NumberFormat.getCompactNumberInstance(Locale.US, NumberFormat.Style.SHORT);
        }
        xAxis.setNumberFormatOverride(nf);
        return chart;
    }

}
