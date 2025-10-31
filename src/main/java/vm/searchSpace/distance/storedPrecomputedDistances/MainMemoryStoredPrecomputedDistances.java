package vm.searchSpace.distance.storedPrecomputedDistances;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
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

    /**
     *
     * @param dataset ignored - the class is just holder of variables given in
     * the constructor
     * @param pivotCount ignored - the class is just holder of variables given
     * in the constructor
     * @return
     */
    @Override
    public float[][] loadPrecomPivotsToObjectsDists(Dataset dataset, int pivotCount) {
        return dists;
    }

    public float[][] getDists() {
        return dists;
    }

    @Override
    public void serializeColumnsHeaders(OutputStream outputStream, Map<Comparable, Integer> columnKeys) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public int serializeRows(OutputStream outputStream, Map<Comparable, Integer> rowKeys, Map<Comparable, Integer> columnKeys, float[][] distsInRow, int rowCounter) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}
