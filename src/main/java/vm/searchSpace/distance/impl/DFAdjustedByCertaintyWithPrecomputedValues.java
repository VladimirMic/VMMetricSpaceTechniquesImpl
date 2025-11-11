package vm.searchSpace.distance.impl;

import vm.datatools.Tools;
import vm.searchSpace.Dataset;
import vm.searchSpace.distance.storedPrecomputedDistances.MainMemoryStoredPrecomputedDistances;

/**
 *
 * @author au734419
 * @param <T>
 */
public class DFAdjustedByCertaintyWithPrecomputedValues<T> extends DFWithPrecomputedValues<T> {

    public static final String NAME_PREFFIX = "AdjustedByCertaintySym";
    private double power = 2;
    public final float[][] origDists;

    public DFAdjustedByCertaintyWithPrecomputedValues(Dataset dataset, MainMemoryStoredPrecomputedDistances distsHolder, float[][] weights) {
        this(dataset, distsHolder, weights, 2);
    }

    /**
     *
     * @param dataset
     * @param distsHolder
     * @param weights symmetric!
     * @param power
     */
    public DFAdjustedByCertaintyWithPrecomputedValues(Dataset dataset, MainMemoryStoredPrecomputedDistances distsHolder, float[][] weights, double power) {
        super(dataset, distsHolder, NAME_PREFFIX);
        this.power = power;
        this.origDists = Tools.copyArray(distsHolder.getDists());
//        float max = -1;
//        float[][] dists = distsHolder.getDists();
//        for (int i = 0; i < dists.length; i++) {
//            for (int j = 0; j < dists[i].length; j++) {
//                float newDist = (float) (dists[i][j] * (1 + Math.pow(1 - origDists[i][j], power)));
//                max = Math.max(max, newDist);
//                distsHolder.modify(i, j, newDist);
//            }
//        }
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights[i].length; j++) {
                float newDist = 1 - weights[i][j];
                distsHolder.modify(i, j, newDist);
            }
        }
    }

//    public DFAdjustedByCertaintyWithPrecomputedValues(Dataset dataset, MainMemoryStoredPrecomputedDistances origDists, float[][] origDists, double power) {
//        super(dataset, origDists, NAME_PREFFIX);
//        this.origDists = origDists;
//        this.power = power;
//    }
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
            float newDist = (float) (orig * (1 + Math.pow(weight, power)));
            return newDist;
        }
        return df.getDistance(obj1, obj2);
    }

}
