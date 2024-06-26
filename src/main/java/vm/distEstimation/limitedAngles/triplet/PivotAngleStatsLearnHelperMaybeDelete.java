package vm.distEstimation.limitedAngles.triplet;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.math.Tools;

/**
 *
 * @author Vlada
 */
public class PivotAngleStatsLearnHelperMaybeDelete {

    /**
     * All sizes of angles are rounded with this granularity for printing. The
     * coefficients are calculated precisely. Use max 2 floating numbers.
     */
    public static final Float ANGLE_SIZE_GRANULARITY_DEG = 1f;

    /**
     * Number of nearest neighbour considered for each pivot
     */
    private final int k;

    /**
     * Number of nearest neighbour considered for each pivot
     */
    public static final Integer IMPLICIT_K = 40000;

    /**
     * Identifier of this pivot p
     */
    private final Comparable pivotId;

    /**
     * Stores (encoded) sampled angles in a triangles randomly sampled from the
     * data
     */
    private final Set<String> anglesInAllTriangles;

    /**
     * Stores (encoded) sampled angles in a triangles of this pivot p, object o,
     * and query q where o is one of the nearest neighbours of q. They key is
     * the distance of q and o, and value is the encoded angles in a triangle
     * The number key-value pairs is up to k.
     */
    private final SortedSet<Map.Entry<Float, String>> nnTriangles;

    private final String output;

    private static final Logger LOGGER = Logger.getLogger(PivotAngleStatsLearnHelperMaybeDelete.class.getName());

    public PivotAngleStatsLearnHelperMaybeDelete(Comparable pivotId) {
        this(pivotId, null);
    }

    public PivotAngleStatsLearnHelperMaybeDelete(Comparable pivotId, String output) {
        this(pivotId, IMPLICIT_K, output);
    }

    public PivotAngleStatsLearnHelperMaybeDelete(Comparable pivotId, int k) {
        this(pivotId, k, null);
    }

    public PivotAngleStatsLearnHelperMaybeDelete(Comparable pivotId, int k, String output) {
        this.pivotId = pivotId;
        anglesInAllTriangles = new HashSet<>();
        nnTriangles = new TreeSet<>((Map.Entry<Float, String> o1, Map.Entry<Float, String> o2) -> {
            float dist1 = o1.getKey();
            float dist2 = o2.getKey();
            if (dist1 != dist2) {
                return Float.compare(dist1, dist2);
            }
            return o1.getValue().compareTo(o2.getValue());
        });
        this.k = k;
        this.output = output;
    }

    public void registerNextTriangleWithThisPivot(float distPO, float distPQ, float distQO) {
        if (distPO == 0 || distPQ == 0 || distQO == 0) {
            return;
        }
        float a = Math.min(distPO, distPQ);
        float b = Math.max(distPO, distPQ);
        float[] angles = Tools.evaluateAnglesOfTriangle(a, b, distQO, true);
        if (angles[0] == 0) {
            String s = "";
        }
        String encodedAngles = Tools.floorToGranularity(angles[0], ANGLE_SIZE_GRANULARITY_DEG) + ";" + Tools.floorToGranularity(angles[1], ANGLE_SIZE_GRANULARITY_DEG);
        anglesInAllTriangles.add(encodedAngles);
        Map.Entry<Float, String> e = new AbstractMap.SimpleEntry<>(distQO, encodedAngles + ";" + a + ";" + b + ";" + distQO);
        nnTriangles.add(e);
        if (nnTriangles.size() > k) {
            nnTriangles.remove(nnTriangles.last());
        }
    }

    private String adjustCoefficients(SortedSet<Map.Entry<Float, String>> nnTriangles) {
        float cLbDiff = Float.MAX_VALUE;
        float cLbSum = Float.MAX_VALUE;
        float cUbSum = 0;
        String[] extremeTriangles = new String[3];
        Iterator<Map.Entry<Float, String>> it = nnTriangles.iterator();
        while (it.hasNext()) {
            String trianglesWithNN = it.next().getValue();
            String[] angles = trianglesWithNN.split(";");
            float a = Float.parseFloat(angles[2]);
            float b = Float.parseFloat(angles[3]);
            float c = Float.parseFloat(angles[4]);
            float sumRatio = c / (a + b);
            float diffRatio = c / (b - a);
            if (!Float.isNaN(diffRatio) && diffRatio < cLbDiff) {
                cLbDiff = diffRatio;
                extremeTriangles[0] = angles[0] + ";" + angles[1];
            }
            if (sumRatio < cLbSum) {
                cLbSum = sumRatio;
                extremeTriangles[1] = angles[0] + ";" + angles[1];;
            }
            if (sumRatio > cUbSum) {
                cUbSum = sumRatio;
                extremeTriangles[2] = angles[0] + ";" + angles[1];;
            }
        }
        return cLbDiff + ";" + cLbSum + ";" + cUbSum + ";" + extremeTriangles[0] + ";" + extremeTriangles[1] + ";" + extremeTriangles[2];
    }

    public void print() throws FileNotFoundException {
        String coeffs = adjustCoefficients(nnTriangles);
        SortedSet<String> nnPointsForPrint = removeOverlaping(nnTriangles);
        PrintStream ps = output == null ? System.out : new PrintStream(new FileOutputStream(output + "_" + k + "k.csv", true));
//        PrintStream ps = output == null ? System.out : new PrintStream(new FileOutputStream(output + "_" + pivotId + "_" + k + ".csv", true));
        ps.print("Alpha " + pivotId + ";Beta " + pivotId + ";NN alpha " + pivotId + ";NN beta " + pivotId + ";C_LB_diff;C_LB_sum;C_UB_sum");
        ps.print(";C_LB_diff alpha;C_LB_diff beta");
        ps.print(";C_LB_sum alpha;C_LB_sum beta");
        ps.println(";C_UB_sum alpha;C_UB_sum beta");
        Iterator<String> it = anglesInAllTriangles.iterator();
        for (int i = 0; it.hasNext(); i++) {
            String colAcolB = it.next();
            if (!nnPointsForPrint.isEmpty()) {
                String first = nnPointsForPrint.first();
                colAcolB += ";" + first;
                nnPointsForPrint.remove(first);
            }
            if (i == 0) {
                colAcolB += ";" + coeffs;
            }
            ps.println(colAcolB);
        }
        ps.flush();
        if (output != null) {
            ps.close();
        }
        LOGGER.log(Level.INFO, "Finished pivot {0}", pivotId);
    }

    private SortedSet<String> removeOverlaping(SortedSet<Map.Entry<Float, String>> nnTriangles) {
        SortedSet<String> ret = new TreeSet();
        for (Map.Entry<Float, String> nnTriangle : nnTriangles) {
            String[] angles = nnTriangle.getValue().split(";");
            ret.add(angles[0] + ";" + angles[1]);
        }
        return ret;
    }

}
