package vm.metricSpace.distance.bounding.nopivot.impl;

import java.util.Arrays;
import java.util.Map;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.metricSpace.distance.impl.HammingDistanceLongs;
import vm.metricSpace.distance.bounding.nopivot.NoPivotFilter;

/**
 *
 * @author Vlada
 */
public class SecondaryFilteringWithSketches extends NoPivotFilter {

    private final Map<Object, long[]> sketches;
    private final DistanceFunctionInterface hamDistFunc;
    private final double[] primDistsThreshold;
    private final int[] hamDistsThresholds;

    public SecondaryFilteringWithSketches(String namePrefix, Map<Object, long[]> sketches, double[] primDistsThreshold, int[] hamDistsThresholds) {
        super(namePrefix);
        this.sketches = sketches;
        this.hamDistFunc = new HammingDistanceLongs();
        this.primDistsThreshold = primDistsThreshold;
        this.hamDistsThresholds = hamDistsThresholds;
    }

    public float lowerBound(long[] querySketch, Object oID, float searchRadius) {
        int pos = Arrays.binarySearch(primDistsThreshold, searchRadius);
        if (pos < 0) {
            pos = -pos - 1;
        }
        if (pos < hamDistsThresholds.length) {
            long[] sk = sketches.get(oID);
            if (sk != null) {
                float hamDist = hamDistFunc.getDistance(querySketch, sk);
                if (hamDist >= hamDistsThresholds[pos]) {
                    return Float.MAX_VALUE;
                }
            }
        }
        return 0;
    }

    @Override
    public float lowerBound(Object... args) {
        long[] querySketch = (long[]) args[0];
        Object oID = args[1];
        Float rad = (Float) args[2];
        return lowerBound(querySketch, oID, rad);
    }

    @Override
    public float upperBound(Object... args) {
        return Float.MAX_VALUE;
    }

    @Override
    protected String getTechName() {
        return "Secondary_filtering_with_sketches";
    }

}
