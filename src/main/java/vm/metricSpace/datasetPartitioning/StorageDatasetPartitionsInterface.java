package vm.metricSpace.datasetPartitioning;

import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;

/**
 *
 * @author Vlada
 */
public abstract class StorageDatasetPartitionsInterface {

    public void store(Map<Comparable, Collection<Comparable>> mapping, String datasetName, int origPivotCount) {
        store(mapping, datasetName, null, origPivotCount);;
    }

    public abstract void store(Map<Comparable, Collection<Comparable>> mapping, String datasetName, String filterName, int origPivotCount);

    public Map<Comparable, TreeSet<Comparable>> load(String datasetName, int origPivotCount) {
        return load(datasetName, null, origPivotCount);
    }

    public abstract Map<Comparable, TreeSet<Comparable>> load(String datasetName, String suffix, int origPivotCount);
}
