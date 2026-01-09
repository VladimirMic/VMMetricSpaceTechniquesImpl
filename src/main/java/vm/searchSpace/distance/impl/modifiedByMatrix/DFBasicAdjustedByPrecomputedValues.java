package vm.searchSpace.distance.impl.modifiedByMatrix;

import java.util.Map;
import vm.datatools.Tools;
import vm.searchSpace.Dataset;
import vm.searchSpace.distance.impl.DFWithPrecomputedValues;
import vm.searchSpace.distance.storedPrecomputedDistances.MainMemoryStoredPrecomputedDistances;

/**
 *
 * @author au734419
 * @param <T>
 */
public class DFBasicAdjustedByPrecomputedValues<T> extends DFWithPrecomputedValues<T> {

    public final float[][] origDists;

    /**
     *
     * @param dataset
     * @param distsHolder
     * @param weights
     * @param name
     */
    public DFBasicAdjustedByPrecomputedValues(Dataset dataset, MainMemoryStoredPrecomputedDistances distsHolder, MainMemoryStoredPrecomputedDistances weights, String name) {
        super(dataset, distsHolder, name);
        this.origDists = Tools.copyArray(distsHolder.getDists());
        Map<Comparable, Integer> origRows = distsHolder.getRowHeaders();
        Map<Comparable, Integer> origColumns = distsHolder.getColumnHeaders();
        Map<Comparable, Integer> newRows = weights.getRowHeaders();
        Map<Comparable, Integer> newColumns = weights.getColumnHeaders();
        float[][] newDists = weights.getDists();
        for (Map.Entry<Comparable, Integer> origRow : origRows.entrySet()) {
            Comparable origRowKey = origRow.getKey();
            int origRowIdx = origRow.getValue();
            int newRowIdx = newRows.get(origRowKey);
            for (Map.Entry<Comparable, Integer> origColumn : origColumns.entrySet()) {
                Comparable origColumnKey = origColumn.getKey();
                int origColumnIdx = origColumn.getValue();
                int newColumnIdx = newColumns.get(origColumnKey);
                float newDist = newDists[newRowIdx][newColumnIdx];
                distsHolder.modify(origRowIdx, origColumnIdx, newDist);
            }
        }
    }

    @Override
    public float getDistance(T obj1, T obj2) {
        Comparable o1ID = Tools.hashArray(obj1);
        Comparable o2ID = Tools.hashArray(obj2);
        String o1IDString = o1ID.toString();
        String o2IDString = o2ID.toString();
        if (newColumnHeaders.containsKey(o1IDString) && newRowHeaders.containsKey(o2IDString)) {
            int o1idx = newColumnHeaders.get(o1IDString);
            int o2idx = newRowHeaders.get(o2IDString);
            float weight = distsHolder.getDists()[o1idx][o2idx];
            float orig = origDists[o1idx][o2idx];
            return modifyDist(orig, weight);
        }
        throw new IllegalArgumentException();
    }

    public float getOrigDistance(T obj1, T obj2) {
        Comparable o1ID = Tools.hashArray(obj1);
        Comparable o2ID = Tools.hashArray(obj2);
        String o1IDString = o1ID.toString();
        String o2IDString = o2ID.toString();
        if (newColumnHeaders.containsKey(o1IDString) && newRowHeaders.containsKey(o2IDString)) {
            int o1idx = newColumnHeaders.get(o1IDString);
            int o2idx = newRowHeaders.get(o2IDString);
            float orig = origDists[o1idx][o2idx];
            return orig;
        }
        return df.getDistance(obj1, obj2);
    }

    /**
     * Override this method to implement acutla dist modifications.
     *
     * @param orig
     * @param weight
     * @return
     */
    protected float modifyDist(float orig, float weight) {
        return weight;
    }

}
