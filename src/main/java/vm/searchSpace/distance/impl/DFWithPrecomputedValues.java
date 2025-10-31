package vm.searchSpace.distance.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import vm.datatools.Tools;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.Dataset;
import vm.searchSpace.distance.DistanceFunctionInterface;
import vm.searchSpace.distance.storedPrecomputedDistances.AbstractPrecomputedDistancesMatrixSerializator;

/**
 *
 * @author xmic
 * @param <T> the data type for the distance function, e.g. float[] for the
 * float vector.
 */
public class DFWithPrecomputedValues<T> extends DistanceFunctionInterface<T> {

    protected final float[][] dists;
    protected final Map<Comparable, Integer> columnHeaders;
    protected final Map<Comparable, Integer> rowHeaders;
    protected final DistanceFunctionInterface<T> df;
    protected final AbstractSearchSpace<T> searchSpace;

    public DFWithPrecomputedValues(AbstractSearchSpace<T> searchSpace, AbstractPrecomputedDistancesMatrixSerializator pd, Dataset dataset, DistanceFunctionInterface<T> encapsulatedDF, int numberOfPivots) {
        this.dists = pd.loadPrecomPivotsToObjectsDists(dataset, numberOfPivots);
        this.columnHeaders = new HashMap<>();
        this.rowHeaders = new HashMap<>();
        Iterator it = dataset.getSearchObjectsFromDataset(-1);
        Map<Comparable, Integer> rows = pd.getRowHeaders();
        Map<Comparable, Integer> columns = pd.getColumnHeaders();
        while (it.hasNext()) {
            Object obj = it.next();
            Comparable oID = searchSpace.getIDOfObject(obj);
            Integer idxRow = rows.get(oID);
            Integer idxColumn = columns.get(oID);
            T oData = searchSpace.getDataOfObject(obj);
            int newKey = Tools.hashArray(oData);
            columnHeaders.put(newKey, idxRow);
            rowHeaders.put(newKey, idxColumn);
        }
        this.df = encapsulatedDF;
        this.searchSpace = searchSpace;
    }

    @Override
    public float getDistance(T obj1, T obj2) {
        Comparable o1ID = Tools.hashArray(obj1);
        Comparable o2ID = Tools.hashArray(obj2);
        if (columnHeaders.containsKey(o1ID) && rowHeaders.containsKey(o2ID)) {
            int o1idx = columnHeaders.get(o1ID);
            int o2idx = rowHeaders.get(o2ID);
            return dists[o1idx][o2idx];
        }
        T obj1Data = searchSpace.getDataOfObject(obj1);
        T obj2Data = searchSpace.getDataOfObject(obj2);
        return df.getDistance(obj1Data, obj2Data);
    }

}
