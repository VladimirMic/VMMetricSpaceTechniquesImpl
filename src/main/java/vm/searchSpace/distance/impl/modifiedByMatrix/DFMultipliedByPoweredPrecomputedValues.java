package vm.searchSpace.distance.impl.modifiedByMatrix;

import vm.searchSpace.Dataset;
import vm.searchSpace.distance.storedPrecomputedDistances.MainMemoryStoredPrecomputedDistances;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class DFMultipliedByPoweredPrecomputedValues<T> extends DFBasicAdjustedByPrecomputedValues<T> {

    public static final String NAME = "Multiplied by Powered Div To Best Pivot";
    private final float addition;
    private final float power;

    public DFMultipliedByPoweredPrecomputedValues(Dataset dataset, MainMemoryStoredPrecomputedDistances distsHolder, MainMemoryStoredPrecomputedDistances weights, float addition, float power) {
        super(dataset, distsHolder, weights, null);
        this.addition = addition;
        this.power = power;
    }

    @Override
    protected float modifyDist(float orig, float weight) {
        return (float) (orig * Math.pow((addition + weight), power));
    }

    @Override
    public String getName() {
        return NAME + "(" + addition + "," + power + ")";
    }

}
