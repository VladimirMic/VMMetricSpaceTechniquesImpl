package vm.searchSpace.distance;

import java.util.Map;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.Dataset;
import vm.searchSpace.distance.storedPrecomputedDistances.AbstractPrecomputedDistancesMatrixLoader;

/**
 *
 * @author xmic
 * @param <T> the data type for the distance function, e.g. float[] for the
 * float vector.
 */
public class DistanceFunctionWithPrecomputedValues<T> extends DistanceFunctionInterface {

    private final float[][] dists;
    private final Map<Comparable, Integer> columnHeaders;
    private final Map<Comparable, Integer> rowHeaders;
    private final DistanceFunctionInterface<T> df;
    private final AbstractSearchSpace<T> searchSpace;

    public DistanceFunctionWithPrecomputedValues(AbstractSearchSpace<T> searchSpace, AbstractPrecomputedDistancesMatrixLoader pd, Dataset dataset, DistanceFunctionInterface<T> encapsulatedDF, int numberOfPivots) {
        this.dists = pd.loadPrecomPivotsToObjectsDists(dataset, numberOfPivots);
        this.columnHeaders = pd.getColumnHeaders();
        this.rowHeaders = pd.getRowHeaders();
        this.df = encapsulatedDF;
        this.searchSpace = searchSpace;
    }

    @Override
    public float getDistance(Object obj1, Object obj2) {
        Comparable o1ID = searchSpace.getIDOfObject(obj1);
        Comparable o2ID = searchSpace.getIDOfObject(obj2);
        if (columnHeaders.containsKey(o1ID) && rowHeaders.containsKey(o2ID)) {
            int o1idx = columnHeaders.get(o1ID);
            int o2idx = rowHeaders.get(o2ID);
            return dists[o1idx][o2idx];
        }
        if (rowHeaders.containsKey(o1ID) && columnHeaders.containsKey(o2ID)) {
            int o1idx = rowHeaders.get(o1ID);
            int o2idx = columnHeaders.get(o2ID);
            return dists[o1idx][o2idx];
        }
        T obj1Data = searchSpace.getDataOfObject(obj1);
        T obj2Data = searchSpace.getDataOfObject(obj2);
        return df.getDistance(obj1Data, obj2Data);
    }

}
