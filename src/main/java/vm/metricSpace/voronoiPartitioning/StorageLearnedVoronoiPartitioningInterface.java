package vm.metricSpace.voronoiPartitioning;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 *
 * @author Vlada
 */
public interface StorageLearnedVoronoiPartitioningInterface {

    public void store(Map<Object, SortedSet<Object>> mapping, String datasetName, int origPivotCount);

    public Map<Object, TreeSet<Object>> load(String datasetName, int origPivotCount);
}
