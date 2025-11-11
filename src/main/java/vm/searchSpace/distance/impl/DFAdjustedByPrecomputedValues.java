package vm.searchSpace.distance.impl;

import vm.datatools.Tools;
import vm.searchSpace.Dataset;
import vm.searchSpace.distance.storedPrecomputedDistances.MainMemoryStoredPrecomputedDistances;

/**
 *
 * @author au734419
 * @param <T>
 */
public class DFAdjustedByPrecomputedValues<T> extends DFWithPrecomputedValues<T> {

    public static final String NAME_PREFFIX = "DFAdjustedByPrecomputedValues";
    public final float[][] origDists;

    /**
     *
     * @param dataset
     * @param distsHolder
     * @param weights
     */
    public DFAdjustedByPrecomputedValues(Dataset dataset, MainMemoryStoredPrecomputedDistances distsHolder, float[][] weights) {
        super(dataset, distsHolder, NAME_PREFFIX);
        this.origDists = Tools.copyArray(distsHolder.getDists());
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights[i].length; j++) {
                float newDist = 1 - weights[i][j];
                distsHolder.modify(i, j, newDist);
            }
        }
    }

    @Override
    public String getSuffix() {
        return "";
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
