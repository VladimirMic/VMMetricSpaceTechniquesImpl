package vm.searchSpace.distance.storedPrecomputedDistances;

import java.util.Map;
import vm.searchSpace.Dataset;

/**
 *
 * @author xmic
 */
public class MainMemoryStoredPrecomputedDistances extends AbstractPrecomputedDistancesMatrixLoader {

    private final float[][] dists;

    public MainMemoryStoredPrecomputedDistances(float[][] dists, Map<Comparable, Integer> columnHeaders, Map<Comparable, Integer> rowHeaders) {
        super.columnHeaders = columnHeaders;
        super.rowHeaders = rowHeaders;
        this.dists = dists;
    }

    /**
     * 
     * @param dataset ignored - the class is just holder of variables given in the constructor
     * @param pivotCount ignored - the class is just holder of variables given in the constructor
     * @return 
     */
    @Override
    public float[][] loadPrecomPivotsToObjectsDists(Dataset dataset, int pivotCount) {
        return dists;
    }

}
