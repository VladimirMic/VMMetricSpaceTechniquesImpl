/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.plot.impl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.BoxAndWhiskerItem;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import vm.plot.AbstractPlotter;
import static vm.plot.AbstractPlotter.COLOURS;
import static vm.plot.AbstractPlotter.getColor;

/**
 *
 * @author au734419
 */
public class BoxPlotPlotter extends AbstractPlotter {

    @Override
    public JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, Object... data) {
        String[] tracesNames = (String[]) data[0];
        COLOUR_NAMES[] tracesColours = (COLOUR_NAMES[]) data[1];
        Object[] groupsNames = (Object[]) data[2];
        List<Float>[][] values = (List<Float>[][]) data[3];
        return createPlot(mainTitle, xAxisLabel, yAxisLabel, tracesNames, tracesColours, groupsNames, values);
    }

    public JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, String[] tracesNames, COLOUR_NAMES[] tracesColours, Object[] groupsNames, List<Float>[][] values) {
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
                if (valuesForGroupAndTrace != null && !valuesForGroupAndTrace.isEmpty()) {
                    dataset.add(valuesForGroupAndTrace, tracesNames[traceID], groupName);
                    BoxAndWhiskerItem item = dataset.getItem(traceID, groupId);
                    float mean = item.getMean().floatValue();
                    LOG.log(Level.INFO, "Mean {3} for {0} in group {1}: {2}", new Object[]{tracesNames[traceID], groupName, mean, yAxisLabel});
                } else {
                    List<Float> atrapa = new ArrayList<>();
                    atrapa.add(0f);
                    dataset.add(atrapa, tracesNames[traceID], groupName);
                }
            }
        }
        JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(mainTitle, xAxisLabel, yAxisLabel, dataset, true);
        return setAppearence(chart, tracesNames, tracesColours, groupsNames);
    }

    public int precomputeSuitableWidth(int height, int tracesCount, int groupsCount) {
        int tracesTotalCount = tracesCount * groupsCount;
        double usedWidth = 28 * tracesTotalCount + 30 * (tracesTotalCount - 1);
        if (groupsCount > 1) {
            usedWidth += 45 * groupsCount + 160;
        } else {
            usedWidth += 70;
        }
        float ratio = height / 500f;
        return (int) (ratio * usedWidth);
    }

    @Override
    public void storePlotPDF(String path, JFreeChart plot) {
        int width = precomputeSuitableWidth(IMPLICIT_HEIGHT, lastTracesCount, lastGroupCount);
        storePlotPDF(path, plot, width, IMPLICIT_HEIGHT);
    }

    @Override
    public void storePlotPNG(String path, JFreeChart plot) {
        int width = precomputeSuitableWidth(IMPLICIT_HEIGHT, lastTracesCount, lastGroupCount);
        storePlotPNG(path, plot, width, IMPLICIT_HEIGHT);
    }

    private int lastTracesCount;
    private int lastGroupCount;

    protected JFreeChart setAppearence(JFreeChart chart, String[] tracesNames, COLOUR_NAMES[] tracesColours, Object[] groupsNames) {
        lastTracesCount = tracesNames.length;
        lastGroupCount = groupsNames.length;

        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        // chart colours
        setChartColor(chart, plot);

        //legend        
        setLegendFont(chart.getLegend());
        if (tracesNames.length == 1) {
//            chart.removeLegend();
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
            xAxis.setTickMarksVisible(false);
            xAxis.setTickMarkInsideLength(0);
            xAxis.setTickMarkOutsideLength(0);
            xAxis.setTickMarksVisible(false);
        }
        setSpacingOfCategoriesAndTraces(plot, renderer, xAxis, tracesNames.length, groupsNames.length);

        // set traces strokes
        for (int i = 0; i < tracesNames.length; i++) {
            renderer.setSeriesStroke(i, new BasicStroke(SERIES_STROKE));
            Color darkColor = tracesColours == null ? COLOURS[i % COLOURS.length] : getColor(tracesColours[i], false);
            Color lightColor = tracesColours == null ? LIGHT_COLOURS[i % LIGHT_COLOURS.length] : getColor(tracesColours[i], true);
            if (tracesNames.length > 1) {
                renderer.setSeriesPaint(i, lightColor);
                renderer.setSeriesOutlinePaint(i, darkColor);
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
