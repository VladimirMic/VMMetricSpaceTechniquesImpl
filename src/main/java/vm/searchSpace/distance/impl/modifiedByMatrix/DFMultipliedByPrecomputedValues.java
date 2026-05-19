package vm.searchSpace.distance.impl.modifiedByMatrix;

import vm.searchSpace.Dataset;
import vm.searchSpace.distance.storedPrecomputedDistances.MainMemoryStoredPrecomputedDistances;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class DFMultipliedByPrecomputedValues<T> extends DFBasicAdjustedByPrecomputedValues<T> {

    public DFMultipliedByPrecomputedValues(Dataset dataset, MainMemoryStoredPrecomputedDistances primaryDists, MainMemoryStoredPrecomputedDistances weights, String name) {
        super(dataset, primaryDists, weights, name);
    }

    @Override
    protected float modifyDist(float orig, float weight) {
        return orig * weight;
    }

}
