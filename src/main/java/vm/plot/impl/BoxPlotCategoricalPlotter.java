/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.plot.impl;

import java.awt.BasicStroke;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.BoxAndWhiskerCalculator;
import org.jfree.data.statistics.BoxAndWhiskerItem;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import vm.plot.AbstractPlotter;

/**
 *
 * @author au734419
 */
public class BoxPlotCategoricalPlotter extends AbstractPlotter {

    @Override
    public JFreeChart createPlot(String mainTitle, String yAxisLabel, String[] tracesNames, String[] groupsNames, List<Float>[][] values) {
        DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();
        for (int groupId = 0; groupId < values.length; groupId++) {
            List<Float>[] valuesForGroups = values[groupId];
            for (int traceID = 0; traceID < valuesForGroups.length; traceID++) {
                List<Float> valuesForGroupAndTrace = valuesForGroups[traceID];
                BoxAndWhiskerItem item = BoxAndWhiskerCalculator.calculateBoxAndWhiskerStatistics(valuesForGroupAndTrace);
                item = new MyBoxAndWhiskerItem(item);
                String groupName = groupsNames == null ? "" : groupsNames[groupId];
                dataset.add(item, tracesNames[traceID], groupName);
            }
        }
        JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(mainTitle, "", yAxisLabel, dataset, true);
        return setAppearence(chart, tracesNames, groupsNames);
    }

    @Override
    @Deprecated
    public JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, String[] groupsNames, float[][] tracesXValues, float[][] tracesYValues) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public int precomputeSuitableWidth(int height, int tracesCount, int groupsCount) {
        int tracesTotalCount = tracesCount * groupsCount;
        float retFor600 = 160 + tracesTotalCount * 75 + groupsCount * 40;
        float ratio = height / 600f;
        return (int) (ratio * retFor600);
    }

    @Override
    public void storePlotSVG(String path, JFreeChart plot) {
        int width = precomputeSuitableWidth(IMPLICIT_HEIGHT, lastTracesCount, lastGroupCount);
        storePlotSVG(path, plot, width, IMPLICIT_HEIGHT);
    }

    @Override
    public void storePlotPNG(String path, JFreeChart plot) {
        int width = precomputeSuitableWidth(IMPLICIT_HEIGHT, lastTracesCount, lastGroupCount);
        storePlotPNG(path, plot, width, IMPLICIT_HEIGHT);
    }

    private int lastTracesCount;
    private int lastGroupCount;

    private JFreeChart setAppearence(JFreeChart chart, String[] tracesNames, String[] groupsNames) {
        lastTracesCount = tracesNames.length;
        lastGroupCount = groupsNames.length;

        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        // chart colours
        setChartColor(chart, plot);

        //legend        
        setLegendFont(chart.getLegend());
        if (tracesNames.length == 1) {
            chart.removeLegend();
        }

        // y axis settings
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        setLabelsOfAxis(yAxis);
        setTicksOfYNumericAxis(yAxis);

        BoxAndWhiskerRenderer renderer = (BoxAndWhiskerRenderer) plot.getRenderer();

        // x axis settings
        CategoryAxis xAxis = plot.getDomainAxis();
        setLabelsOfAxis(xAxis);
        setRotationOfXAxisCategoriesFont(xAxis, groupsNames, tracesNames.length);
        if (groupsNames == null || groupsNames.length <= 1) {
            xAxis.setTickLabelsVisible(false);
            xAxis.setAxisLineVisible(false);
        }
        setSpacingOfCategoriesAndTraces(renderer, xAxis, tracesNames.length, groupsNames.length);

        // set traces strokes
        for (int i = 0; i < tracesNames.length; i++) {
            renderer.setSeriesStroke(i, new BasicStroke(SERIES_STROKE));
            if (tracesNames.length > 1) {
                renderer.setSeriesPaint(i, LIGHT_COLOURS[i % LIGHT_COLOURS.length]);
                renderer.setSeriesOutlinePaint(i, COLOURS[i % COLOURS.length]);
            } else {
                renderer.setSeriesPaint(i, LIGHT_BOX_BLACK);
                renderer.setSeriesOutlinePaint(i, BOX_BLACK);
            }
            renderer.setSeriesOutlineStroke(i, new BasicStroke(3));
            renderer.setSeriesStroke(i, new BasicStroke(3));
        }
//            renderer.setMeanVisible(false);
        renderer.setUseOutlinePaintForWhiskers(true);
        renderer.setMaxOutlierVisible(false);
        renderer.setMinOutlierVisible(false);
        plot.setBackgroundAlpha(0);
        return chart;
    }

    private static class MyBoxAndWhiskerItem extends BoxAndWhiskerItem {

        public MyBoxAndWhiskerItem(BoxAndWhiskerItem item) {
            super(item.getMean(),
                    item.getMedian(),
                    item.getQ1(),
                    item.getQ3(),
                    item.getMinRegularValue(),
                    item.getMaxRegularValue(),
                    item.getMinOutlier(),
                    item.getMaxOutlier(),
                    item.getOutliers());
        }

        @Override
        public Number getMaxOutlier() {
            Number ret = super.getMinOutlier();
            List<Number> outliers = getOutliers();
            if (outliers == null || outliers.isEmpty()) {
                return ret;
            }
//            return Math.max(ret.doubleValue(), outliers.get(outliers.size() - 1).doubleValue());
            return Math.min(ret.doubleValue(), outliers.get(outliers.size() - 1).doubleValue());
        }

        @Override
        public Number getMinOutlier() {
            Number ret = super.getMinOutlier();
            List<Number> outliers = getOutliers();
            if (outliers == null || outliers.isEmpty()) {
                return ret;
            }
//            return Math.min(ret.doubleValue(), outliers.get(0).doubleValue());
            return Math.max(ret.doubleValue(), outliers.get(0).doubleValue());
        }

    }
}
