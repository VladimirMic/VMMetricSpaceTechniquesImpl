package vm.metricSpace;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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

    public Dataset(String datasetName, AbstractMetricSpace<T> metricSpace, AbstractMetricSpacesStorage metricSpacesStorage) {
        this.datasetName = datasetName;
        this.metricSpace = metricSpace;
        this.metricSpacesStorage = metricSpacesStorage;
    }

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

    public Iterator<Object> getMetricObjectsFromDatasetKeyValueStorage(Object... params) {
        return new IteratorOfMetricObjectsMadeOfKeyValueMap(params);
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

    public TreeSet<Map.Entry<String, Float>> evaluateSmallestDistances(int objectCount, int queriesCount, int retSize) {
        List<Object> metricObjects = getSampleOfDataset(objectCount + queriesCount);
        if (objectCount + queriesCount > metricObjects.size()) {
            throw new IllegalArgumentException("Unsufficient number of data objects. Need " + (objectCount + queriesCount) + ", found " + metricObjects.size());
        }
        List<Object> sampleObjects = metricObjects.subList(0, objectCount);
        List<Object> queriesSamples = metricObjects.subList(objectCount, objectCount + queriesCount);
        DistanceFunctionInterface df = getDistanceFunction();
        Comparator<Map.Entry<String, Float>> comp = new Tools.MapByFloatValueComparator<>();
        TreeSet<Map.Entry<String, Float>> result = new TreeSet(comp);
        for (int i = 0; i < sampleObjects.size(); i++) {
            Object o = sampleObjects.get(i);
            Object oData = metricSpace.getDataOfMetricObject(o);
            Comparable oID = metricSpace.getIDOfMetricObject(o);
            for (Object q : queriesSamples) {
                Object qData = metricSpace.getDataOfMetricObject(q);
                Comparable qID = metricSpace.getIDOfMetricObject(q);
                if (qID.equals(oID)) {
                    continue;
                }
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
            Comparable id1 = metricSpace.getIDOfMetricObject(o1);
            Comparable id2 = metricSpace.getIDOfMetricObject(o2);
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

    /**
     * Return (disk stored on main memory stored) map of IDs of objects and
     * their data. Feel free to skip this method if not needed
     *
     * @return
     */
    public abstract Map<Comparable, T> getKeyValueStorage();

    public abstract boolean hasKeyValueStorage();

    public abstract void deleteKeyValueStorage();

    /**
     * Return a negative number if all
     *
     * @return
     */
    public abstract int getRecommendedNumberOfPivotsForFiltering();

    public static class StaticIteratorOfMetricObjectsMadeOfKeyValueMap<T> implements Iterator<Object> {

        protected final AbstractMetricSpace<T> metricSpace;
        private final int maxCount;
        private int counter;

        private final Iterator<Map.Entry<Comparable, T>> it;

        public StaticIteratorOfMetricObjectsMadeOfKeyValueMap(Iterator<Map.Entry<Comparable, T>> it, AbstractMetricSpace<T> metricSpace, Object... params) {
            this.metricSpace = metricSpace;
            if (params.length > 0) {
                int value = Integer.parseInt(params[0].toString());
                maxCount = value > 0 ? value : Integer.MAX_VALUE;
            } else {
                maxCount = Integer.MAX_VALUE;
            }
            this.it = it;
            counter = 0;
        }

        @Override
        public boolean hasNext() {
            return counter < maxCount && it.hasNext();
        }

        @Override
        public Object next() {
            counter++;
            if (!it.hasNext()) {
                throw new NoSuchElementException("No more objects in the map");
            }
            Map.Entry<Comparable, T> next = it.next();
            return metricSpace.createMetricObject(next.getKey(), next.getValue());
        }

    }

    private class IteratorOfMetricObjectsMadeOfKeyValueMap implements Iterator<Object> {

        protected final AbstractMetricSpace<T> metricSpace;
        private final int maxCount;
        private int counter;

        private final Iterator<Map.Entry<Comparable, T>> it;

        public IteratorOfMetricObjectsMadeOfKeyValueMap(Object... params) {
            this.metricSpace = getMetricSpace();
            if (params.length > 0) {
                int value = Integer.parseInt(params[0].toString());
                maxCount = value > 0 ? value : Integer.MAX_VALUE;
            } else {
                maxCount = Integer.MAX_VALUE;
            }
            this.it = getKeyValueStorage().entrySet().iterator();
            counter = 0;
        }

        @Override
        public boolean hasNext() {
            return counter < maxCount && it.hasNext();
        }

        @Override
        public Object next() {
            counter++;
            if (!it.hasNext()) {
                throw new NoSuchElementException("No more objects in the map");
            }
            Map.Entry<Comparable, T> next = it.next();
            return metricSpace.createMetricObject(next.getKey(), next.getValue());
        }

    }

}
