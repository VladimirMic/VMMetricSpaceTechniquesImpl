/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.plot;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnits;
import org.jfree.data.xy.XYSeries;
import vm.datatools.Tools;

/**
 *
 * @author au734419
 */
public abstract class AbstractPlotter {

    public static final Logger LOG = Logger.getLogger(AbstractPlotter.class.getName());

    public static final Integer IMPLICIT_WIDTH = 800;
    public static final Integer IMPLICIT_HEIGHT = 600;

    public abstract JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, Double xAxisStep, Double yAxisStep, XYSeries... traces);

    public JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, XYSeries... traces) {
        return createPlot(mainTitle, xAxisLabel, yAxisLabel, null, null, traces);
    }

    public JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, Double xAxisStep, Double yAxisStep, String[] tracesNames, float[][] tracesXValues, float[][] tracesYValues) {
        XYSeries[] traces = transformCoordinatesIntoTraces(tracesNames, tracesXValues, tracesYValues);
        return createPlot(mainTitle, xAxisLabel, yAxisLabel, xAxisStep, yAxisStep, traces);
    }

    public JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, Double xAxisStep, Double yAxisStep, String traceName, float[] traceXValues, float[] traceYValues) {
        XYSeries[] traces = transformCoordinatesIntoTraces(traceName, traceXValues, traceYValues);
        return createPlot(mainTitle, xAxisLabel, yAxisLabel, xAxisStep, yAxisStep, traces);
    }

    public JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, String[] tracesNames, float[][] tracesXValues, float[][] tracesYValues) {
        XYSeries[] traces = transformCoordinatesIntoTraces(tracesNames, tracesXValues, tracesYValues);
        return createPlot(mainTitle, xAxisLabel, yAxisLabel, null, null, traces);
    }

    public JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, String traceName, float[] traceXValues, float[] traceYValues) {
        XYSeries[] traces = transformCoordinatesIntoTraces(traceName, traceXValues, traceYValues);
        return createPlot(mainTitle, xAxisLabel, yAxisLabel, null, null, traces);
    }

    protected XYSeries[] transformCoordinatesIntoTraces(String traceName, float[] traceXValues, float[] traceYValues) {
        String[] names = new String[]{traceName};
        return transformCoordinatesIntoTraces(names, new float[][]{traceXValues}, new float[][]{traceYValues});
    }

    protected XYSeries[] transformCoordinatesIntoTraces(String[] tracesNames, float[][] tracesXValues, float[][] tracesYValues) {
        if (tracesNames.length != tracesXValues.length || tracesNames.length != tracesYValues.length) {
            throw new IllegalArgumentException("Inconsistent number of traces in data. Names count: " + tracesNames.length + ", x count: " + tracesXValues.length + ", y count: " + tracesYValues.length);
        }
        XYSeries[] ret = new XYSeries[tracesNames.length];
        for (int i = 0; i < tracesNames.length; i++) {
            ret[i] = new XYSeries(tracesNames[i]);
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

    protected double setAxisUnits(Double step, NumberAxis axis, int axisImplicitTicksNumber) {
        if (step == null) {
            double diff = Math.abs(axis.getUpperBound() - axis.getLowerBound());
            if (axis.getUpperBound() < axis.getLowerBound()) {
                String s = "";
            }
            float division = (float) (diff / axisImplicitTicksNumber);
            step = getStep(division);
            LOG.log(Level.INFO, "The step for the axis is set to {0}", step);
        }
        TickUnits tickUnits = new TickUnits();
        NumberTickUnit xTickUnitNumber = new NumberTickUnit(step);
        tickUnits.add(xTickUnitNumber);
        axis.setStandardTickUnits(tickUnits);
        return step;
    }

    private double getStep(float division) {
        int m = 0;
        int d = 0;
        while (division > 1) {
            division /= 10;
            d++;
        }
        while (division < 1) {
            division *= 10;
            m++;
        }
        int integer = (int) (division + 1);
        return integer * Math.pow(10, d - m);
    }

    protected double getThresholdForXStepForUB(double ub) {
        int m = 0;
        int d = 0;
        double ubCopy = Math.abs(ub);
        while (ubCopy > 1) {
            ubCopy /= 1000;
            d += 3;
        }
        while (ubCopy < 1) {
            ubCopy *= 1000;
            m += 3;
        }
        double ret = 0.1 * Math.pow(10, d - m);
        LOG.log(Level.INFO, "UB: {0}, minStep: {1}", new Object[]{ub, ret});
        return ret;
    }

    protected int getMaxTickLabelLength(double bound, double xStep) {
        int ret = 0;
        if (bound < 0) {
            ret++; // minus
        }
        int intxStep = (int) xStep;
        float fxStep = (float) xStep;
        if (fxStep != intxStep) {
            ret++; // dot
        }
        while (fxStep != intxStep) {
            ret++; //floating point numbers
            fxStep *= 10;
            intxStep = (int) fxStep;
        }
        if (bound < 1 && bound > -1) {
            ret++; // zero before dot
        }
        bound = Math.abs(bound);
        while (bound > 1) {
            ret++;
            bound /= 10;
        }
        return ret;
    }

    protected int getMaxTickLabelLength(double lb, double ub, double xStep) {
        int retLB = getMaxTickLabelLength(lb, xStep);
        int retUB = getMaxTickLabelLength(ub, xStep);
        int ret = Math.max(retLB, retUB);
        LOG.log(Level.INFO, "UB: {0}, LB: {1}, xStep: {2}, max tickLength: {3}", new Object[]{ub, lb, xStep, ret});
        return ret;
    }

    public void storePlot(String path, JFreeChart plot) {
        storePlot(path, plot, IMPLICIT_WIDTH, IMPLICIT_HEIGHT);
    }

    public void storePlot(String path, JFreeChart plot, int width, int height) {
        if (!path.endsWith(".png")) {
            path += ".png";
        }
        try {
            LOG.log(Level.INFO, "Storing plot to {0}", path);
            ChartUtils.saveChartAsPNG(new File(path), plot, width, height);
        } catch (IOException ex) {
            Logger.getLogger(ToyExample.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
