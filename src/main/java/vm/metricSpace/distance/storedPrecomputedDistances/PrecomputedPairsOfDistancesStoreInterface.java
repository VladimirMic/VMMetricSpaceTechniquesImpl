package vm.metricSpace.distance.storedPrecomputedDistances;

import java.util.Map;
import java.util.TreeSet;
import vm.metricSpace.distance.bounding.onepivot.learning.LearningTriangleInequalityWithLimitedAngles;

/**
 *
 * @author Vlada
 */
public interface PrecomputedPairsOfDistancesStoreInterface {

    public static final Integer SAMPLE_SET_SIZE = 10000;
    public static final Integer SAMPLE_QUERY_SET_SIZE = 1000;
    /**
     * Number of stored minimum distances
     */
    public static final Integer IMPLICIT_K = (int) (LearningTriangleInequalityWithLimitedAngles.RATIO_OF_SMALLEST_DISTS * SAMPLE_SET_SIZE * SAMPLE_QUERY_SET_SIZE);

    public void storePrecomputedDistances(TreeSet<Map.Entry<String, Float>> dists);

    public TreeSet<Map.Entry<String, Float>> loadPrecomputedDistances();
}
