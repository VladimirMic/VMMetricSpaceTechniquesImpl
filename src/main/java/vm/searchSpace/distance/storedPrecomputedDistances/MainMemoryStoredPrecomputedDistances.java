package vm.searchSpace.distance.storedPrecomputedDistances;

import java.io.IOException;
import java.util.Map;
import vm.searchSpace.Dataset;

/**
 *
 * @author xmic
 */
public class MainMemoryStoredPrecomputedDistances extends AbstractPrecomputedDistancesMatrixSerializator {

    private final float[][] dists;

    public MainMemoryStoredPrecomputedDistances(float[][] dists, Map<Comparable, Integer> columnHeaders, Map<Comparable, Integer> rowHeaders) {
        super.columnHeaders = columnHeaders;
        super.rowHeaders = rowHeaders;
        this.dists = dists;
    }

    public float[][] getDists() {
        return dists;
    }

    public void modify(int rowIdx, int columnIdx, float newValue) {
        dists[rowIdx][columnIdx] = newValue;
    }

    @Override
    public void serializeColumnsHeaders(Dataset dataset, int pivotCount, String additionalName, Map<Comparable, Integer> columnKeys) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public int serializeRows(Dataset dataset, int pivotCount, String additionalName, Map<Comparable, Integer> rowKeys, Map<Comparable, Integer> columnKeys, float[][] distsInRow, int rowCounter) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public MainMemoryStoredPrecomputedDistances loadPrecomPivotsToObjectsDists(Dataset dataset, String dfModification, int pivotCount) {
        return this;
    }


}
