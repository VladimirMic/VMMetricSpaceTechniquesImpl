package vm.searchSpace.distance.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import vm.datatools.Tools;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.Dataset;
import vm.searchSpace.distance.DistanceFunctionInterface;
import vm.searchSpace.distance.storedPrecomputedDistances.AbstractPrecomputedDistancesMatrixSerializator;
import vm.searchSpace.distance.storedPrecomputedDistances.MainMemoryStoredPrecomputedDistances;

/**
 *
 * @author xmic
 * @param <T> the data type for the distance function, e.g. float[] for the
 * float vector.
 */
public class DFWithPrecomputedValues<T> extends DistanceFunctionInterface<T> {

    protected MainMemoryStoredPrecomputedDistances distsHolder;
    protected final DistanceFunctionInterface<T> df;
    protected final AbstractSearchSpace<T> searchSpace;

    public DFWithPrecomputedValues(Dataset dataset, AbstractPrecomputedDistancesMatrixSerializator pd, int numberOfPivots) {
        distsHolder = pd.loadPrecomPivotsToObjectsDists(dataset, numberOfPivots);
        searchSpace = dataset.getSearchSpace();
        Map<Comparable, Integer> newColumnHeaders = new HashMap<>();
        Map<Comparable, Integer> newRowHeaders = new HashMap<>();
        Iterator it = dataset.getSearchObjectsFromDataset(-1);
        Map<Comparable, Integer> origRowHeaders = pd.getRowHeaders();
        Map<Comparable, Integer> origColumnHeaders = pd.getColumnHeaders();
        while (it.hasNext()) {
            Object obj = it.next();
            Comparable oID = searchSpace.getIDOfObject(obj);
            Integer idxRow = origRowHeaders.get(oID);
            Integer idxColumn = origColumnHeaders.get(oID);
            T oData = searchSpace.getDataOfObject(obj);
            Integer newKey = Tools.hashArray(oData);
            String newKeyStriong = newKey.toString();
            newColumnHeaders.put(newKeyStriong, idxRow);
            newRowHeaders.put(newKeyStriong, idxColumn);
        }
        distsHolder = new MainMemoryStoredPrecomputedDistances(distsHolder.getDists(), newColumnHeaders, newRowHeaders);
        this.df = dataset.getDistanceFunction();
    }

    public DFWithPrecomputedValues(Dataset dataset, MainMemoryStoredPrecomputedDistances distsHolder, int numberOfPivots) {
        this.distsHolder = distsHolder;
        this.df = dataset.getDistanceFunction();
        this.searchSpace = dataset.getSearchSpace();
    }

    public void setDistsHolder(MainMemoryStoredPrecomputedDistances distsHolder) {
        this.distsHolder = distsHolder;
    }

    @Override
    public float getDistance(T obj1, T obj2) {
        Comparable o1ID = Tools.hashArray(obj1);
        Comparable o2ID = Tools.hashArray(obj2);
        String o1IDString = o1ID.toString();
        String o2IDString = o2ID.toString();
        Map<Comparable, Integer> columnHeaders = distsHolder.getColumnHeaders();
        Map<Comparable, Integer> rowHeaders = distsHolder.getRowHeaders();
        if (columnHeaders.containsKey(o1IDString) && rowHeaders.containsKey(o2IDString)) {
            int o1idx = columnHeaders.get(o1IDString);
            int o2idx = rowHeaders.get(o2IDString);
            return distsHolder.getDists()[o1idx][o2idx];
        }
        T obj1Data = searchSpace.getDataOfObject(obj1);
        T obj2Data = searchSpace.getDataOfObject(obj2);
        return df.getDistance(obj1Data, obj2Data);
    }

    public int getColumnCount() {
        return distsHolder.getColumnHeaders().size();
    }

    public int getRowCount() {
        return distsHolder.getRowHeaders().size();
    }

    public Map<Comparable, Integer> getColumnHeaders() {
        return distsHolder.getColumnHeaders();
    }

    public Map<Comparable, Integer> getRowHeaders() {
        return distsHolder.getRowHeaders();
    }

    public float[][] getDists() {
        return distsHolder.getDists();
    }

}
