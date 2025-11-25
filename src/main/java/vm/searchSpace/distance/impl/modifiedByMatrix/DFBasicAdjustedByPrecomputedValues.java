package vm.searchSpace.distance.impl.modifiedByMatrix;

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
    public DFBasicAdjustedByPrecomputedValues(Dataset dataset, MainMemoryStoredPrecomputedDistances distsHolder, float[][] weights, String name) {
        super(dataset, distsHolder, name);
        this.origDists = Tools.copyArray(distsHolder.getDists());
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights[i].length; j++) {
                float newDist = 1 - weights[i][j];
                distsHolder.modify(i, j, newDist);
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
