package vm.metricSpace.distance.storedPrecomputedDistances;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import vm.metricSpace.distance.bounding.onepivot.learning.LearningTriangleInequalityWithLimitedAngles;

/**
 *
 * @author Vlada
 */
public abstract class AbstractPrecomputedPairsOfDistancesStorage {

    public static final Integer SAMPLE_SET_SIZE = 10000;
    public static final Integer SAMPLE_QUERY_SET_SIZE = 1000;
    /**
     * Number of stored minimum distances
     */
    public static final Integer IMPLICIT_K = (int) (LearningTriangleInequalityWithLimitedAngles.RATIO_OF_SMALLEST_DISTS * SAMPLE_SET_SIZE * SAMPLE_QUERY_SET_SIZE);

    public abstract void storePrecomputedDistances(TreeSet<Map.Entry<String, Float>> dists);

    public abstract TreeSet<Map.Entry<String, Float>> loadPrecomputedDistances();

    public static Set<Comparable> getIDsOfObjects(TreeSet<Map.Entry<String, Float>> smallestDists) {
        Set ret = new HashSet();
        for (Map.Entry<String, Float> smallestDist : smallestDists) {
            String[] split = smallestDist.getKey().split(";");
            ret.add(split[0]);
            ret.add(split[1]);
        }
        return ret;
    }

}
