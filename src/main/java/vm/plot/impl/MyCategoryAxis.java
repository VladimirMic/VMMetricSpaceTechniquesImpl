package vm.plot.impl;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.List;
import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPosition;
import org.jfree.chart.axis.CategoryTick;

import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.entity.CategoryLabelEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.text.TextBlock;

/**
 * An axis that displays categories. This is used with the {@link CategoryPlot}
 * class.
 */
public class MyCategoryAxis extends CategoryAxis implements Cloneable, Serializable {

    /**
     * For serialization.
     */
    private static final long serialVersionUID = 5886554608114265863L;

    private final int eachTickToShow;

    /**
     * Creates a new category axis with no label.
     */
    public MyCategoryAxis() {
        this(null, 1);
    }

    /**
     * Constructs a category axis, using default values where necessary.
     *
     * @param label the axis label (<code>null</code> permitted).
     * @param eachTickToShow
     */
    public MyCategoryAxis(String label, int eachTickToShow) {
        super(label);
        this.eachTickToShow = eachTickToShow;
    }

//    @Override
//    public void drawTickMarks(Graphics2D g2, double cursor,
//            Rectangle2D dataArea, RectangleEdge edge, AxisState state) {
//
//        Plot p = getPlot();
//        if (p == null) {
//            return;
//        }
//        CategoryPlot plot = (CategoryPlot) p;
//        double il = getTickMarkInsideLength();
//        double ol = getTickMarkOutsideLength();
//        Line2D line = new Line2D.Double();
//        List<Comparable> categories = plot.getCategoriesForAxis(this);
//        g2.setPaint(getTickMarkPaint());
//        g2.setStroke(getTickMarkStroke());
//        Object saved = g2.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL);
//        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
//                RenderingHints.VALUE_STROKE_NORMALIZE);
//        int counter = 0;
//        boolean draw;
//        if (edge.equals(RectangleEdge.TOP)) {
//            for (Comparable key : categories) {
//                draw = counter % eachTickToShow == 0;
//                if (draw) {
//                    double x = getCategoryMiddle(key, categories, dataArea, edge);
//                    line.setLine(x, cursor, x, cursor + il);
//                    g2.draw(line);
//                    line.setLine(x, cursor, x, cursor - ol);
//                    g2.draw(line);
//                }
//                counter++;
//            }
//            state.cursorUp(ol);
//        } else if (edge.equals(RectangleEdge.BOTTOM)) {
//            for (Comparable key : categories) {
//                draw = counter % eachTickToShow == 0;
//                if (draw) {
//                    double x = getCategoryMiddle(key, categories, dataArea, edge);
//                    line.setLine(x, cursor, x, cursor - il);
//                    g2.draw(line);
//                    line.setLine(x, cursor, x, cursor + ol);
//                    g2.draw(line);
//                }
//                counter++;
//            }
//            state.cursorDown(ol);
//        } else if (edge.equals(RectangleEdge.LEFT)) {
//            for (Comparable key : categories) {
//                draw = counter % eachTickToShow == 0;
//                if (draw) {
//                    double y = getCategoryMiddle(key, categories, dataArea, edge);
//                    line.setLine(cursor, y, cursor + il, y);
//                    g2.draw(line);
//                    line.setLine(cursor, y, cursor - ol, y);
//                    g2.draw(line);
//                }
//                counter++;
//            }
//            state.cursorLeft(ol);
//        } else if (edge.equals(RectangleEdge.RIGHT)) {
//            for (Comparable key : categories) {
//                draw = counter % eachTickToShow == 0;
//                if (draw) {
//                    double y = getCategoryMiddle(key, categories, dataArea, edge);
//                    line.setLine(cursor, y, cursor - il, y);
//                    g2.draw(line);
//                    line.setLine(cursor, y, cursor + ol, y);
//                    g2.draw(line);
//                }
//                counter++;
//            }
//            state.cursorRight(ol);
//        }
//        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, saved);
//    }

