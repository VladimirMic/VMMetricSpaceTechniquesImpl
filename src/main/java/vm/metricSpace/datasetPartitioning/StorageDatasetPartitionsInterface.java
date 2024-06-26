package vm.metricSpace.datasetPartitioning;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 *
 * @author Vlada
 */
public interface StorageDatasetPartitionsInterface {

    public void store(Map<Comparable, SortedSet<Comparable>> mapping, String datasetName, int origPivotCount);

    public Map<Comparable, TreeSet<Comparable>> load(String datasetName, int origPivotCount);
}
