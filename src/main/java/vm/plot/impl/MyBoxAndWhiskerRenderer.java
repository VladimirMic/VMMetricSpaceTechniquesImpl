/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.plot.impl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.Outlier;
import org.jfree.chart.renderer.OutlierListCollection;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;

/**
 *
 */
public class MyBoxAndWhiskerRenderer extends BoxAndWhiskerRenderer {

    public static Integer GAP_TRACES = 28;
    public static Integer GAP_GROUPS = 28;
    public static Integer Y_WIDTH = 160;

//    private Float minDisplayedCategoryValue = null;
//    private Float maxnDisplayedCategoryValue = null;
//
//    public MyBoxAndWhiskerRenderer(Object[] categoryLabels) {
//        if (Tools.isParseableToFloats(categoryLabels)) {
//            minDisplayedCategoryValue = Float.valueOf(categoryLabels[0].toString());
//            minDisplayedCategoryValue = Float.valueOf(categoryLabels[categoryLabels.length - 1].toString());
//        }
//    }
//
//    public void setMinDisplayedCategoryValue(Float minDisplayedCategoryValue) {
//        this.minDisplayedCategoryValue = minDisplayedCategoryValue;
//    }
//
//    public void setMaxnDisplayedCategoryValue(Float maxnDisplayedCategoryValue) {
//        this.maxnDisplayedCategoryValue = maxnDisplayedCategoryValue;
//    }
    private Double outlierRadius;

    public Double getOutlierRadius() {
        return outlierRadius;
    }

    public void setOutlierRadius(Double outlierRadius) {
        this.outlierRadius = outlierRadius;
    }

