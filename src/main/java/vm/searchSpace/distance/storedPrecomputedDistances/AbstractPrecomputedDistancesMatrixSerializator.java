package vm.searchSpace.distance.storedPrecomputedDistances;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
     * @param dfModification
     * @param pivotCount if -1 then all pivots are used (pivots correspond to
     * columns)
     * @return map of distances (for instance, rows correspond to the objects
     * from the dataset, columns correspod to the pivots)
     */
    public abstract MainMemoryStoredPrecomputedDistances loadPrecomPivotsToObjectsDists(Dataset dataset, String dfModification, int pivotCount);

    public MainMemoryStoredPrecomputedDistances loadPrecomPivotsToObjectsDists(Dataset dataset, int pivotCount) {
        return loadPrecomPivotsToObjectsDists(dataset, "", pivotCount);
    }

    public MainMemoryStoredPrecomputedDistances loadPrecomPivotsToObjectsDists(Dataset dataset) {
        return this.loadPrecomPivotsToObjectsDists(dataset, dataset.getRecommendedNumberOfPivotsForFiltering());
    }

    public Map<Comparable, Integer> getRowHeaders() {
        return Collections.unmodifiableMap(rowHeaders);
    }

    public Map<Comparable, Integer> getColumnHeaders() {
        return Collections.unmodifiableMap(columnHeaders);
    }

    // maybe not needed anymore?? Not sure
//    public final <T> void checkOrdersOfPivots(List<Object> pivots, AbstractSearchSpace<T> searchSpace) {
//        List<Comparable> pivotIDsList = searchSpace.getIDsOfObjects(pivots.iterator());
//        String[] pivotIDs = DataTypeConvertor.objectsToStrings(pivotIDsList);
//        for (int p = 0; p < pivotIDsList.size(); p++) {
//            Comparable pId = pivotIDs[p];
//            if (!columnHeaders.containsKey(pId)) {
//                throw new IllegalArgumentException("Precomputed distances dost not contain pivot " + pId);
//            }
//            int pIdx = columnHeaders.get(pId);
//            if (pIdx != p) {
//                throw new IllegalArgumentException("Wrong pivot ordering " + pIdx + ", " + p);
//            }
//        }
//    }

    public int serialize(Dataset dataset, int pivotCount, String additionalName, Map<Comparable, Integer> rowKeys, Map<Comparable, Integer> columnKeys, float[][] distsInRow, int rowCounter) throws IOException {
        serializeColumnsHeaders(dataset, pivotCount, additionalName, columnKeys);
        return serializeRows(dataset, pivotCount, additionalName, rowKeys, columnKeys, distsInRow, rowCounter);
    }

    public int serialize(Dataset dataset, int pivotCount, String additionalName, Map<Comparable, Integer> rowKeys, Map<Comparable, Integer> columnKeys, float[][] distsInRow) throws IOException {
        return serialize(dataset, pivotCount, additionalName, rowKeys, columnKeys, distsInRow, 0);
    }

    public abstract void serializeColumnsHeaders(Dataset dataset, int pivotCount, String additionalName, Map<Comparable, Integer> columnKeys) throws IOException;

    public abstract int serializeRows(Dataset dataset, int pivotCount, String additionalName, Map<Comparable, Integer> rowKeys, Map<Comparable, Integer> columnKeys, float[][] distsInRow, int rowCounter) throws IOException;

}
