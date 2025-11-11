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
    protected final Map<Comparable, Integer> newColumnHeaders;
    protected final Map<Comparable, Integer> newRowHeaders;
    protected final String namePrefix;

    public DFWithPrecomputedValues(Dataset dataset, AbstractPrecomputedDistancesMatrixSerializator pd, int numberOfPivots, String name) {
        distsHolder = pd.loadPrecomPivotsToObjectsDists(dataset, numberOfPivots);
        searchSpace = dataset.getSearchSpace();
        newColumnHeaders = new HashMap<>();
        newRowHeaders = new HashMap<>();
        Map<Comparable, Integer> origRowHeaders = pd.getRowHeaders();
        Map<Comparable, Integer> origColumnHeaders = pd.getColumnHeaders();
        createNewHeaders(dataset, origRowHeaders, origColumnHeaders);
        this.df = dataset.getDistanceFunction();
        this.namePrefix = name;
    }

    public DFWithPrecomputedValues(Dataset dataset, MainMemoryStoredPrecomputedDistances distsHolder, String namePrefix) {
        this.distsHolder = distsHolder;
        this.df = dataset.getDistanceFunction();
        this.searchSpace = dataset.getSearchSpace();
        newColumnHeaders = new HashMap<>();
        newRowHeaders = new HashMap<>();
        createNewHeaders(dataset, distsHolder.getRowHeaders(), distsHolder.getColumnHeaders());
        this.namePrefix = namePrefix;
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
        if (newColumnHeaders.containsKey(o1IDString) && newRowHeaders.containsKey(o2IDString)) {
            int o1idx = newColumnHeaders.get(o1IDString);
            int o2idx = newRowHeaders.get(o2IDString);
            return distsHolder.getDists()[o1idx][o2idx];
        }
        return df.getDistance(obj1, obj2);
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

    private void createNewHeaders(Dataset dataset, Map<Comparable, Integer> origRowHeaders, Map<Comparable, Integer> origColumnHeaders) {
        Iterator it = dataset.getSearchObjectsFromDataset(-1);
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
    }

    @Override
    public String toString() {
        return namePrefix + getSuffix();
    }

    public String getSuffix() {
        return "";
    }

}
