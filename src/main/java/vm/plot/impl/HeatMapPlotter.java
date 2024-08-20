/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.plot.impl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.Map;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYZDataset;
import vm.plot.AbstractPlotter;

/**
 *
 * @author Vlada
 */
public class HeatMapPlotter extends AbstractPlotter {

    public static final Integer IMPLICIT_WIDTH_FOR_HEAT_MAP_PLOT = (int) (IMPLICIT_WIDTH * 1.5);

    public static final Integer IMPLICIT_HEIGHT_FOR_HEAT_MAP_PLOT = (int) (IMPLICIT_HEIGHT * 1.5);
    public static final Integer LEGEND_IMPLICIT_COLOUR_COUNT = 20;
    private int legendCount = LEGEND_IMPLICIT_COLOUR_COUNT;

    @Override
    public JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, Object... data) {
        String traceName = (String) data[0];
        float[][] values = (float[][]) data[1];
        Map<Object, Integer> columnHeaders = (Map<Object, Integer>) data[2];
        Map<Object, Integer> rowHeaders = (Map<Object, Integer>) data[3];
        return createPlot(mainTitle, xAxisLabel, yAxisLabel, traceName, values, columnHeaders, rowHeaders);
    }

    public JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, String traceName, float[][] values, Map<Object, Integer> columnHeaders, Map<Object, Integer> rowHeaders) {
        DefaultXYZDataset dataset = new DefaultXYZDataset();
        int size = columnHeaders.size() * rowHeaders.size();
        double[] xValues = new double[size];
        double[] yValues = new double[size];
        double[] zValues = new double[size];
        double[] extremes = new double[6];
        extremes[0] = Double.MAX_VALUE;
        extremes[2] = Double.MAX_VALUE;
        extremes[4] = Double.MAX_VALUE;
        int counter = 0;
        for (Map.Entry<Object, Integer> row : rowHeaders.entrySet()) {
            double yValue = Double.parseDouble(row.getKey().toString());
            int yIdx = row.getValue();
            extremes[2] = Math.min(extremes[2], yValue);
            extremes[3] = Math.max(extremes[3], yValue);
            for (Map.Entry<Object, Integer> column : columnHeaders.entrySet()) {
                double xValue = Double.parseDouble(column.getKey().toString());
                int xIdx = column.getValue();
                extremes[0] = Math.min(extremes[0], xValue);
                extremes[1] = Math.max(extremes[1], xValue);
                double zValue = values[yIdx][xIdx];
                xValues[counter] = xValue;
                yValues[counter] = yValue;
                zValues[counter] = zValue;
                extremes[4] = Math.min(extremes[4], zValue);
                extremes[5] = Math.max(extremes[5], zValue);
                counter++;
            }
        }
//        extremes[1] = 60;
//        extremes[3] = 1000000;
        double[][] valuesArray = new double[][]{xValues, yValues, zValues};
        dataset.addSeries(traceName, valuesArray);
        JFreeChart ret = datasetToChart(dataset, xAxisLabel, yAxisLabel, extremes);
        return ret;
    }

    private JFreeChart datasetToChart(XYZDataset dataset, String xAxisLabel, String yAxisLabel, double[] extremes) {
        // x-axis for time
        NumberAxis xAxis = new NumberAxis(xAxisLabel);
        xAxis.setLowerBound(extremes[0]);
        xAxis.setUpperBound(extremes[1]);
        xAxis.setLowerMargin(0);
        xAxis.setUpperMargin(0);

        // visible y-axis with symbols
        NumberAxis yAxis = new NumberAxis(yAxisLabel);
        yAxis.setLowerBound(extremes[2]);
        yAxis.setUpperBound(extremes[3]);

        // another invisible y-axis for scaling
        // (this is not necessary if your y-values are suitable) 
        NumberAxis valueAxis1 = new NumberAxis("Marker");
        valueAxis1.setLowerMargin(0);
        valueAxis1.setUpperMargin(0);
        valueAxis1.setVisible(false);

        // create a paint-scale and a legend showing it
        double minZ = extremes[4];
        double maxZ = extremes[5];
        LookupPaintScale paintScale = new LookupPaintScale(minZ, maxZ, Color.black);

        PaintScaleLegend psl = new PaintScaleLegend(paintScale, new NumberAxis());
        psl.setPosition(RectangleEdge.RIGHT);
        psl.setAxisLocation(AxisLocation.TOP_OR_RIGHT);
        psl.setMargin(50.0, 20.0, 80.0, 0.0);

        double stepDouble = setAxisUnits(null, (NumberAxis) psl.getAxis(), legendCount);
        float step = (float) stepDouble;
        minZ = vm.math.Tools.round((float) minZ, step, true) - step;
        for (int i = 0; minZ <= maxZ; i++) {
            int idx = i % COLOURS.length;
            minZ += step;
            paintScale.add(minZ, COLOURS[idx]);
            minZ += step;
            paintScale.add(minZ, LIGHT_COLOURS[idx]);
        }

        // finally a renderer and a plot
        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, new XYBlockRenderer());
        XYBlockRenderer renderer = ((XYBlockRenderer) plot.getRenderer());
        renderer.setPaintScale(paintScale);
        renderer.setSeriesStroke(0, new BasicStroke(30));
        JFreeChart chart = new JFreeChart(null, null, plot, false);
        chart.addSubtitle(psl);
        setAppearence(chart, plot, xAxis, yAxis, (NumberAxis) psl.getAxis());
        return chart;
    }

    @Override
    public String getSimpleName() {
        return "HeatMap";
    }

    private JFreeChart setAppearence(JFreeChart chart, XYPlot plot, NumberAxis xAxis, NumberAxis yAxis, NumberAxis zAxis) {
        setLabelsOfAxis(xAxis);
        setLabelsOfAxis(yAxis);
        setLabelsOfAxis(zAxis);
        setChartColor(chart, plot);
        return chart;
    }

    @Override
    public void storePlotPDF(String path, JFreeChart plot, int width, int height) {
        throw new UnsupportedOperationException("For some reason, the library cannot store heatmaps in a vector format");
    }

    @Override
    public void storePlotPNG(String path, JFreeChart plot) {
        storePlotPNG(path, plot, IMPLICIT_WIDTH_FOR_HEAT_MAP_PLOT, IMPLICIT_HEIGHT_FOR_HEAT_MAP_PLOT);
    }

    public void setLegendCount(int legendCount) {
        this.legendCount = legendCount;
    }

}
