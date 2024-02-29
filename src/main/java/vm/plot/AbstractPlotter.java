/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.plot;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnits;
import org.jfree.data.xy.XYSeries;

/**
 *
 * @author au734419
 */
public abstract class AbstractPlotter {

    public static final Logger LOG = Logger.getLogger(AbstractPlotter.class.getName());

    public static final Integer IMPLICIT_WIDTH = 800;
    public static final Integer IMPLICIT_HEIGHT = 600;

    public abstract JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, Double xAxisStep, Double yAxisStep, XYSeries... traces);

    protected Double setAxisUnits(Double step, NumberAxis axis, int axisImplicitTicksNumber) {
        if (step == null) {
            double diff = axis.getUpperBound() - axis.getLowerBound();
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

    private Double getStep(float division) {
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
        int integer = (int) division;
        return integer * Math.pow(10, d - m);
    }

    public void storePlot(String path, JFreeChart plot) {
        storePlot(path, plot, IMPLICIT_WIDTH, IMPLICIT_HEIGHT);
    }

    public void storePlot(String path, JFreeChart plot, int width, int height) {
        try {
            ChartUtils.saveChartAsPNG(new File(path), plot, width, height);
        } catch (IOException ex) {
            Logger.getLogger(ToyExample.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
