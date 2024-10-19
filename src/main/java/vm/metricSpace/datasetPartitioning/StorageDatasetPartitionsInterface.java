package vm.metricSpace.datasetPartitioning;

import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;

/**
 *
 * @author Vlada
 */
public interface StorageDatasetPartitionsInterface {

    public void store(Map<Comparable, Collection<Comparable>> mapping, String datasetName, int origPivotCount);

    public Map<Comparable, TreeSet<Comparable>> load(String datasetName, int origPivotCount);
}
