package vm.searchSpace.distance.storedPrecomputedDistances;

import java.io.IOException;
import java.util.Map;
import vm.searchSpace.Dataset;

/**
 *
 * @author xmic
 */
public class MainMemoryStoredPrecomputedDistances extends AbstractPrecomputedDistancesMatrixSerializator {

    protected final float[][] dists;

    public MainMemoryStoredPrecomputedDistances(float[][] dists, Map<Comparable, Integer> columnHeaders, Map<Comparable, Integer> rowHeaders) {
        super.columnHeaders = columnHeaders;
        super.rowHeaders = rowHeaders;
        this.dists = dists;
    }

    @Deprecated // be careful that the order of rows and column is not the same as in the case of the dataset! You must check the headers. See @getDistance method
    public float[][] getDists() {
        return dists;
    }

    @Deprecated // assumes that columnHeaders and rowHeaders are indexes (numbers) of rows and columns from 1 to n. Returns sorted matrix of distances
    public float[][] getSortedDists() {
        float[][] ret = new float[dists.length][dists[0].length];
        for (Comparable cKey : columnHeaders.keySet()) {
            Integer cIdx = columnHeaders.get(cKey);
            int cKeyInt = Integer.parseInt(cKey.toString()) - 1;
            for (Comparable rKey : rowHeaders.keySet()) {
                Integer rIdx = rowHeaders.get(rKey);
                int rKeyInt = Integer.parseInt(rKey.toString()) - 1;
                ret[cKeyInt][rKeyInt] = dists[cIdx][rIdx];
                String s = "";
            }

        }
        return ret;
    }

    public void modify(int rowIdx, int columnIdx, float newValue) {
        dists[rowIdx][columnIdx] = newValue;
    }

    public float getDistance(Comparable rowOID, Comparable columnPID) {
        Integer i = rowHeaders.get(rowOID);
        Integer j = columnHeaders.get(columnPID);
        if (i != null && j != null) {
            return dists[i][j];
        }
        throw new IllegalArgumentException("At least one idx is null: " + i + ", " + j);
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
