package vm.searchSpace;

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
import vm.searchSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author xmic
 * @param <T>
 */
public abstract class Dataset<T> {

    public static final Logger LOG = Logger.getLogger(Dataset.class.getName());

    protected String datasetName;

    protected AbstractSearchSpacesStorage searchSpacesStorage;

    public Dataset(String datasetName, AbstractSearchSpacesStorage searchSpacesStorage) {
        this.datasetName = datasetName;
        this.searchSpacesStorage = searchSpacesStorage;
    }

    /**
     *
     * @param params volutary, if you want to use them to get your search
     * objects. Usually if the first obnject is an integer, then it limits the
     * number of returned objects
     * @return
     */
    public Iterator<Object> getSearchObjectsFromDataset(Object... params) {
        return searchSpacesStorage.getObjectsFromDataset(datasetName, params);
    }

    public Iterator<Object> getSearchObjectsFromDatasetKeyValueStorage(Object... params) {
        return new IteratorOfSearchObjectsMadeOfKeyValueMap(params);
    }

    /**
     * Returnes sample. Advise: make it deterministic.
     *
     * @param objCount
     * @return
     */
    public List<Object> getSampleOfDataset(int objCount) {
        return searchSpacesStorage.getSampleOfDataset(datasetName, objCount);
    }

    /**
     * Query objects stored under the same name as the dataset
     *
     * @return uses method getQuerySetName and returns associated query objects
     */
    public List<Object> getQueryObjects(Object... params) {
        return searchSpacesStorage.getQueryObjects(getQuerySetName(), params);
    }

    public List<Object> getPivots(int objCount) {
        return searchSpacesStorage.getPivots(getPivotSetName(), objCount);
    }

    /**
     * Distance function, also called the (dis)similarity function. Usually
     * search function but does not have to be
     *
     * @return
     */
    public DistanceFunctionInterface getDistanceFunction() {
        return getSearchSpace().getDistanceFunction();
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

    public AbstractSearchSpace<T> getSearchSpace() {
        return searchSpacesStorage.getSearchSpace();
    }

    public AbstractSearchSpacesStorage getSearchSpacesStorage() {
        return searchSpacesStorage;
    }

    public int getPrecomputedDatasetSize() {
        return searchSpacesStorage.getPrecomputedDatasetSize(datasetName);
    }

    public int updateDatasetSize() {
        return searchSpacesStorage.updateDatasetSize(datasetName);
    }

    public void storePivots(List<Object> pivots, String pivotSetNane, Object... additionalParamsToStoreWithNewPivotSet) {
        searchSpacesStorage.storePivots(pivots, pivotSetNane, additionalParamsToStoreWithNewPivotSet);
    }

    public void storeQueryObjects(List<Object> queryObjs, String querySetName, Object... additionalParamsToStoreWithNewQuerySet) {
        searchSpacesStorage.storeQueryObjects(queryObjs, querySetName, additionalParamsToStoreWithNewQuerySet);
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
        List<Object> searchObjects = getSampleOfDataset(objectCount + queriesCount);
        if (objectCount + queriesCount > searchObjects.size()) {
            LOG.log(Level.SEVERE, "Unsufficient number of data objects. Need {0}, found {1}", new Object[]{objectCount + queriesCount, searchObjects.size()});
            return null;
        }
        AbstractSearchSpace<T> searchSpace = getSearchSpace();
        List<Object> sampleObjects = searchObjects.subList(0, objectCount);
        List<Object> queriesSamples = searchObjects.subList(objectCount, objectCount + queriesCount);
        DistanceFunctionInterface df = getDistanceFunction();
        Comparator<Map.Entry<String, Float>> comp = new Tools.MapByFloatValueComparator<>();
        TreeSet<Map.Entry<String, Float>> result = new TreeSet(comp);
        for (int i = 0; i < sampleObjects.size(); i++) {
            Object o = sampleObjects.get(i);
            Object oData = searchSpace.getDataOfObject(o);
            Comparable oID = searchSpace.getIDOfObject(o);
            for (Object q : queriesSamples) {
                Object qData = searchSpace.getDataOfObject(q);
                Comparable qID = searchSpace.getIDOfObject(q);
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
        List<Object> searchObjectsSample = getSampleOfDataset(objectCount);
        Random r = new Random();
        int counter = 0;
        float[] distances = new float[distCount];
        int size = searchObjectsSample.size();
        AbstractSearchSpace<T> searchSpace = getSearchSpace();
        DistanceFunctionInterface distanceFunction = getDistanceFunction();
        while (counter < distCount) {
            Object o1 = searchObjectsSample.get(r.nextInt(size));
            Object o2 = searchObjectsSample.get(r.nextInt(size));
            Comparable id1 = searchSpace.getIDOfObject(o1);
            Comparable id2 = searchSpace.getIDOfObject(o2);
            if (id1.equals(id2)) {
                continue;
            }
            if (listWhereAddExaminedPairs != null) {
                listWhereAddExaminedPairs.add(new Object[]{id1, id2});
            }
            o1 = searchSpace.getDataOfObject(o1);
            o2 = searchSpace.getDataOfObject(o2);
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

    public abstract boolean shouldStoreDistsToPivots();

    public abstract boolean shouldCreateKeyValueStorage();

    public static class StaticIteratorOfSearchObjectsMadeOfKeyValueMap<T> implements Iterator<Object> {

        protected final AbstractSearchSpace<T> searchSpace;
        private final int maxCount;
        private int counter;

        private final Iterator<Map.Entry<Comparable, T>> it;

        public StaticIteratorOfSearchObjectsMadeOfKeyValueMap(Iterator<Map.Entry<Comparable, T>> it, AbstractSearchSpace<T> searchSpace, Object... params) {
            this.searchSpace = searchSpace;
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
            return searchSpace.createSearchObject(next.getKey(), next.getValue());
        }

    }

    protected class IteratorOfSearchObjectsMadeOfKeyValueMap implements Iterator<Object> {

        protected final AbstractSearchSpace<T> searchSpace;
        private final int maxCount;
        private int counter;

        private final Iterator<Map.Entry<Comparable, T>> it;

        public IteratorOfSearchObjectsMadeOfKeyValueMap(Object... params) {
            this.searchSpace = getSearchSpace();
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
            return searchSpace.createSearchObject(next.getKey(), next.getValue());
        }

    }

}
