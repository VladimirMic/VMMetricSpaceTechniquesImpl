/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.plot.impl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import vm.plot.AbstractPlotter;

/**
 *
 * @author au734419
 */
public class BoxPlotPlotter extends AbstractPlotter {

    @Override
    public JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, String[] tracesNames, float[][] tracesXValues, float[][] tracesYValues) {
        DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();
        List[][] traces = null;//(List[]) series;
//        for (List trace : traces) {
//            dataset.add(trace);
//        }
        JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(mainTitle, xAxisLabel, yAxisLabel, dataset, true);
        if (traces.length == 1) {
            String traceName = tracesNames[0];
            if (chart.getLegend() != null && (traceName.equals(yAxisLabel.toLowerCase()) || traceName.equals(xAxisLabel.toLowerCase()))) {
                chart.removeLegend();
            }
        }
//        XYPlot plot = (XYPlot) chart.getPlot();
//        // chart colours
//        setChartColor(chart, plot);
//
//        // x axis settings
//        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
//        setLabelsOfAxis(xAxis);
//        xAxis.setUpperMargin(0.15);
//        setTicksOfXNumericAxis(xAxis);
//
//        // y axis settings
//        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
//        setLabelsOfAxis(yAxis);
//        setTicksOfYNumericAxis(yAxis);
//
//        //legend        
//        setLegendFont(chart.getLegend());
//
//        // set traces strokes
//        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
//        AffineTransform resize = new AffineTransform();
//        resize.scale(1000, 1000);
//        for (int i = 0; i < traces.length; i++) {
//            renderer.setSeriesStroke(i, new BasicStroke(SERIES_STROKE));
//            renderer.setSeriesShapesVisible(i, true);
//            renderer.setSeriesPaint(i, COLOURS[i % COLOURS.length]);
//        }
//        plot.setRangeGridlinePaint(Color.BLACK);
//        plot.setBackgroundAlpha(0);
//
        return chart;
    }
}
