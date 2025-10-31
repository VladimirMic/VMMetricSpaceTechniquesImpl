package vm.searchSpace.distance.storedPrecomputedDistances;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vm.datatools.DataTypeConvertor;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.Dataset;

/**
 *
 * @author Vlada
 */
public abstract class AbstractPrecomputedDistancesMatrixSerializator {

    /*
    Mapping of object IDs to column indexes
     */
    protected Map<Comparable, Integer> columnHeaders;
    /*
    Mapping of object IDs to row indexes
     */
    protected Map<Comparable, Integer> rowHeaders;

    public AbstractPrecomputedDistancesMatrixSerializator() {
        this.rowHeaders = new HashMap<>();
        this.columnHeaders = new HashMap<>();
    }

    /**
     *
     * @param dataset
     * @param pivotCount if -1 then all pivots are used (pivots correspond to
     * columns)
     * @return map of distances (for instance, rows correspond to the objects
     * from the dataset, columns correspod to the pivots)
     */
    public abstract float[][] loadPrecomPivotsToObjectsDists(Dataset dataset, int pivotCount);

    public float[][] loadPrecomPivotsToObjectsDists(Dataset dataset) {
        return this.loadPrecomPivotsToObjectsDists(dataset, dataset.getRecommendedNumberOfPivotsForFiltering());
    }

    public Map<Comparable, Integer> getRowHeaders() {
        return Collections.unmodifiableMap(rowHeaders);
    }

    public Map<Comparable, Integer> getColumnHeaders() {
        return Collections.unmodifiableMap(columnHeaders);
    }

    public final <T> void checkOrdersOfPivots(List<Object> pivots, AbstractSearchSpace<T> searchSpace) {
        List<Comparable> pivotIDsList = searchSpace.getIDsOfObjects(pivots.iterator());
        String[] pivotIDs = DataTypeConvertor.objectsToStrings(pivotIDsList);
        for (int p = 0; p < pivotIDsList.size(); p++) {
            Comparable pId = pivotIDs[p];
            if (!columnHeaders.containsKey(pId)) {
                throw new IllegalArgumentException("Precomputed distances dost not contain pivot " + pId);
            }
            int pIdx = columnHeaders.get(pId);
            if (pIdx != p) {
                throw new IllegalArgumentException("Wrong pivot ordering " + pIdx + ", " + p);
            }
        }
    }

    public int serialize(OutputStream outputStream, Map<Comparable, Integer> rowKeys, Map<Comparable, Integer> columnKeys, float[][] distsInRow, int rowCounter) throws IOException {
        serializeColumnsHeaders(outputStream, columnKeys);
        return serializeRows(outputStream, rowKeys, columnKeys, distsInRow, rowCounter);
    }

    public abstract void serializeColumnsHeaders(OutputStream outputStream, Map<Comparable, Integer> columnKeys) throws IOException;

    public abstract int serializeRows(OutputStream outputStream, Map<Comparable, Integer> rowKeys, Map<Comparable, Integer> columnKeys, float[][] distsInRow, int rowCounter) throws IOException;

}
