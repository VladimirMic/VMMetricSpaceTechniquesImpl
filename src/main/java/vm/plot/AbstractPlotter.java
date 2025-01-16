/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.plot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.io.File;
import java.io.IOException;
import java.text.CompactNumberFormat;
import java.text.DateFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickMarkPosition;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jfree.graphics2d.svg.SVGUtils;
import vm.datatools.DataTypeConvertor;
import vm.javatools.SVGtoPDF;
import vm.mathtools.Tools;

/**
 *
 * @author au734419
 */
public abstract class AbstractPlotter {

    public static final Logger LOG = Logger.getLogger(AbstractPlotter.class.getName());

    public static final Integer FONT_SIZE_AXIS_LABEL = 28;
    public static final Integer FONT_SIZE_AXIS_TICKS = 28;

    public static final Integer X_TICKS_IMPLICIT_NUMBER_FOR_SHORT_DESC = 12;
    public static final Integer X_TICKS_IMPLICIT_NUMBER_FOR_LONG_DESC = 8;
    // 9 is too much // possibly create map for different lengths of descriptions. 8 for length 5

    public static final Integer Y_TICKS_IMPLICIT_NUMBER = 14;

    public static final Float SERIES_STROKE = 2f;
    public static final Float GRID_STROKE = 0.6f;

    public static final Integer IMPLICIT_WIDTH = 600;
    public static final Integer IMPLICIT_HEIGHT = 600;

    public static final Font FONT_AXIS_TITLE = new Font("Arial", Font.PLAIN, FONT_SIZE_AXIS_LABEL);
    public static final Font FONT_AXIS_MARKERS = new Font("Arial", Font.PLAIN, FONT_SIZE_AXIS_TICKS);

    public static final Stroke DASHED_STROKE = new BasicStroke(GRID_STROKE, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{3, 3}, 0);
    public static final Stroke FULL_STROKE = new BasicStroke(GRID_STROKE);

    public static final Color BOX_BLACK = Color.BLACK;
    public static final Color LIGHT_BOX_BLACK = new Color(128, 128, 128);

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

    public static final Color[] LIGHT_COLOURS = new Color[]{
        new Color(143, 187, 217),
        new Color(234, 147, 147),
        new Color(149, 207, 149),
        new Color(255, 191, 134),
        new Color(201, 179, 222),
        new Color(197, 170, 165),
        new Color(241, 187, 224),
        new Color(191, 191, 191),
        new Color(221, 222, 144),
        new Color(139, 222, 231)
    };

    public static enum COLOUR_NAMES {
        C1_BLUE,
        C2_RED,
        C3_GREEN,
        C4_ORANGE,
        C5_VIOLET,
        C6_BROWN,
        C7_PURPLE,
        C8_GREY,
        C9_LIME,
        C10_CYAN,
        CX_BLACK
    }

    protected boolean logY = false;
    protected boolean enforceInvolvingZeroToYAxis = false;
    protected boolean verticalXLabels = false;

    private Boolean includeZeroForXAxis = null;

    protected DateFormat dateFormat = null;
    protected DateTickUnitType timeTickType = null;
    protected Integer timeUnitInterval = null;

    public void setIncludeZeroForXAxis(Boolean includeZeroForXAxis) {
        this.includeZeroForXAxis = includeZeroForXAxis;
    }

    public void setLogY(boolean logY) {
        this.logY = logY;
    }

    public void setVerticalXLabels(boolean verticalXLabels) {
        this.verticalXLabels = verticalXLabels;
    }

    public void setEnforceInvolvingZeroToYAxis(boolean enforceInvolvingZeroToYAxis) {
        this.enforceInvolvingZeroToYAxis = enforceInvolvingZeroToYAxis;
    }

    public abstract JFreeChart createPlot(String mainTitle, String xAxisLabel, String yAxisLabel, Object... data);

    public abstract String getSimpleName();

