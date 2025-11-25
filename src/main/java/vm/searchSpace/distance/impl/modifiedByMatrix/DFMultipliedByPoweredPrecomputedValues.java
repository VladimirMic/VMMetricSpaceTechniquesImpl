package vm.searchSpace.distance.impl.modifiedByMatrix;

import vm.searchSpace.Dataset;
import vm.searchSpace.distance.storedPrecomputedDistances.MainMemoryStoredPrecomputedDistances;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class DFMultipliedByPoweredPrecomputedValues<T> extends DFBasicAdjustedByPrecomputedValues<T> {

    public static final String NAME = "Distance Multiplied by Powered Ratio of Closest Pivots";
    private final float base;
    private final float power;

    public DFMultipliedByPoweredPrecomputedValues(Dataset dataset, MainMemoryStoredPrecomputedDistances distsHolder, float[][] weights, float base, float power) {
        super(dataset, distsHolder, weights, null);
        this.base = base;
        this.power = power;
    }

    @Override
    protected float modifyDist(float orig, float weight) {
        return (float) (orig * Math.pow((base + weight), power));
    }

    @Override
    public String getName() {
        return NAME + "(" + base + "," + power + ")";
    }

}
