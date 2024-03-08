/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.plot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jfree.graphics2d.svg.SVGUtils;

/**
 *
 * @author au734419
 */
public abstract class AbstractPlotter {

    public static final Logger LOG = Logger.getLogger(AbstractPlotter.class.getName());

    public static final Integer FONT_SIZE_AXIS_LABEL = 30;
    public static final Integer FONT_SIZE_AXIS_TICKS = 28;

    public static final Integer X_TICKS_IMPLICIT_NUMBER_FOR_SHORT_DESC = 12;
    public static final Integer X_TICKS_IMPLICIT_NUMBER_FOR_LONG_DESC = 8; // 9 is too much // pripadne udelat mapu pro ruzne max delky popisu. 8 by bylo pro delku 5

    public static final Integer Y_TICKS_IMPLICIT_NUMBER = 14;

    public static final Float SERIES_STROKE = 2f;
    public static final Float GRID_STROKE = 0.6f;

    public static final Integer IMPLICIT_WIDTH = 800;
    public static final Integer IMPLICIT_HEIGHT = 600;

    public static final Font FONT_AXIS_TITLE = new Font("Arial", Font.PLAIN, FONT_SIZE_AXIS_LABEL);
    public static final Font FONT_AXIS_MARKERS = new Font("Arial", Font.PLAIN, FONT_SIZE_AXIS_TICKS);

    public static final Color[] COLOURS = new Color[]{
        new Color(31, 119, 180),
        new Color(214, 39, 40),
        new Color(44, 160, 44),
        new Color(255, 127, 14),
        new Color(148, 103, 189),
        new Color(140, 86, 75),
        new Color(227, 119, 194),
        new Color(127, 127, 127),
        new Color(188, 189, 34),
        new Color(23, 190, 207)
    };

    public abstract JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, String[] tracesNames, float[][] tracesXValues, float[][] tracesYValues);

    protected double setAxisUnits(Double step, NumberAxis axis, int axisImplicitTicksNumber) {
        if (step == null) {
            double diff = Math.abs(axis.getUpperBound() - axis.getLowerBound());
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
        double iDouble = integer;
        double power = Math.pow(10, d - m);
        double ret;
        if (power < 1) { // numeric stupid precision - java is making errors without that
            power = Math.pow(10, m - d);
            ret = iDouble / power;
        } else {
            ret = iDouble * power;
        }
        return ret;
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
        double fxStep = xStep;
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

    public void storePlotSVG(String path, JFreeChart plot) {
        storePlotSVG(path, plot, IMPLICIT_WIDTH, IMPLICIT_HEIGHT);
    }

    public void storePlotSVG(String path, JFreeChart plot, int width, int height) {
        if (!path.endsWith(".svg")) {
            path += ".svg";
        }
        SVGGraphics2D g2 = new SVGGraphics2D(width, height);
        Rectangle r = new Rectangle(0, 0, width, height);
        plot.draw(g2, r);
        File f = new File(path);
        try {
            SVGUtils.writeToSVG(f, g2.getSVGElement());
        } catch (IOException ex) {
            Logger.getLogger(AbstractPlotter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Deprecated // store as svg
    public void storePlotPNG(String path, JFreeChart plot) {
        storePlotPNG(path, plot, IMPLICIT_WIDTH, IMPLICIT_HEIGHT);
    }

    public void storePlotPNG(String path, JFreeChart plot, int width, int height) {
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

    private Boolean includeZeroForXAxis = null;

    protected void setTicksOfXNumericAxis(NumberAxis xAxis) {
        if (includeZeroForXAxis == null) {
            LOG.log(Level.WARNING, "Asking for involving zero to x axis");
            Object[] options = new String[]{"Yes", "No"};
            String question = "Do you want to involve ZERO to the X axis for all the plots being produced?";
            int add = JOptionPane.showOptionDialog(null, question, "Involve zero to the axis?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, JOptionPane.NO_OPTION);
            includeZeroForXAxis = add == 0;
        }
        xAxis.setAutoRangeIncludesZero(includeZeroForXAxis);

        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        NumberFormat nfBig = NumberFormat.getCompactNumberInstance(Locale.US, NumberFormat.Style.SHORT);
        nfBig.setMinimumFractionDigits(1);

        Double xStep = setAxisUnits(null, xAxis, X_TICKS_IMPLICIT_NUMBER_FOR_SHORT_DESC);
        double ub = xAxis.getUpperBound();
        double lb = xAxis.getLowerBound();
        int maxTickLength = getMaxTickLabelLength(lb, ub, xStep);
        if (maxTickLength >= 4) {
            xStep = setAxisUnits(null, xAxis, X_TICKS_IMPLICIT_NUMBER_FOR_LONG_DESC);
            double thr = getThresholdForXStepForUB(ub);
            if (maxTickLength >= 3 && ub >= 1000 && xStep >= thr) {
                nf = nfBig;
            }
        }
        if (xStep >= 1000) {
            nf = NumberFormat.getCompactNumberInstance(Locale.US, NumberFormat.Style.SHORT);
        }
        xAxis.setNumberFormatOverride(nf);
    }

    protected void setTicksOfYNumericAxis(NumberAxis yAxis) {
        double yStep = setAxisUnits(null, yAxis, Y_TICKS_IMPLICIT_NUMBER);
        double ub = yAxis.getUpperBound();
        if (ub >= 1000) {
            NumberFormat nfBig = NumberFormat.getCompactNumberInstance(Locale.US, NumberFormat.Style.SHORT);
            double lb = yAxis.getLowerBound();
            if (ub - lb <= 900000) {
                int decimals = getDecimalsForShortExpressionOfYTicks(ub, yStep);
                nfBig.setMinimumFractionDigits(decimals);
            }
            yAxis.setNumberFormatOverride(nfBig);
        } else {
            NumberFormat nf = NumberFormat.getInstance(Locale.US);
            yAxis.setNumberFormatOverride(nf);
        }
    }

    protected void setLabelsOfAxis(ValueAxis axis) {
        axis.setTickLabelFont(FONT_AXIS_MARKERS);
        axis.setLabelFont(FONT_AXIS_TITLE);
    }

    protected void setLegendFont(LegendTitle legend) {
        if (legend != null) {
            legend.setItemFont(FONT_AXIS_MARKERS);
        }
    }

    protected void setChartColor(JFreeChart chart, Plot plot) {
        chart.setBackgroundPaint(Color.WHITE);
        plot.setBackgroundAlpha(0);
        if (plot instanceof XYPlot) {
            XYPlot xyPlot = (XYPlot) plot;
            xyPlot.setDomainGridlinePaint(Color.BLACK);
            xyPlot.setDomainGridlineStroke(new BasicStroke(GRID_STROKE));
            xyPlot.setRangeGridlineStroke(new BasicStroke(GRID_STROKE));
        } else {
            System.out.println("!!!");
            System.out.println("!!!");
            System.out.println("!!!");
            System.out.println("!!!");
        }
    }

    private int getDecimalsForShortExpressionOfYTicks(double ub, Double yStep) {
        int ret = 0;
        int div = 0;
        while (ub > 1000) {
            div += 3;
            ub /= 1000;
        }
        yStep = yStep / Math.pow(10, div);
        double mod = yStep % 1;
        while (mod != 0) {
            ret++;
            mod = (10 * mod) % 1;
        }
        LOG.log(Level.INFO, "yStep: {0}, decimals: {1}", new Object[]{yStep, ret});
        return ret;
    }

}
