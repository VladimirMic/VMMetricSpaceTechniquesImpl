/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.plot.impl;

import java.util.ArrayList;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import vm.datatools.DataTypeConvertor;
import vm.datatools.Tools;

/**
 *
 * @author au734419
 */
@Deprecated // feel free to use BoxPlotPlotter instead. It check the x axis, and of it consists of numbers, use this class automatically
public class BoxPlotXYPlotter extends BoxPlotPlotter {

    @Override
    public JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, String[] tracesNames, COLOUR_NAMES[] tracesColours, Object[] groupsNames, List<Float>[][] values) {
        DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();
        if (tracesNames.length != values.length) {
            throw new IllegalArgumentException("Number of traces descriptions does not match the values" + tracesNames.length + ", " + values.length);
        }
        Float[] groupNumbers = DataTypeConvertor.objectsToObjectFloats(groupsNames);
        float xStep = (float) vm.math.Tools.gcd(groupNumbers);
        for (int traceID = 0; traceID < values.length; traceID++) {
            List<Float>[] valuesForGroups = values[traceID];
            if (groupsNames.length != valuesForGroups.length) {
                throw new IllegalArgumentException("Number of groups descriptions does not match the values" + tracesNames.length + ", " + valuesForGroups.length);
            }
            Float previousKey = null;
            Integer iValue;
            String keyString;
            for (int groupId = 0; groupId < valuesForGroups.length; groupId++) {
                List<Float> valuesForGroupAndTrace = valuesForGroups[groupId];
                Float groupName = groupsNames == null ? groupId : Float.valueOf(groupNumbers[groupId].toString());
                while (previousKey != null && groupName > previousKey + xStep) {
                    previousKey += xStep;
                    iValue = Tools.parseInteger(previousKey);
                    keyString = iValue == null ? previousKey.toString() : iValue.toString();
                    dataset.add(new ArrayList(), tracesNames[traceID], keyString);
                }
                iValue = Tools.parseInteger(groupName);
                keyString = iValue == null ? groupName.toString() : iValue.toString();
                if (valuesForGroupAndTrace != null) {
                    dataset.add(valuesForGroupAndTrace, tracesNames[traceID], keyString);
                }
                previousKey = groupName;
            }
        }
        JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(mainTitle, xAxisLabel, yAxisLabel, dataset, true);
        return setAppearence(chart, tracesNames, tracesColours, groupsNames);
    }

    @Override
    public String getSimpleName() {
        return "BoxPlotNumerical";
    }

}
