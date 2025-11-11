package vm.searchSpace.distance.impl;

import vm.searchSpace.Dataset;
import vm.searchSpace.distance.storedPrecomputedDistances.MainMemoryStoredPrecomputedDistances;

/**
 *
 * @author au734419
 * @param <T>
 */
public class DFAdjustedByCertaintyWithPrecomputedValues<T> extends DFWithPrecomputedValues<T> {

    private double power = 2;
    public static final String NAME_PREFFIX = "AdjustedByCertaintySym_pow";

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
        float max = -1;
        float[][] dists = distsHolder.getDists();
        for (int i = 0; i < dists.length; i++) {
            for (int j = 0; j < dists[i].length; j++) {
                float newDist = (float) (dists[i][j] * (1 + Math.pow(1 - weights[i][j], power)));
                max = Math.max(max, newDist);
                distsHolder.modify(i, j, newDist);
            }
        }
        for (int i = 0; i < dists.length; i++) {
            for (int j = 0; j < dists[i].length; j++) {
                float newDist = dists[i][j] / max;
                distsHolder.modify(i, j, newDist);
            }
        }
    }

    public DFAdjustedByCertaintyWithPrecomputedValues(Dataset dataset, MainMemoryStoredPrecomputedDistances distsHolder, double power) {
        super(dataset, distsHolder, NAME_PREFFIX);
        this.power = power;
    }

    @Override
    public String getSuffix() {
        return Double.toString(power);
    }

}