    @Override
    public void drawVerticalItem(Graphics2D g2, CategoryItemRendererState state,
            Rectangle2D dataArea, CategoryPlot plot, CategoryAxis domainAxis,
            ValueAxis rangeAxis, CategoryDataset dataset, int row, int column) {

        BoxAndWhiskerCategoryDataset bawDataset = (BoxAndWhiskerCategoryDataset) dataset;

        double categoryEnd = domainAxis.getCategoryEnd(column, getColumnCount(), dataArea, plot.getDomainAxisEdge());
        double categoryStart = domainAxis.getCategoryStart(column, getColumnCount(), dataArea, plot.getDomainAxisEdge());
        double categoryWidth = categoryEnd - categoryStart;

        double xx = categoryStart;
        int seriesCount = getRowCount();
//        int categoryCount = getColumnCount();

        double seriesGap = 30;
        state.setBarWidth(28);
//        double seriesGap = dataArea.getWidth() * getItemMargin() / (categoryCount * (seriesCount - 1));

        if (seriesCount > 1) {
            double usedWidth = (state.getBarWidth() * seriesCount) + (seriesGap * (seriesCount - 1));
            // offset the start of the boxes if the total width used is smaller than the category width
            double offset = (categoryWidth - usedWidth) / 2;
            xx = xx + offset + (row * (state.getBarWidth() + seriesGap));
        } else {
            // offset the start of the box if the box width is smaller than the
            // category width
            double offset = (categoryWidth - state.getBarWidth()) / 2;
            xx = xx + offset;
        }

        Paint itemPaint = getItemPaint(row, column);
        g2.setPaint(itemPaint);
        Stroke s = getItemStroke(row, column);
        g2.setStroke(s);

        double aRadius = 0;  // average radius

        RectangleEdge location = plot.getRangeAxisEdge();

        Number yQ1 = bawDataset.getQ1Value(row, column);
        Number yQ3 = bawDataset.getQ3Value(row, column);
        Number yMax = bawDataset.getMaxRegularValue(row, column);
        Number yMin = bawDataset.getMinRegularValue(row, column);
        Shape box = null;
        Paint outlinePaint = getItemOutlinePaint(row, column);

        if (yQ1 != null && yQ3 != null && yMax != null && yMin != null) {

            double yyQ1 = rangeAxis.valueToJava2D(yQ1.doubleValue(), dataArea, location);
            double yyQ3 = rangeAxis.valueToJava2D(yQ3.doubleValue(), dataArea, location);
            double yyMax = rangeAxis.valueToJava2D(yMax.doubleValue(), dataArea, location);
            double yyMin = rangeAxis.valueToJava2D(yMin.doubleValue(), dataArea, location);
            double xxmid = xx + state.getBarWidth() / 2.0;
            double halfW = (state.getBarWidth() / 2.0) * getWhiskerWidth();

            // draw the body...
            box = new Rectangle2D.Double(xx, Math.min(yyQ1, yyQ3), state.getBarWidth(), Math.abs(yyQ1 - yyQ3));
            if (getFillBox()) {
                g2.fill(box);
            }

            if (getUseOutlinePaintForWhiskers()) {
                g2.setPaint(outlinePaint);
            }
            // draw the upper shadow...
            g2.draw(new Line2D.Double(xxmid, yyMax, xxmid, yyQ3));
            g2.draw(new Line2D.Double(xxmid - halfW, yyMax, xxmid + halfW, yyMax));

            // draw the lower shadow...
            g2.draw(new Line2D.Double(xxmid, yyMin, xxmid, yyQ1));
            g2.draw(new Line2D.Double(xxmid - halfW, yyMin, xxmid + halfW, yyMin));

            g2.setStroke(getItemOutlineStroke(row, column));
            g2.setPaint(outlinePaint);
            g2.draw(box);
        }

        g2.setPaint(Color.BLACK);
        g2.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{3, 3}, 0));

        // draw mean - SPECIAL AIMS REQUIREMENT...
        if (isMeanVisible()) {
            Number yMean = bawDataset.getMeanValue(row, column);
            if (yMean != null) {
                double yyAverage = rangeAxis.valueToJava2D(yMean.doubleValue(), dataArea, location);
                aRadius = state.getBarWidth() / 4;
                // here we check that the average marker will in fact be
                // visible before drawing it...
                if ((yyAverage > (dataArea.getMinY() - aRadius)) && (yyAverage < (dataArea.getMaxY() + aRadius))) {
                    g2.draw(new Line2D.Double(xx, yyAverage, xx + state.getBarWidth(), yyAverage));
                }
            }
        }

        g2.setPaint(outlinePaint);
        g2.setStroke(getItemOutlineStroke(row, column));

        // draw median...
        if (isMedianVisible()) {
            Number yMedian = bawDataset.getMedianValue(row, column);
            if (yMedian != null) {
                double yyMedian = rangeAxis.valueToJava2D(yMedian.doubleValue(), dataArea, location);
                g2.draw(new Line2D.Double(xx, yyMedian, xx + state.getBarWidth(), yyMedian));
            }
        }

        // draw outliers...
        double maxAxisValue = rangeAxis.valueToJava2D(rangeAxis.getUpperBound(), dataArea, location) + aRadius;
        double minAxisValue = rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, location) - aRadius;
        g2.setPaint(outlinePaint);
        double oRadius = outlierRadius == null ? state.getBarWidth() / 6 : outlierRadius;    // outlier radius
        java.util.List outliers = new ArrayList();
        OutlierListCollection outlierListCollection = new OutlierListCollection();

        // OutlierListCollection
        List yOutliers = bawDataset.getOutliers(row, column);
        if (yOutliers != null) {
            Number minRegular = bawDataset.getMinRegularValue(row, column);
            Number maxRegular = bawDataset.getMaxRegularValue(row, column);
            for (int i = 0; i < yOutliers.size(); i++) {
                double outlier = ((Number) yOutliers.get(i)).doubleValue();
                if (outlier > maxRegular.doubleValue() || outlier < minRegular.doubleValue()) {
                    double yyOutlier = rangeAxis.valueToJava2D(outlier, dataArea, location);
                    outliers.add(new Outlier(xx + state.getBarWidth() / 2.0, yyOutlier, oRadius));
                }
            }
            Collections.sort(outliers);

            // Process outliers. Each outlier is either added to the
            // appropriate outlier list or a new outlier list is made // clustering
            for (Iterator iterator = outliers.iterator(); iterator.hasNext();) {
                Outlier outlier = (Outlier) iterator.next();
                Point2D point = outlier.getPoint();
                drawEllipse(point, oRadius, g2);
            }

            // draw farout indicators
            if (outlierListCollection.isHighFarOut()) {
                drawHighFarOut(aRadius / 2.0, g2, xx + state.getBarWidth() / 2.0, maxAxisValue);
            }

            if (outlierListCollection.isLowFarOut()) {
                drawLowFarOut(aRadius / 2.0, g2, xx + state.getBarWidth() / 2.0, minAxisValue);
            }
        }
        // collect entity and tool tip information...
        if (state.getInfo() != null && box != null) {
            EntityCollection entities = state.getEntityCollection();
            if (entities != null) {
                addItemEntity(entities, dataset, row, column, box);
            }
        }

    }

    /**
     * Draws two dots to represent the average value of more than one outlier.
     *
     * @param point the location
     * @param boxWidth the box width.
     * @param oRadius the radius.
     * @param g2 the graphics device.
     */
    private void drawMultipleEllipse(Point2D point, double boxWidth,
            double oRadius, Graphics2D g2) {

        Ellipse2D dot1 = new Ellipse2D.Double(point.getX() - (boxWidth / 2)
                + oRadius, point.getY(), oRadius, oRadius);
        Ellipse2D dot2 = new Ellipse2D.Double(point.getX() + (boxWidth / 2),
                point.getY(), oRadius, oRadius);
        g2.draw(dot1);
        g2.draw(dot2);
    }

    /**
     * Draws a dot to represent an outlier.
     *
     * @param point the location.
     * @param oRadius the radius.
     * @param g2 the graphics device.
     */
    private void drawEllipse(Point2D point, double oRadius, Graphics2D g2) {
        Ellipse2D dot = new Ellipse2D.Double(point.getX() + oRadius / 2, point.getY(), oRadius, oRadius);
        g2.draw(dot);
    }

    /**
     * Draws a triangle to indicate the presence of far-out values.
     *
     * @param aRadius the radius.
     * @param g2 the graphics device.
     * @param xx the x coordinate.
     * @param m the y coordinate.
     */
    private void drawHighFarOut(double aRadius, Graphics2D g2, double xx,
            double m) {
        double side = aRadius * 2;
        g2.draw(new Line2D.Double(xx - side, m + side, xx + side, m + side));
        g2.draw(new Line2D.Double(xx - side, m + side, xx, m));
        g2.draw(new Line2D.Double(xx + side, m + side, xx, m));
    }

    /**
     * Draws a triangle to indicate the presence of far-out values.
     *
     * @param aRadius the radius.
     * @param g2 the graphics device.
     * @param xx the x coordinate.
     * @param m the y coordinate.
     */
    private void drawLowFarOut(double aRadius, Graphics2D g2, double xx,
            double m) {
        double side = aRadius * 2;
        g2.draw(new Line2D.Double(xx - side, m - side, xx + side, m - side));
        g2.draw(new Line2D.Double(xx - side, m - side, xx, m));
        g2.draw(new Line2D.Double(xx + side, m - side, xx, m));
    }

    @Override
    public void drawHorizontalItem(Graphics2D g2, CategoryItemRendererState state, Rectangle2D dataArea, CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis, CategoryDataset dataset, int row, int column) {
        BoxAndWhiskerCategoryDataset bawDataset = (BoxAndWhiskerCategoryDataset) dataset;
        double categoryEnd = domainAxis.getCategoryEnd(column, getColumnCount(), dataArea, plot.getDomainAxisEdge());
        double categoryStart = domainAxis.getCategoryStart(column, getColumnCount(), dataArea, plot.getDomainAxisEdge());
        double categoryWidth = Math.abs(categoryEnd - categoryStart);

        double yy = categoryStart;
        int seriesCount = getRowCount();
        int categoryCount = getColumnCount();

        if (seriesCount > 1) {
            double seriesGap = dataArea.getHeight() * getItemMargin() / (categoryCount * (seriesCount - 1));
            double usedWidth = (state.getBarWidth() * seriesCount) + (seriesGap * (seriesCount - 1));
            // offset the start of the boxes if the total width used is smaller
            // than the category width
            double offset = (categoryWidth - usedWidth) / 2;
            yy = yy + offset + (row * (state.getBarWidth() + seriesGap));
        } else {
            // offset the start of the box if the box width is smaller than
            // the category width
            double offset = (categoryWidth - state.getBarWidth()) / 2;
            yy = yy + offset;
        }

        g2.setPaint(getItemPaint(row, column));
        Stroke s = getItemStroke(row, column);
        g2.setStroke(s);

        RectangleEdge location = plot.getRangeAxisEdge();

        Number xQ1 = bawDataset.getQ1Value(row, column);
        Number xQ3 = bawDataset.getQ3Value(row, column);
        Number xMax = bawDataset.getMaxRegularValue(row, column);
        Number xMin = bawDataset.getMinRegularValue(row, column);

        Shape box = null;
        if (xQ1 != null && xQ3 != null && xMax != null && xMin != null) {

            double xxQ1 = rangeAxis.valueToJava2D(xQ1.doubleValue(), dataArea, location);
            double xxQ3 = rangeAxis.valueToJava2D(xQ3.doubleValue(), dataArea, location);
            double xxMax = rangeAxis.valueToJava2D(xMax.doubleValue(), dataArea, location);
            double xxMin = rangeAxis.valueToJava2D(xMin.doubleValue(), dataArea, location);
            double yymid = yy + state.getBarWidth() / 2.0;
            double halfW = (state.getBarWidth() / 2.0) * getWhiskerWidth();

            // draw the box...
            box = new Rectangle2D.Double(Math.min(xxQ1, xxQ3), yy, Math.abs(xxQ1 - xxQ3), state.getBarWidth());
            if (getFillBox()) {
                g2.fill(box);
            }

            Paint outlinePaint = getItemOutlinePaint(row, column);
            if (getUseOutlinePaintForWhiskers()) {
                g2.setPaint(outlinePaint);
            }
            // draw the upper shadow...
            g2.draw(new Line2D.Double(xxMax, yymid, xxQ3, yymid));
            g2.draw(new Line2D.Double(xxMax, yymid - halfW, xxMax, yymid + halfW));

            // draw the lower shadow...
            g2.draw(new Line2D.Double(xxMin, yymid, xxQ1, yymid));
            g2.draw(new Line2D.Double(xxMin, yymid - halfW, xxMin, yymid + halfW));

            g2.setStroke(getItemOutlineStroke(row, column));
            g2.setPaint(outlinePaint);
            g2.draw(box);
        }

        // draw mean - SPECIAL AIMS REQUIREMENT...
        //////////////////////
        g2.setPaint(getArtifactPaint());
        if (isMeanVisible()) {
            Number xMean = bawDataset.getMeanValue(row, column);
            if (xMean != null) {
                double xxMean = rangeAxis.valueToJava2D(xMean.doubleValue(), dataArea, location);
                g2.draw(new Line2D.Double(xxMean, yy, xxMean, yy + state.getBarWidth()));
            }
        }

        // draw median...
        if (isMedianVisible()) {
            Number xMedian = bawDataset.getMedianValue(row, column);
            if (xMedian != null) {
                double xxMedian = rangeAxis.valueToJava2D(xMedian.doubleValue(), dataArea, location);
                g2.draw(new Line2D.Double(xxMedian, yy, xxMedian, yy + state.getBarWidth()));
            }
        }

        // draw outliers...
        double maxAxisValue = rangeAxis.valueToJava2D(rangeAxis.getUpperBound(), dataArea, location) + aRadius;
        double minAxisValue = rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, location) - aRadius;
        Paint outlinePaint = getItemOutlinePaint(row, column);
        g2.setPaint(outlinePaint);
        double oRadius = outlierRadius == null ? state.getBarWidth() / 6 : outlierRadius;    // outlier radius
        java.util.List outliers = new ArrayList();
        OutlierListCollection outlierListCollection = new OutlierListCollection();

        // OutlierListCollection
        List xOutliers = bawDataset.getOutliers(row, column);
        if (xOutliers != null) {
            Number minRegular = bawDataset.getMinRegularValue(row, column);
            Number maxRegular = bawDataset.getMaxRegularValue(row, column);
            for (int i = 0; i < xOutliers.size(); i++) {
                double outlier = ((Number) xOutliers.get(i)).doubleValue();
                if (outlier > maxRegular.doubleValue() || outlier < minRegular.doubleValue()) {
                    double xxOutlier = rangeAxis.valueToJava2D(outlier, dataArea, location);
                    outliers.add(new Outlier(xxOutlier, yy + state.getBarWidth() / 2.0, oRadius));
                }
            }
            Collections.sort(outliers);

            // Process outliers. Each outlier is either added to the
            // appropriate outlier list or a new outlier list is made // clustering
            for (Iterator iterator = outliers.iterator(); iterator.hasNext();) {
                Outlier outlier = (Outlier) iterator.next();
                Point2D point = outlier.getPoint();
                drawEllipse(point, oRadius, g2);
            }

            // draw farout indicators
            if (outlierListCollection.isHighFarOut()) {
                drawHighFarOut(aRadius / 2.0, g2, yy + state.getBarWidth() / 2.0, maxAxisValue);
            }

            if (outlierListCollection.isLowFarOut()) {
                drawLowFarOut(aRadius / 2.0, g2, yy + state.getBarWidth() / 2.0, minAxisValue);
            }
        }

        // collect entity and tool tip information...
        if (state.getInfo() != null && box != null) {
            EntityCollection entities = state.getEntityCollection();
            if (entities != null) {
                addItemEntity(entities, dataset, row, column, box);
            }
        }
    }

}
