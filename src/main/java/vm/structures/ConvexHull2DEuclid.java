package vm.structures;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Vladimir Mic using the code of Arnav Kr. Mandal
 * url{https://www.geeksforgeeks.org/convex-hull-set-1-jarviss-algorithm-or-wrapping/}
 * WARNING: quantise the coordinates a little bit! Due to rounding of the
 * doubles, the evaluation may end up with an error, preventing infinite loop.
 */
public class ConvexHull2DEuclid implements Serializable {

    /**
     * Class id for serialization.
     */
    private static final long serialVersionUID = 4654243L;

    private static final transient Logger LOGGER = Logger.getLogger(ConvexHull2DEuclid.class.getName());

    private transient final TreeSet<Point2D.Double> points;
    private List<Point2D.Double> hull = null;

    public List<Point2D.Double> getHull() {
        return hull;
    }

    public ConvexHull2DEuclid() {
        points = new TreeSet<>((Point2D.Double o1, Point2D.Double o2) -> {
            if (o1.x != o2.x) {
                return Double.compare(o1.x, o2.x);
            }
            return Double.compare(o1.y, o2.y);
        });
    }

    public ConvexHull2DEuclid(String csvString) {
        this();
        String[] split = csvString.split(",");
        for (int i = 0; i < split.length; i = i + 2) {
            float x = Float.parseFloat(split[i]);
            float y = Float.parseFloat(split[i + 1]);
            addPoint(x, y, false);
        }
    }

    public static String getPointsAsCSVLine(List<Point2D.Double> hull) {
        String ret = "";
        if (hull.isEmpty()) {
            return ret;
        }
        for (Point2D.Double point : hull) {
            ret += point.x + "," + point.y + ",";
        }
        return ret.substring(0, ret.length() - 1);
    }

    public void printAsCoordinatesInColumns() {
        List<Point2D.Double> points = getHull();
        if (points == null) {
            return;
        }
        for (Point2D.Double point : points) {
            System.out.println(point.x + "," + point.y);
        }
        Point2D.Double point = points.get(0);
        System.out.println(point.x + "," + point.y);
    }

    @Override
    public String toString() {
        return getPointsAsCSVLine(evaluateHull(true));
    }

    public void addPoint(double x, double y, boolean symmetric) {
        points.add(new Point2D.Double(x, y));
        if (points.size() > 50000) {
            LOGGER.log(Level.INFO, "Registered {0} points. Starting hull evaluation to dismiss irrelevant points", points.size());
            evaluateHull(symmetric);
        }
    }

    /**
     * Finds orientation of ordered triplet (p, q, r)
     *
     * @param p first point
     * @param q second point
     * @param r third point
     * @return 0 iff p, q and r are colinear; -1 iff they are clockwise; 1 iff
     * they are counterclockwise;
     *
     */
    public static final int orientation(Point2D.Double p, Point2D.Double q, Point2D.Double r) {
        if (p.equals(q) || q.equals(r) || r.equals(p)) {
            return 0;
        }
        double a = q.y - p.y;
        double b = r.x - q.x;
        double c = q.x - p.x;
        double d = r.y - q.y;
        double val = a * b - c * d;
        if (val == 0) {
            return 0;
        }
        return (val > 0) ? -1 : 1;
    }

    /**
     * Finds orientation of ordered triplet (p, q, r)
     *
     * @param p first point
     * @param q second point
     * @param r third point
     * @return 0 iff p, q and r are colinear; -1 iff they are clockwise; 1 iff
     * they are counterclockwise;
     *
     */
    public static final int orientation(float[] p, float[] q, float[] r) {
        if (Arrays.equals(p, q) || Arrays.equals(q, r) || Arrays.equals(r, p)) {
            return 0;
        }
        float val = (q[1] - p[1]) * (r[0] - q[0]) - (q[0] - p[0]) * (r[1] - q[1]);
        if (val == 0) {
            return 0;
        }
        return (val > 0) ? -1 : 1;
    }

    public List<Point2D.Double> evaluateHull() {
        return evaluateHull(false);
    }

    private List<Point2D.Double> evaluateHull(boolean symmetricXandY) {
        if (hull != null) {
            points.addAll(hull);
        }
        if (points.size() < 3) {
            return new ArrayList<>(points);
        }
        Point2D.Double[] pointsArray = new Point2D.Double[points.size()];
        List<Point2D.Double> hullAsPoints = evaluateHull(points.toArray(pointsArray));
        if (!symmetricXandY) {
            return hullAsPoints;
        }
        // add symmetric
        for (Point2D.Double point : hullAsPoints) {
            points.add(new Point2D.Double(point.y, point.x));
        }
        pointsArray = new Point2D.Double[points.size()];
        return evaluateHull(points.toArray(pointsArray));
    }

    public boolean coversPoint(Point2D.Float point) {
        Point2D.Double[] pointsArray = new Point2D.Double[points.size()];
        List<Point2D.Double> hullAsPoints = evaluateHull(points.toArray(pointsArray));
        // add symmetric
        for (Point2D.Double p : hullAsPoints) {
            points.add(new Point2D.Double(p.y, p.x));
        }
        pointsArray = new Point2D.Double[points.size()];
        hullAsPoints = evaluateHull(points.toArray(pointsArray));
        return !hullAsPoints.contains(point);
    }

    private List<Point2D.Double> evaluateHull(Point2D.Double[] pointsArray) {
        LOGGER.log(Level.INFO, "Starting hull evaluation");

        List<Point2D.Double> ret = new ArrayList<>();
        // Start from leftmost point, keep moving
        // counterclockwise until reach the start point
        // again. Since points are sorted, the left most point has index 0.
        int vertex = 0;
        int curr;
        do {
            // Add current point to result
            Point2D.Double add = pointsArray[vertex];
            if (ret.contains(add) || add == null) {
                LOGGER.log(Level.SEVERE, "Error: hull already contains point {0}, {1}, vertex: {2}. This happens probably due to very close points.", new Object[]{add.x, add.y, vertex});
                System.exit(-1);
            } else {
                ret.add(add);
            }

            // Search for a point 'curr' such that
            // orientation(vertex, curr, x) is counterclockwise
            // for all points 'x'. The idea is to keep
            // track of last visited most counterclock-
            // wise point in curr. If any point 'i' is more
            // counterclock-wise than curr, then update curr.
            curr = (vertex + 1) % pointsArray.length;

            for (int i = 0; i < pointsArray.length; i++) {
                // If i is more counterclockwise than current q, then update q
                if (orientation(pointsArray[vertex], pointsArray[i], pointsArray[curr]) == 1) {
                    curr = i;
                }
            }
            // curr is the most counterclockwise with respect to vertex.
            // Set vertex = q for next iteration, i.e., q is added to result 'hull'
            vertex = curr;

        } while (vertex != 0); // While we don't come to first point
        LOGGER.log(Level.INFO, "Hull evaluated");
        points.removeAll(points);
        points.addAll(ret);
        hull = ret;
        return ret;
    }

    public static final Map<String, List<Point2D.Double>> parsePivotsHulls(String path, boolean transformToRadians) {
        Map<String, List<Point2D.Double>> ret = new HashMap<>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(path));
            String line = br.readLine();
            while (line != null) {
                String[] split = line.split(";");
                ConvexHull2DEuclid hull = new ConvexHull2DEuclid(split[1]);
                hull.evaluateHull(true);

//                System.out.println(split[0]);
                hull.printAsCoordinatesInColumns();

                if (transformToRadians) {
                    ret.put(split[0], transformHullFromDegreesToRadians(hull.getHull()));
                } else {
                    ret.put(split[0], hull.getHull());
                }
                line = br.readLine();
            }
            System.out.flush();
        } catch (IOException ex) {
            Logger.getLogger(ConvexHull2DEuclid.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(ConvexHull2DEuclid.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return ret;
    }

    public static List<Point2D.Double> transformHullFromDegreesToRadians(List<Point2D.Double> hull) {
        List<Point2D.Double> ret = new ArrayList<>();
        for (int i = 0; i < hull.size(); i++) {
            Point2D.Double point = hull.get(i);
            double x = (float) degsToRad(point.x);
            double y = (float) degsToRad(point.y);
            ret.add(new Point2D.Double(x, y));
        }
        return ret;
    }

    public static void addArtificialPeakForX90(List<Point2D.Float> hullInRad) {
        for (int i = 0; i < hullInRad.size(); i++) {
            Point2D.Float start = hullInRad.get(i);
            Point2D.Float end = hullInRad.get((i + 1) % hullInRad.size());
            if (start.x < Math.PI / 2 && end.x > Math.PI / 2) {
                float[] f = evaluateEquationOfLinearFunc(start.y, end.y, start.x, end.x);
                float xToAdd = (float) (Math.PI / 2);
                float yToAdd = f[0] * xToAdd + f[1];
                Point2D.Float add = new Point2D.Float(xToAdd, yToAdd);
                if (!hullInRad.contains(add)) {
                    hullInRad.add(i + 1, add);
                    break;
                }
            }
        }
    }

    private static float[] evaluateEquationOfLinearFunc(float y1, float y2, float x1, float x2) {
        float directive = (y1 - y2) / (x1 - x2);
        float shift = y1 - directive * x1;
        return new float[]{directive, shift};
    }

    public static List<Point2D.Float> transformHullToSinCoords(List<Point2D.Float> hullInRad) {
        List<Point2D.Float> ret = new ArrayList<>();
        for (int i = 0; i < hullInRad.size(); i++) {
            Point2D.Float p = hullInRad.get(i);
            float sinX = (float) Math.sin(p.x);
            float sinY = (float) Math.sin(p.y);
            ret.add(new Point2D.Float(sinX, sinY));
        }
        return ret;
    }

    public static double degsToRad(double angleInDegrees) {
        return (angleInDegrees / 180) * Math.PI;
    }
}
