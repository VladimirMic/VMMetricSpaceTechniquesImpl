package vm.metricSpace;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author xmic
 * @param <T>
 */
public abstract class Dataset<T> {

    protected String datasetName;

    protected AbstractMetricSpace<T> metricSpace;
    protected MetricSpacesStorageInterface metricSpacesStorage;

    /**
     *
     * @param params volutary, if you want to use them to get your metric
     * objects. Usually if the first obnject is an integer, then it limits the
     * number of returned objects
     * @return
     */
    public Iterator<Object> getMetricObjectsFromDataset(Object... params) {
        return metricSpacesStorage.getObjectsFromDataset(datasetName, params);
    }

    /**
     * Returnes sample. Advise: make it deterministic.
     *
     * @param objCount
     * @return
     */
    public List<Object> getSampleOfDataset(int objCount) {
        return metricSpacesStorage.getSampleOfDataset(datasetName, objCount);
    }

    /**
     * Query objects stored under the same name as the dataset
     *
     * @return uses method getQuerySetName and returns associated query objects
     */
    public List<Object> getMetricQueryObjects() {
        return metricSpacesStorage.getQueryObjects(getQuerySetName());
    }

    public List<Object> getPivots(int objCount) {
        return metricSpacesStorage.getPivots(getPivotSetName(), objCount);
    }

    /**
     * Distance function, also called the (dis)similarity function. Usually
     * metric function but does not have to be
     *
     * @return
     */
    public DistanceFunctionInterface getDistanceFunction() {
        return metricSpace.getDistanceFunctionForDataset(datasetName);
    }

    public String getDatasetName() {
        return datasetName;
    }

    public String getQuerySetName() {
        return datasetName;
    }

    public String getPivotSetName() {
        return datasetName;
    }

    public AbstractMetricSpace<T> getMetricSpace() {
        return metricSpace;
    }

    public MetricSpacesStorageInterface getMetricSpacesStorage() {
        return metricSpacesStorage;
    }

    public int getPrecomputedDatasetSize() {
        return metricSpacesStorage.getPrecomputedDatasetSize(datasetName);
    }

    /**
     * Return (usually disk stored) map of IDs of objects and their data. Feel
     * free to skip this method if not needed
     *
     * @return
     */
    public abstract Map<Object, Object> getKeyValueStorage();
}
