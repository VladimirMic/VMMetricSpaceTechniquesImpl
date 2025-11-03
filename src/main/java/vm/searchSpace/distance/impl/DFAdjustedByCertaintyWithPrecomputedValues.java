package vm.searchSpace.distance.impl;

import vm.searchSpace.Dataset;
import vm.searchSpace.distance.storedPrecomputedDistances.MainMemoryStoredPrecomputedDistances;

/**
 *
 * @author au734419
 * @param <T>
 */
public class DFAdjustedByCertaintyWithPrecomputedValues<T> extends DFWithPrecomputedValues<T> {

    public static final double POWER = 2;
    public static final String NAME = "AdjustedByCertainty";

    /**
     *
     * @param dataset
     * @param distsHolder
     * @param weights symmetric!
     */
    public DFAdjustedByCertaintyWithPrecomputedValues(Dataset dataset, MainMemoryStoredPrecomputedDistances distsHolder, float[][] weights) {
        super(dataset, distsHolder, NAME);
        float[][] dists = distsHolder.getDists();
        for (int i = 0; i < dists.length; i++) {
            for (int j = 0; j < dists[i].length; j++) {
                float newDist = (float) (dists[i][j] * (1 + Math.pow(1 - weights[i][j], DFAdjustedByCertaintyWithPrecomputedValues.POWER)));
                distsHolder.modify(i, j, newDist);
            }
        }
    }

}
