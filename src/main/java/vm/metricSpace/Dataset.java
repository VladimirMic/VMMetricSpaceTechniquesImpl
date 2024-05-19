package vm.metricSpace;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author xmic
 * @param <T>
 */
public abstract class Dataset<T> {

    public static final Logger LOG = Logger.getLogger(Dataset.class.getName());

    protected String datasetName;

    protected AbstractMetricSpace<T> metricSpace;
    protected AbstractMetricSpacesStorage metricSpacesStorage;

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
    public List<Object> getQueryObjects(Object... params) {
        return metricSpacesStorage.getQueryObjects(getQuerySetName(), params);
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

    public AbstractMetricSpacesStorage getMetricSpacesStorage() {
        return metricSpacesStorage;
    }

    public int getPrecomputedDatasetSize() {
        return metricSpacesStorage.getPrecomputedDatasetSize(datasetName);
    }

    public int updateDatasetSize() {
        return metricSpacesStorage.updateDatasetSize(datasetName);
    }

    public void storePivots(List<Object> pivots, String pivotSetNane, Object... additionalParamsToStoreWithNewPivotSet) {
        metricSpacesStorage.storePivots(pivots, pivotSetNane, additionalParamsToStoreWithNewPivotSet);
    }

    public void storeQueryObjects(List<Object> queryObjs, String querySetName, Object... additionalParamsToStoreWithNewQuerySet) {
        metricSpacesStorage.storeQueryObjects(queryObjs, querySetName, additionalParamsToStoreWithNewQuerySet);
    }

    /**
     * Return (usually disk stored) map of IDs of objects and their data. Feel
     * free to skip this method if not needed
     *
     * @return
     */
    public abstract Map<Object, Object> getKeyValueStorage();

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(getDatasetName());
        hash = 37 * hash + Objects.hashCode(getQuerySetName());
        hash = 37 * hash + Objects.hashCode(getPivotSetName());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Dataset<?> other = (Dataset<?>) obj;
        if (!Objects.equals(getDatasetName(), other.getDatasetName())) {
            return false;
        }
        if (!Objects.equals(getQuerySetName(), other.getQuerySetName())) {
            return false;
        }
        return Objects.equals(getPivotSetName(), getPivotSetName());
    }

    public TreeSet<Map.Entry<String, Float>> evaluateSampleOfSmallestDistances(int objectCount, int queriesCount, int retSize, List<Object[]> listWhereAddExaminedPairs) {
        List<Object> metricObjects = getSampleOfDataset(objectCount + queriesCount);
        List<Object> sampleObjects = metricObjects.subList(0, objectCount);
        List<Object> queriesSamples = metricObjects.subList(objectCount, objectCount + queriesCount);
        DistanceFunctionInterface df = getDistanceFunction();
        Comparator<Map.Entry<String, Float>> comp = new Tools.MapByFloatValueComparator<>();
        TreeSet<Map.Entry<String, Float>> result = new TreeSet(comp);
        for (int i = 0; i < sampleObjects.size(); i++) {
            Object o = sampleObjects.get(i);
            Object oData = metricSpace.getDataOfMetricObject(o);
            Object oID = metricSpace.getIDOfMetricObject(o);
            for (Object q : queriesSamples) {
                Object qData = metricSpace.getDataOfMetricObject(q);
                Object qID = metricSpace.getIDOfMetricObject(q);
                float dist = df.getDistance(oData, qData);
                String key = oID + ";" + qID;
                AbstractMap.SimpleEntry<String, Float> e = new AbstractMap.SimpleEntry(key, dist);
                result.add(e);
                while (result.size() > retSize) {
                    result.remove(result.last());
                }
            }
            if ((i + 1) % 500 == 0) {
                LOG.log(Level.INFO, "Processed object {0} out of {1}", new Object[]{i + 1, sampleObjects.size()});
            }
        }
        return result;
    }

    public float[] evaluateSampleOfRandomDistances(int objectCount, int distCount, List<Object[]> listWhereAddExaminedPairs) {
        List<Object> metricObjectsSample = getSampleOfDataset(objectCount);
        Random r = new Random();
        int counter = 0;
        float[] distances = new float[distCount];
        int size = metricObjectsSample.size();
        DistanceFunctionInterface distanceFunction = getDistanceFunction();
        while (counter < distCount) {
            Object o1 = metricObjectsSample.get(r.nextInt(size));
            Object o2 = metricObjectsSample.get(r.nextInt(size));
            Object id1 = metricSpace.getIDOfMetricObject(o1);
            Object id2 = metricSpace.getIDOfMetricObject(o2);
            if (id1.equals(id2)) {
                continue;
            }
            if (listWhereAddExaminedPairs != null) {
                listWhereAddExaminedPairs.add(new Object[]{id1, id2});
            }
            o1 = metricSpace.getDataOfMetricObject(o1);
            o2 = metricSpace.getDataOfMetricObject(o2);
            distances[counter] = distanceFunction.getDistance(o1, o2);
            counter++;
            if (counter % 1000000 == 0) {
                LOG.log(Level.INFO, "Computed {0} distances out of {1}", new Object[]{counter, distCount});
            }
        }
        return distances;
    }

}
