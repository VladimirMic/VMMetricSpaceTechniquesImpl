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
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import vm.plot.AbstractPlotter;

/**
 *
 * @author au734419
 */
public class BoxPlotPlotter extends AbstractPlotter {

    @Override
    public JFreeChart createPlot(String mainTitle, String yAxisLabel, String[] tracesNames, Object[] groupsNames, List<Float>[][] values) {
        DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();
        if (tracesNames.length != values.length) {
            throw new IllegalArgumentException("Number of traces descriptions does not match the values" + tracesNames.length + ", " + values.length);
        }
        for (int traceID = 0; traceID < values.length; traceID++) {
            List<Float>[] valuesForGroups = values[traceID];
            if (groupsNames.length != valuesForGroups.length) {
                throw new IllegalArgumentException("Number of groups descriptions does not match the values" + tracesNames.length + ", " + valuesForGroups.length);
            }
            for (int groupId = 0; groupId < valuesForGroups.length; groupId++) {
                List<Float> valuesForGroupAndTrace = valuesForGroups[groupId];
                String groupName = groupsNames == null ? "" : groupsNames[groupId].toString();
                dataset.add(valuesForGroupAndTrace, tracesNames[traceID], groupName);
            }
        }
        JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(mainTitle, "", yAxisLabel, dataset, true);
        return setAppearence(chart, tracesNames, groupsNames);
    }

    @Override
    @Deprecated
    public JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, Object[] groupsNames, float[][] tracesXValues, float[][] tracesYValues) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int precomputeSuitableWidth(int height, int tracesCount, int groupsCount) {
        int tracesTotalCount = tracesCount * groupsCount;
        double usedWidth = 28 * tracesTotalCount + 30 * (tracesTotalCount - 1) + 45 * groupsCount + 160;
        float ratio = height / 500f;
        return (int) (ratio * usedWidth);
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

    protected JFreeChart setAppearence(JFreeChart chart, String[] tracesNames, Object[] groupsNames) {
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

        BoxAndWhiskerRenderer renderer = new CustomBoxAndWhiskerRenderer();

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
        renderer.setUseOutlinePaintForWhiskers(true);
        renderer.setMaxOutlierVisible(false);
        renderer.setMinOutlierVisible(false);
        plot.setBackgroundAlpha(0);
        plot.setRenderer(renderer);
        return chart;
    }

    @Override
    public String getSimpleName() {
        return "BoxPlotCat";
    }

}
