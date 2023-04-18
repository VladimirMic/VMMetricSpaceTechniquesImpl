package vm.metricSpace.distance.storedPrecomputedDistances;

import java.util.Map;
import java.util.TreeSet;

/**
 *
 * @author Vlada
 */
public interface PrecomputedPairsOfDistancesStoreInterface {

    public void storePrecomputedDistances(TreeSet<Map.Entry<String, Float>> dists);

    public TreeSet<Map.Entry<String, Float>> loadPrecomputedDistances();
}
