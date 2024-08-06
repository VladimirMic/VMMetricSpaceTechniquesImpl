package vm.metricSpace.distance.storedPrecomputedDistances;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vm.datatools.DataTypeConvertor;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;

/**
 *
 * @author Vlada
 */
public abstract class AbstractPrecomputedDistancesMatrixLoader {

    /*
    Mapping of object IDs to column indexes
     */
    protected Map<Comparable, Integer> columnHeaders;
    /*
    Mapping of object IDs to row indexes
     */
    protected Map<Comparable, Integer> rowHeaders;

    public AbstractPrecomputedDistancesMatrixLoader() {
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
        return this.loadPrecomPivotsToObjectsDists(dataset, -1);
    }

    public Map<Comparable, Integer> getRowHeaders() {
        return Collections.unmodifiableMap(rowHeaders);
    }

    public Map<Comparable, Integer> getColumnHeaders() {
        return Collections.unmodifiableMap(columnHeaders);
    }

    public final <T> void checkOrdersOfPivots(List<Object> pivots, AbstractMetricSpace<T> metricSpace) {
        List<Comparable> pivotIDsList = metricSpace.getIDsOfMetricObjects(pivots.iterator());
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

}