    @Override
    protected AxisState drawCategoryLabels(Graphics2D g2, Rectangle2D plotArea,
            Rectangle2D dataArea, RectangleEdge edge, AxisState state,
            PlotRenderingInfo plotState) {

        if (state == null) {
            throw new NullPointerException();
        }
        if (!isTickLabelsVisible()) {
            return state;
        }
        List<CategoryTick> ticks = refreshTicks(g2, state, plotArea, edge);
        //state.setTicks(ticks);        
        //FIXME MMC had to remove this as the types don't match

        int categoryIndex = 0;
        int myCategoryLabelPositionOffset = getCategoryLabelPositionOffset();
        for (CategoryTick tick : ticks) {

            g2.setFont(getTickLabelFont(tick.getCategory()));
            g2.setPaint(getTickLabelPaint(tick.getCategory()));

            CategoryLabelPosition position = getCategoryLabelPositions().getLabelPosition(edge);
            double x0 = 0.0;
            double x1 = 0.0;
            double y0 = 0.0;
            double y1 = 0.0;
            if (edge == RectangleEdge.TOP) {
                x0 = getCategoryStart(categoryIndex, ticks.size(), dataArea,
                        edge);
                x1 = getCategoryEnd(categoryIndex, ticks.size(), dataArea,
                        edge);
                y1 = state.getCursor() - myCategoryLabelPositionOffset;
                y0 = y1 - state.getMax();
            } else if (edge == RectangleEdge.BOTTOM) {
                x0 = getCategoryStart(categoryIndex, ticks.size(), dataArea,
                        edge);
                x1 = getCategoryEnd(categoryIndex, ticks.size(), dataArea,
                        edge);
                y0 = state.getCursor() + myCategoryLabelPositionOffset;
                y1 = y0 + state.getMax();
            } else if (edge == RectangleEdge.LEFT) {
                y0 = getCategoryStart(categoryIndex, ticks.size(), dataArea,
                        edge);
                y1 = getCategoryEnd(categoryIndex, ticks.size(), dataArea,
                        edge);
                x1 = state.getCursor() - myCategoryLabelPositionOffset;
                x0 = x1 - state.getMax();
            } else if (edge == RectangleEdge.RIGHT) {
                y0 = getCategoryStart(categoryIndex, ticks.size(), dataArea,
                        edge);
                y1 = getCategoryEnd(categoryIndex, ticks.size(), dataArea,
                        edge);
                x0 = state.getCursor() + myCategoryLabelPositionOffset;
                x1 = x0 - state.getMax();
            }
            Rectangle2D area = new Rectangle2D.Double(x0, y0, (x1 - x0),
                    (y1 - y0));
            Point2D anchorPoint = position.getCategoryAnchor().getAnchorPoint(area);
            TextBlock block = tick.getLabel();
            if (categoryIndex % eachTickToShow == 0) {
                block.draw(g2, (float) anchorPoint.getX(),
                        (float) anchorPoint.getY(), position.getLabelAnchor(),
                        (float) anchorPoint.getX(), (float) anchorPoint.getY(),
                        position.getAngle());
            }
            Shape bounds = block.calculateBounds(g2,
                    (float) anchorPoint.getX(), (float) anchorPoint.getY(),
                    position.getLabelAnchor(), (float) anchorPoint.getX(),
                    (float) anchorPoint.getY(), position.getAngle());
            if (plotState != null && plotState.getOwner() != null) {
                EntityCollection entities
                        = plotState.getOwner().getEntityCollection();
                if (entities != null) {
                    String tooltip = getCategoryLabelToolTip(
                            tick.getCategory());
                    String url = getCategoryLabelURL(tick.getCategory());
                    entities.add(new CategoryLabelEntity(tick.getCategory(),
                            bounds, tooltip, url));
                }
            }
            categoryIndex++;
        }

        if (edge.equals(RectangleEdge.TOP)) {
            double h = state.getMax() + myCategoryLabelPositionOffset;
            state.cursorUp(h);
        } else if (edge.equals(RectangleEdge.BOTTOM)) {
            double h = state.getMax() + myCategoryLabelPositionOffset;
            state.cursorDown(h);
        } else if (edge == RectangleEdge.LEFT) {
            double w = state.getMax() + myCategoryLabelPositionOffset;
            state.cursorLeft(w);
        } else if (edge == RectangleEdge.RIGHT) {
            double w = state.getMax() + myCategoryLabelPositionOffset;
            state.cursorRight(w);
        }
        return state;
    }

}