    protected double setAxisUnits(Double step, NumberAxis axis, int axisImplicitTicksNumber, boolean forceIntegerStep) {
        if (step == null) {
            double diff = Math.abs(axis.getUpperBound() - axis.getLowerBound());
            float division = (float) (diff / axisImplicitTicksNumber);
            step = getStep(division);
            LOG.log(Level.INFO, "The step for the axis is set to {0}", step);
        }
        if (forceIntegerStep) {
            step = Math.ceil(step);
        }
        TickUnits tickUnits = new TickUnits();
        NumberTickUnit tickUnitNumber = new NumberTickUnit(step);
        tickUnits.add(tickUnitNumber);
        axis.setStandardTickUnits(tickUnits);
        axis.setTickUnit(tickUnitNumber);
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

    private int getMaxTickLabelLength(double lb, double ubShown, Double xStep, NumberFormat nf) {
        double ubShownCopy = ubShown;
        int ret = 0;
        while (ubShownCopy > lb) {
            int length = nf.format(ubShownCopy).length();
            ret = Math.max(ret, length);
            ubShownCopy -= xStep;
        }
        LOG.log(Level.INFO, "Max tickLength: {0} for ubShown {1} and step {2}", new Object[]{ret, ubShown, xStep});
        return ret;
    }

    public void storePlotPDF(File file, JFreeChart plot) {
        storePlotPDF(file.getAbsolutePath(), plot);
    }

    public void storePlotPNG(File file, JFreeChart plot) {
        storePlotPNG(file.getAbsolutePath(), plot);
    }

    public void storePlotPDF(String path, JFreeChart plot) {
        storePlotPDF(path, plot, IMPLICIT_WIDTH, IMPLICIT_HEIGHT);
    }

    public void storePlotPDF(String path, JFreeChart plot, int width, int height) {
        if (!path.endsWith(".svg")) {
            path += ".svg";
        }
        SVGGraphics2D g2 = new SVGGraphics2D(width, height);
        Rectangle r = new Rectangle(0, 0, width, height);
        plot.draw(g2, r);
        File f = new File(path);
        f.getParentFile().mkdirs();
        try {
            LOG.log(Level.INFO, "Storing plot {0}", f);
            SVGUtils.writeToSVG(f, g2.getSVGElement());
            boolean transformToPdf = SVGtoPDF.transformToPdf(f);
            if (transformToPdf) {
                f.delete();
            }
        } catch (IOException ex) {
            Logger.getLogger(AbstractPlotter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void storePlotPNG(String path, JFreeChart plot) {
        storePlotPNG(path, plot, IMPLICIT_WIDTH, IMPLICIT_HEIGHT);
    }

    public void storePlotPNG(File file, JFreeChart plot, int width, int height) {
        storePlotPNG(file.getAbsolutePath(), plot, width, height);
    }

    public void storePlotPDF(File file, JFreeChart plot, int width, int height) {
        storePlotPDF(file.getAbsolutePath(), plot, width, height);
    }

    public void storePlotPNG(String path, JFreeChart plot, int width, int height) {
        if (!path.endsWith(".png")) {
            path += ".png";
        }
        File file = new File(path);
        File parentFile = file.getParentFile();
        String name = file.getName();
        file = new File(parentFile, "png");
        file.mkdirs();
        file = new File(file, name);
        try {
            LOG.log(Level.INFO, "Storing plot to {0}", path);
            ChartUtils.saveChartAsPNG(file, plot, width, height);
        } catch (IOException ex) {
            Logger.getLogger(AbstractPlotter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private Double xAxisUpperBound = Double.NaN;

    public void setXAxisUpperBound(double value) {
        xAxisUpperBound = value;
    }

    protected void setTicksOfXNumericAxis(ValueAxis xAxis) {
        if (!xAxisUpperBound.equals(Double.NaN)) {
            xAxis.setUpperBound(xAxisUpperBound);
        }
        Boolean includeZeroForXAxisLocal = includeZeroForXAxis;
        try {
            boolean coversZero = xAxis.getLowerBound() <= 0 && xAxis.getUpperBound() >= 0;
            if (coversZero) {
                includeZeroForXAxisLocal = true;
            } else if (includeZeroForXAxisLocal == null) {
                LOG.log(Level.WARNING, "Asking for involving zero to x axis");
                Object[] options = new String[]{"Yes", "No"};
                String question = "Do you want to involve ZERO to the X axis for all the plots being produced?";
                int add = JOptionPane.showOptionDialog(null, question, "Involve zero to the axis?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, JOptionPane.NO_OPTION);
                includeZeroForXAxisLocal = add == 0;
            }
        } catch (Throwable e) {
            includeZeroForXAxisLocal = true;
        }
        if (xAxis instanceof NumberAxis) {
            setTicksForNumberAxis((NumberAxis) xAxis, includeZeroForXAxisLocal);
        }
        if (xAxis instanceof DateAxis) {
            setTicksForDateAxis((DateAxis) xAxis);
        }
    }

    private void setTicksForDateAxis(DateAxis xAxis) {
        xAxis.setDateFormatOverride(dateFormat);
        xAxis.setVerticalTickLabels(false);
        xAxis.setTickMarkPosition(DateTickMarkPosition.START);
        xAxis.setTickUnit(new DateTickUnit(timeTickType, timeUnitInterval, dateFormat));
        xAxis.setVerticalTickLabels(verticalXLabels);
    }

    protected void setRotationOfYAxisNumbersFont(NumberAxis axis, double step, NumberFormat nf) {
        // intentional
    }

    private void setTicksForNumberAxis(NumberAxis xAxis, Boolean includeZeroForXAxisLocal) {
        xAxis.setAutoRangeIncludesZero(includeZeroForXAxisLocal);
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        Double xStep = setAxisUnits(null, xAxis, X_TICKS_IMPLICIT_NUMBER_FOR_SHORT_DESC, false);
        if (xStep >= 120) {
            NumberFormat nfBig = new CompactNumberFormat(
                    "#,##0.##",
                    DecimalFormatSymbols.getInstance(Locale.US),
                    new String[]{"", "", "", "0K", "00K", "000K", "0M", "00M", "000M", "0B", "00B", "000B", "0T", "00T", "000T"});
            nf = nfBig;
            try {
                setDecimalsForShortExpressionOfYTicks(nf, xStep, xAxis);
            } catch (ParseException ex) {
                Logger.getLogger(AbstractPlotter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        xAxis.setNumberFormatOverride(nf);

        double ubShown = calculateHighestVisibleTickValue(xAxis);
        double lb = xAxis.getLowerBound();
        int maxTickLength = getMaxTickLabelLength(lb, ubShown, xStep, nf);
        if (maxTickLength >= 4) {
            setAxisUnits(null, xAxis, X_TICKS_IMPLICIT_NUMBER_FOR_LONG_DESC, false);
        }
    }

    protected void setRotationOfXAxisCategoriesFont(CategoryAxis xAxis, Object[] groupsNames, int tracesPerGroup) {
        int maxLength = 0;
        for (Object groupName : groupsNames) {
            maxLength = Math.max(maxLength, groupName.toString().length());
        }
        if (maxLength >= 3 * tracesPerGroup) {
            xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        }
    }

    protected void setSpacingOfCategoriesAndTraces(CategoryPlot plot, BoxAndWhiskerRenderer renderer, CategoryAxis xAxis, int tracesPerGroupCount, int groupCount) {
        if (groupCount == 1) {
            renderer.setItemMargin(0.3);
            xAxis.setCategoryMargin(0);
        }
        xAxis.setLowerMargin(0);
        xAxis.setUpperMargin(0);
//        int tracesTotalCount = tracesPerGroupCount * groupCount;
//        double edgeMarging = 1f / (4 * tracesTotalCount);
//        xAxis.setLowerMargin(edgeMarging);
//        xAxis.setUpperMargin(edgeMarging);
    }

    private float minRecall = 0.9f;

    public void updateMinRecall(float minRecall) {
        minRecall = Tools.round(minRecall, 0.1f, true);
        this.minRecall = Math.min(minRecall, this.minRecall);
    }

    protected void setTicksOfYNumericAxis(NumberAxis yAxis, boolean forceIntegers) {
        String label = yAxis.getLabel();
        label = label.toLowerCase().trim();
        if (enforceInvolvingZeroToYAxis) {
            yAxis.setLowerBound(0);
        }
        if ("".equals(label)) {
            yAxis.setTickLabelsVisible(false);
            yAxis.setTickMarksVisible(false);
            return;
        }
        if (label.equals("recall") || label.equals("precision") || label.equals("accuracy")) {
            NumberFormat nf = NumberFormat.getInstance(Locale.US);
            yAxis.setNumberFormatOverride(nf);
            yAxis.setUpperBound(1);
            if (!enforceInvolvingZeroToYAxis) {
                yAxis.setLowerBound(minRecall);
            }
            setAxisUnits(null, yAxis, Y_TICKS_IMPLICIT_NUMBER, forceIntegers);
            return;
        }
        if (label.equals("frr") || label.equals("false reject rate")) {
            yAxis.setUpperBound(1 - minRecall);
        }
        double yStep = setAxisUnits(null, yAxis, Y_TICKS_IMPLICIT_NUMBER, forceIntegers);
        if (yAxis.getUpperBound() >= 1000) {
            NumberFormat nfBig = new CompactNumberFormat(
                    "#,##0.##",
                    DecimalFormatSymbols.getInstance(Locale.US),
                    new String[]{"", "", "", "0K", "00K", "000K", "0M", "00M", "000M", "0B", "00B", "000B", "0T", "00T", "000T"});
            try {
                setDecimalsForShortExpressionOfYTicks(nfBig, yStep, yAxis);
            } catch (ParseException ex) {
                Logger.getLogger(AbstractPlotter.class.getName()).log(Level.SEVERE, null, ex);
            }
            yAxis.setNumberFormatOverride(nfBig);
        } else {
            NumberFormat nf = NumberFormat.getInstance(Locale.US);
            yAxis.setNumberFormatOverride(nf);
        }
        setRotationOfYAxisNumbersFont(yAxis, yStep, yAxis.getNumberFormatOverride());
    }

    protected void setLabelsOfAxis(Axis axis) {
        axis.setTickLabelFont(FONT_AXIS_MARKERS);
        axis.setLabelFont(FONT_AXIS_TITLE);
        axis.setAxisLineVisible(false); // doubles lines next to axes
    }

    protected void setLegendFont(LegendTitle legend) {
        if (legend != null) {
            legend.setItemFont(FONT_AXIS_MARKERS);
            legend.setItemLabelPadding(new RectangleInsets(2, 2, 2, 15));
        }
    }

    protected void setChartColor(JFreeChart chart, Plot plot) {
        chart.setBackgroundPaint(Color.WHITE);
        plot.setBackgroundAlpha(0);
        if (plot instanceof XYPlot) {
            XYPlot xyPlot = (XYPlot) plot;
            xyPlot.setDomainGridlineStroke(DASHED_STROKE);
            xyPlot.setDomainGridlinePaint(Color.GRAY);
            xyPlot.setRangeGridlineStroke(DASHED_STROKE);
            xyPlot.setRangeGridlinePaint(Color.GRAY);
        } else if (plot instanceof CategoryPlot) {
            CategoryPlot catPlot = (CategoryPlot) plot;
            catPlot.setRangeGridlinesVisible(true);
            catPlot.setRangeGridlineStroke(DASHED_STROKE);
            catPlot.setRangeGridlinePaint(Color.GRAY);
        } else {
            System.out.println("!!!");
            System.out.println("!!!");
            System.out.println("!!!");
            System.out.println("!!!");
        }
    }

    protected double calculateHighestVisibleTickValue(NumberAxis axis) {
        double unit = axis.getTickUnit().getSize();
        double index = Math.floor(axis.getRange().getUpperBound() / unit);
        return index * unit;
    }

    private void setDecimalsForShortExpressionOfYTicks(NumberFormat nfBig, Double step, NumberAxis axis) throws ParseException {
        double max = calculateHighestVisibleTickValue(axis);
        double lb = axis.getLowerBound();
        int decimalsOfNext = 0;
        boolean ok;
        do {
            ok = true;
            double currDouble = max;
            String prev = "";
            String currString;
            while (currDouble > lb) {
                currString = nfBig.format(currDouble);
                double check = nfBig.parse(currString).doubleValue();
                check = DataTypeConvertor.floatToPreciseDouble(Tools.round((float) check, step.floatValue(), false)); // problem with floats
                if (currString.equals(prev) || check != currDouble) {
                    ok = false;
                    decimalsOfNext++;
                    nfBig.setMaximumFractionDigits(decimalsOfNext);
                    break;
                }
                currDouble -= step;
                prev = currString;
            }
        } while (!ok);
        LOG.log(Level.INFO, "yStep: {0}, decimals: {1}", new Object[]{step, decimalsOfNext});
    }

    public static final Color getColor(COLOUR_NAMES name, boolean light) {
        int idx = Arrays.binarySearch(COLOUR_NAMES.values(), name);
        if (name == COLOUR_NAMES.CX_BLACK) {
            if (!light) {
                return BOX_BLACK;
            } else {
                return LIGHT_BOX_BLACK;
            }
        }
        if (!light) {
            return COLOURS[idx];
        }
        return LIGHT_COLOURS[idx];
    }

    protected float[] minMaxY;

    protected void setMinAndMaxYValues(float[][] tracesYValues) {
        minMaxY = new float[]{Float.MAX_VALUE, -Float.MAX_VALUE};
        for (float[] r : tracesYValues) {
            for (float f : r) {
                if (f > 0) {
                    minMaxY[0] = Math.min(minMaxY[0], f);
                }
                minMaxY[1] = Math.max(minMaxY[1], f);
            }
        }
        if (minMaxY[0] > 0) {
            minMaxY[0] *= 0.9;
        } else {
            minMaxY[0] *= 1.1;
        }
        if (minMaxY[1] > 0) {
            minMaxY[1] *= 0.9;
        } else {
            minMaxY[1] *= 1.1;
        }
    }

}
