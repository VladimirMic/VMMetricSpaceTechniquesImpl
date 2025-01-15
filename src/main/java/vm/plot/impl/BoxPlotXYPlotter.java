/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.plot.impl;

import java.util.List;
import java.util.Map;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import vm.datatools.DataTypeConvertor;
import vm.datatools.Tools;

/**
 *
 * @author au734419
 */
public class BoxPlotXYPlotter extends BoxPlotPlotter {

    protected final boolean isHorizontal;

    protected BoxPlotXYPlotter(boolean isHorizontal) {
        this.isHorizontal = isHorizontal;
    }

    public BoxPlotXYPlotter() {
        this(false);
    }

    @Override
    public JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, String[] tracesNames, COLOUR_NAMES[] tracesColours, Object[] xValues, List<Float>[][] values) {
        // notice that DefaultBoxAndWhiskerXYDataset contains a single only one series in the dataset. For that reason, implemented as category dataset with fixed step
        DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();
        if (tracesNames.length != values.length) {
            throw new IllegalArgumentException("Number of traces descriptions does not match the values" + tracesNames.length + ", " + values.length);
        }
        Float[] groupNumbers = DataTypeConvertor.objectsToObjectFloats(xValues);
        float xStep = (float) vm.mathtools.Tools.gcd(groupNumbers);
        if (Float.isNaN(xStep)) {
            xStep = Float.MAX_VALUE;
        }
        for (int traceID = 0; traceID < values.length; traceID++) {
            List<Float>[] valuesForGroups = values[traceID];
            if (xValues.length != valuesForGroups.length) {
                throw new IllegalArgumentException("Number of groups descriptions does not match the values" + tracesNames.length + ", " + valuesForGroups.length);
            }
            Float previousKey = null;
            Integer iValue;
            String keyString;
            for (int groupId = 0; groupId < valuesForGroups.length; groupId++) {
                List<Float> valuesForGroupAndTrace = valuesForGroups[groupId];
                Float groupName = Float.valueOf(groupNumbers[groupId].toString());
                while (previousKey != null && groupName > previousKey + xStep * 1.5f) {// othewise damages the floating point numbers
                    previousKey += xStep;
                    iValue = Tools.parseInteger(previousKey);
                    keyString = iValue == null ? previousKey.toString() : iValue.toString();
                    dataset.add(new BoxPlotPlotter.DummyBoxAndWhiskerItem(), tracesNames[traceID], keyString);
                }
                // check if it is an integer (if float than ok
                iValue = Tools.parseInteger(groupName);
                keyString = iValue == null ? groupName.toString() : iValue.toString();
                if (valuesForGroupAndTrace != null) {
                    dataset.add(valuesForGroupAndTrace, tracesNames[traceID], keyString);
                }
                previousKey = Float.valueOf(groupNumbers[groupId].toString());// othewise damages the floating point numbers
            }
        }
        JFreeChart chart;
        if (isHorizontal) {
            chart = ChartFactory.createBoxAndWhiskerChart(mainTitle, yAxisLabel, xAxisLabel + " (width: " + xStep + ")", dataset, true);
        } else {
            chart = ChartFactory.createBoxAndWhiskerChart(mainTitle, xAxisLabel, yAxisLabel, dataset, true);
        }
        return setAppearence(chart, tracesNames, tracesColours, xValues);
    }

    public JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, String traceName, COLOUR_NAMES traceColour, Map<Float, List<Float>> xToYValues) {
        Object[] xValues = xToYValues.keySet().toArray();
        List<Float>[] retArray = new List[xValues.length];
        for (int i = 0; i < xValues.length; i++) {
            Float key = (Float) xValues[i];
            retArray[i] = xToYValues.get(key);

        }
        return createPlot(mainTitle, xAxisLabel, yAxisLabel, traceName, traceColour, xValues, retArray);
    }

    @Override
    public String getSimpleName() {
        return "BoxPlotNumerical";
    }

    @Override
    protected JFreeChart setAppearence(JFreeChart chart, String[] tracesNames, COLOUR_NAMES[] tracesColours, Object[] groupsNames) {
        if (isHorizontal) {
            CategoryPlot plot = (CategoryPlot) chart.getPlot();
            String label = plot.getDomainAxis().getLabel();
            plot.setDomainAxis(new MyCategoryAxis(label, getNumberOrderOfShownXLabel(groupsNames.length)));
            plot.setOrientation(PlotOrientation.HORIZONTAL);
            plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
            plot.setDomainAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
        }
        return super.setAppearence(chart, tracesNames, tracesColours, groupsNames);
    }

    @Override
    public void storePlotPDF(String path, JFreeChart plot) {
        int width;
        if (isHorizontal) {
            width = IMPLICIT_WIDTH;
        } else {
            width = precomputeSuitableWidth(IMPLICIT_HEIGHT, lastTracesCount, lastGroupCount);
        }
        storePlotPDF(path, plot, width, IMPLICIT_HEIGHT);
    }

    @Override
    public void storePlotPNG(String path, JFreeChart plot) {
        int width;
        if (isHorizontal) {
            width = IMPLICIT_WIDTH;
        } else {
            width = precomputeSuitableWidth(IMPLICIT_HEIGHT, lastTracesCount, lastGroupCount);
        }
        storePlotPNG(path, plot, width, IMPLICIT_HEIGHT);
    }

    protected int getNumberOrderOfShownXLabel(int numberOfXLabels) {
        return 1;
    }

}
