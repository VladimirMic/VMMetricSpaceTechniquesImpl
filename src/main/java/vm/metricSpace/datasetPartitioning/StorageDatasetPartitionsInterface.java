package vm.metricSpace.datasetPartitioning;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import vm.datatools.DataTypeConvertor;

/**
 *
 * @author Vlada
 */
public abstract class StorageDatasetPartitionsInterface {

    public void store(Map<Comparable, Collection<Comparable>> mapping, String datasetName, int origPivotCount) {
        store(mapping, datasetName, null, origPivotCount);
    }

    public abstract void store(Map<Comparable, Collection<Comparable>> mapping, String datasetName, String filterName, int origPivotCount);

    public Map<Comparable, Collection<Comparable>> load(String datasetName, int origPivotCount) {
        return load(datasetName, null, origPivotCount);
    }

    public Map<Comparable, TreeSet<Comparable>> loadAsTreeSets(String datasetName, int origPivotCount) {
        return loadAsTreeSets(datasetName, null, origPivotCount);
    }

    public Map<Comparable, TreeSet<Comparable>> loadAsTreeSets(String datasetName, String suffix, int origPivotCount) {
        Map<Comparable, Collection<Comparable>> load = load(datasetName, suffix, origPivotCount);
        return transformToTreeSets(load);
    }

    protected Map<Comparable, TreeSet<Comparable>> transformToTreeSets(Map<Comparable, Collection<Comparable>> loaded) {
        Map<Comparable, TreeSet<Comparable>> ret = new HashMap<>();
        for (Map.Entry<Comparable, Collection<Comparable>> entry : loaded.entrySet()) {
            TreeSet<Comparable> cell = DataTypeConvertor.castCell(entry.getValue());
            ret.put(entry.getKey(), cell);
        }
        return ret;
    }

    public abstract Map<Comparable, Collection<Comparable>> load(String datasetName, String suffix, int origPivotCount);
}
