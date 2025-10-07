package vm.search.algorithm;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.Dataset;
import vm.searchSpace.DatasetOfCandidates;
import vm.searchSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author Vlada
 * @param <T>
 */
public abstract class SearchingAlgorithm<T> {

    private static final Logger LOG = Logger.getLogger(SearchingAlgorithm.class.getName());
    public static final Integer K_IMPLICIT_FOR_QUERIES = 30;
    public static final int STEP_COUNTS_FOR_CAND_SE_PROCESSING_FROM_INDEX = 5; // deprecated to use larger number. The memory overhead mitigates a positive influence of caching in case of large datasets
    public static final Integer BATCH_SIZE = 5000000; //  5000000 simulates independent queries as data are not effectively cached in the CPU cache

    public static int getNumberOfRepetitionsDueToCaching(Dataset dataset) {
        return dataset instanceof DatasetOfCandidates ? 1 : 2;
    }

    protected final ConcurrentHashMap<Comparable, AtomicInteger> distCompsPerQueries = new ConcurrentHashMap();
    protected final ConcurrentHashMap<Comparable, AtomicLong> timesPerQueries = new ConcurrentHashMap();
    protected final ConcurrentHashMap<Comparable, List<AtomicLong>> additionalStatsPerQueries = new ConcurrentHashMap();
    protected final ConcurrentHashMap<Comparable, float[]> qpDistsCached = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<Comparable, int[]> qPivotPermutationCached = new ConcurrentHashMap<>();

    public abstract List<Comparable> candSetKnnSearch(AbstractSearchSpace<T> searchSpace, Object queryObject, int k, Iterator<Object> objects, Object... additionalParams);

    /**
     * Implicit implementation that just reranks the candidate set defined in
     * this algorithm. Feel free to override with another complete search.
     *
     * @param searchSpace
     * @param queryObject
     * @param k
     * @param objects
     * @param additionalParams
     * @return
     */
    public TreeSet<Map.Entry<Comparable, Float>> completeKnnSearch(AbstractSearchSpace<T> searchSpace, Object queryObject, int k, Iterator<Object> objects, Object... additionalParams) {
        List<Comparable> candSet = candSetKnnSearch(searchSpace, queryObject, k, objects, additionalParams);
        Dataset dataset = (Dataset) additionalParams[0];
        return rerankCandidateSet(searchSpace, queryObject, k, dataset.getDistanceFunction(), dataset.getKeyValueStorage(), candSet);
    }

    public Map.Entry<Comparable, Float> adjustAndReturnLastEntry(TreeSet<Map.Entry<Comparable, Float>> currAnswer, int k) {
        int size = currAnswer.size();
        if (size < k) {
            return null;
        }
        while (currAnswer.size() > k) {
            currAnswer.remove(currAnswer.last());
        }
        return currAnswer.last();
    }

    /**
     *
     * @param currAnswer
     * @param k max size of an answer
     * @param searchRadius value that is returned if the answer is smaller than
     * k
     * @return distance to the kthNN or given searchRadius
     */
    public static float adjustAndReturnSearchRadiusAfterAddingOne(TreeSet<Map.Entry<Comparable, Float>> currAnswer, int k, float searchRadius) {
        if (currAnswer == null) {
            return Float.MAX_VALUE;
        }
        int size = currAnswer.size();
        if (size < k) {
            return searchRadius;
        }
        if (size > k) {
            currAnswer.remove(currAnswer.last());
        }
        return currAnswer.last().getValue();
    }

    protected void incAdditionalParam(Comparable qId, long byValue, int idx) {
        List<AtomicLong> list = additionalStatsPerQueries.get(qId);
        if (list == null) {
            list = new ArrayList<>();
            additionalStatsPerQueries.put(qId, list);
        }
        if (list.size() <= idx) {
            for (int i = list.size(); i <= idx; i++) {
                list.add(new AtomicLong());
            }
        }
        list.get(idx).addAndGet(byValue);
    }

    public float adjustAndReturnSearchRadiusAfterAddingMore(TreeSet<Map.Entry<Comparable, Float>> currAnswer, int k, float searchRadius) {
        int size = currAnswer.size();
        if (size < k) {
            return searchRadius;
        }
        while (currAnswer.size() > k) {
            currAnswer.remove(currAnswer.last());
        }
        return currAnswer.last().getValue();
    }

    public TreeSet<Map.Entry<Comparable, Float>> rerankCandidateSet(AbstractSearchSpace<T> searchSpace, Object queryObj, int k, DistanceFunctionInterface df, Map<Comparable, T> mapOfAllFullObjects, List<Comparable> candsIDs) {
        T queryObjData = searchSpace.getDataOfObject(queryObj);
        TreeSet<Map.Entry<Comparable, Float>> ret = new TreeSet<>(new Tools.MapByFloatValueComparator());
        if (mapOfAllFullObjects == null) {
            for (int i = 0; i < Math.min(candsIDs.size(), k); i++) {
                Comparable id = candsIDs.get(i);
                ret.add(new AbstractMap.SimpleEntry<>(id, (float) i));
            }
            return ret;
        }
        float qRange = Float.MAX_VALUE;
        for (Comparable candID : candsIDs) {
            T searchObjectData;
            try {
                searchObjectData = (T) mapOfAllFullObjects.get(candID);
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Something wrong happened in the data map. Trying to repeat it. CandID: " + candID, ex);
                continue;
            }
            float distance = df.getDistance(queryObjData, searchObjectData);
            if (distance < qRange) {
                ret.add(new AbstractMap.SimpleEntry<>(candID, distance));
                qRange = adjustAndReturnSearchRadiusAfterAddingOne(ret, k, qRange);
            }
        }
        return ret;
    }

    /**
     * @param searchSpace
     * @param queryObjects
     * @param k
     * @param objects
     * @param additionalParams -- voluntarily the first one can be the
     * parallelisation.
     * @return evaluates all query objects in parallel. Parallelisation is done
     * over the query objects. For each query objects returns a map of IDs and
     * distances.
     */
    public TreeSet<Map.Entry<Comparable, Float>>[] completeKnnFilteringWithQuerySet(AbstractSearchSpace<T> searchSpace, List<Object> queryObjects, int k, Iterator<Object> objects, Object... additionalParams) {
        final TreeSet<Map.Entry<Comparable, Float>>[] ret;
        if (additionalParams != null && additionalParams.length == queryObjects.size() && additionalParams instanceof TreeSet[]) {
            ret = (TreeSet<Map.Entry<Comparable, Float>>[]) additionalParams;
        } else {
            ret = new TreeSet[queryObjects.size()];
        }
        final List<Object> batch = new ArrayList<>();
        for (int i = 0; i < queryObjects.size(); i++) {
            Comparable qID = searchSpace.getIDOfObject(queryObjects.get(i));
            timesPerQueries.put(qID, new AtomicLong());
            if (ret[i] == null) {
                ret[i] = new TreeSet<>(new Tools.MapByFloatValueComparator());
            }
        }
        ExecutorService threadPool;
        if (additionalParams != null && additionalParams.length > 0 && additionalParams[0] instanceof Integer) {
            threadPool = vm.javatools.Tools.initExecutor((Integer) additionalParams[0]);
        } else {
            threadPool = vm.javatools.Tools.initExecutor();
        }
        int batchCounter = 0;
        while (objects.hasNext()) {
            try {
                batch.clear();
                long t = -System.currentTimeMillis();
                for (int i = 0; i < BATCH_SIZE && objects.hasNext(); i++) {
                    batch.add(objects.next());
                    if (i % 50000 == 0 && t + System.currentTimeMillis() > 5000) {
                        LOG.log(Level.INFO, "Loading objects into the batch. {0} Loaded", i);
                        t = -System.currentTimeMillis();
                    }
                }
                if (batch.isEmpty()) {
                    break;
                }
                batchCounter++;
                LOG.log(Level.INFO, "Start processing {1} , batch size {0} ... This will take some time, depending on the algorithm efficiency", new Object[]{batch.size(), getResultName()});
                System.gc();
                CountDownLatch latch = new CountDownLatch(queryObjects.size());
                final AbstractSearchSpace<T> searchSpaceFinal = searchSpace;
                t = -System.currentTimeMillis();
                for (int i = 0; i < queryObjects.size(); i++) {
                    final Object queryObject = queryObjects.get(i);
                    final TreeSet<Map.Entry<Comparable, Float>> answerToQuery = ret[i];
                    threadPool.execute(() -> {
                        vm.javatools.Tools.sleepDuringTheNight();
                        TreeSet<Map.Entry<Comparable, Float>> completeKnnSearch = completeKnnSearch(searchSpaceFinal, queryObject, k, batch.iterator(), answerToQuery, additionalParams);
                        answerToQuery.addAll(completeKnnSearch);
                        latch.countDown();
                    });
                }
                latch.await();
                t += System.currentTimeMillis();
                LOG.log(Level.INFO, "Batch {0} processed in {1} ms", new Object[]{batchCounter, t});
            } catch (InterruptedException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
        threadPool.shutdown();
        return ret;
    }

    public static TreeSet<Map.Entry<Comparable, Float>>[] initKNNResultSets(int numberOfQueries) {
        TreeSet<Map.Entry<Comparable, Float>>[] ret = new TreeSet[numberOfQueries];
        for (int i = 0; i < numberOfQueries; i++) {
            ret[i] = new TreeSet<>(new Tools.MapByFloatValueComparator());
        }
        return ret;
    }

    /**
     * @param searchSpace
     * @param queryObjects
     * @param k
     * @param kCandSetMaxSize
     * @param keyValueStorage
     * @param additionalParams
     * @return
     */
    public TreeSet<Map.Entry<Comparable, Float>>[] completeKnnSearchWithPartitioningForQuerySet(AbstractSearchSpace<T> searchSpace, List<Object> queryObjects, int k, int kCandSetMaxSize, Map<Object, T> keyValueStorage, Object... additionalParams) {
        final TreeSet<Map.Entry<Comparable, Float>>[] ret = new TreeSet[queryObjects.size()];
        ExecutorService threadPool = vm.javatools.Tools.initExecutor(vm.javatools.Tools.PARALELISATION);
        try {
            CountDownLatch latch = new CountDownLatch(queryObjects.size());
            final AbstractSearchSpace<T> searchSpaceFinal = searchSpace;
            for (int i = 0; i < queryObjects.size(); i++) {
                final Object queryObject = queryObjects.get(i);
                final int iFinal = i;
                threadPool.execute(() -> {
                    Object[] params = Tools.concatArrays(new Object[]{keyValueStorage, kCandSetMaxSize}, additionalParams);
                    ret[iFinal] = completeKnnSearch(searchSpaceFinal, queryObject, k, null, params);
                    latch.countDown();
                });
            }
            latch.await();
        } catch (InterruptedException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        threadPool.shutdown();
        return ret;
    }

    public TreeSet<Map.Entry<Comparable, Float>>[] evaluateIteratorsSequentiallyForEachQuery(Dataset dataset, List queryObjects, int k) {
        Map<Integer, TreeSet<Map.Entry<Comparable, Float>>[]> map = evaluateIteratorsSequentiallyForEachQuery(dataset, queryObjects, k, false, -1);
        return map.get(-1);
    }

    /**
     * Returns candidate set size mapped to the sorted sets (for each query) of
     * nearest neighbours in a form of <Map.Entry<Object, Float>> of id and
     * distance
     *
     * @param dataset
     * @param queryObjects
     * @param k
     * @param storePartialCandSetSizes
     * @param candidatesProvided
     * @return
     */
    public Map<Integer, TreeSet<Map.Entry<Comparable, Float>>[]> evaluateIteratorsSequentiallyForEachQuery(Dataset dataset, List queryObjects, int k, boolean storePartialCandSetSizes, int candidatesProvided) {
        AbstractSearchSpace searchSpace = dataset.getSearchSpace();
        int batchSize = storePartialCandSetSizes && candidatesProvided > 0 ? candidatesProvided / STEP_COUNTS_FOR_CAND_SE_PROCESSING_FROM_INDEX : candidatesProvided;
        if (batchSize < 0) {
            batchSize = BATCH_SIZE;
        }
        Map<Integer, TreeSet<Map.Entry<Comparable, Float>>[]> ret = initAnswerMapForCandSetSizes(candidatesProvided, queryObjects.size(), batchSize);
        for (int i = 0; i < queryObjects.size(); i++) {
            vm.javatools.Tools.sleepDuringTheNight();
            Object q = queryObjects.get(i);
            Comparable qID = searchSpace.getIDOfObject(q);
            Iterator candsIt = dataset.getSearchObjectsFromDataset(qID);
            for (int batchCounter = 1; candsIt.hasNext(); batchCounter++) {
                Iterator<Object> batchIt = Tools.getObjectsFromIterator(candsIt, batchSize).iterator();
                TreeSet<Map.Entry<Comparable, Float>>[] prev = candidatesProvided < 0 ? ret.get(-1) : ret.get((batchCounter - 1) * batchSize);
                TreeSet<Map.Entry<Comparable, Float>> newAnswer = prev == null || prev[i] == null ? null : new TreeSet<>(prev[i].comparator());
                if (newAnswer != null) {
                    newAnswer.addAll(prev[i]);
                }
                TreeSet<Map.Entry<Comparable, Float>>[] retForCandSetSize = candidatesProvided < 0 ? ret.get(-1) : ret.get(batchCounter * batchSize);
                retForCandSetSize[i] = completeKnnSearch(searchSpace, q, k, batchIt, newAnswer, batchCounter * batchSize);
                incAdditionalParam(qID, getTimeOfQuery(qID), 2 * batchCounter - 1);
                incAdditionalParam(qID, getDistCompsForQuery(qID), 2 * batchCounter);
            }
            long timeOfQuery = getTimeOfQuery(qID);
            int dc = getDistCompsForQuery(qID);
            LOG.log(Level.INFO, "Evaluated query {0} in {1} ms with {2} dc", new Object[]{i, timeOfQuery, dc});
        }
        return ret;
    }

    private Map<Integer, TreeSet<Map.Entry<Comparable, Float>>[]> initAnswerMapForCandSetSizes(int candidatesProvided, int queriesCount, int batch) {
        Map<Integer, TreeSet<Map.Entry<Comparable, Float>>[]> ret = new TreeMap<>();
        if (candidatesProvided == -1) {
            ret.put(candidatesProvided, new TreeSet[queriesCount]);
        } else {
            for (int batchCurr = 0; batchCurr <= candidatesProvided; batchCurr += batch) {
                ret.put(batchCurr, new TreeSet[queriesCount]);
            }
        }
        return ret;
    }

    public void incTime(Comparable qId, long time) {
        AtomicLong ai = timesPerQueries.get(qId);
        if (ai != null) {
            ai.addAndGet(time);
        } else {
            timesPerQueries.put(qId, new AtomicLong(time));
        }
    }

    @Deprecated
    protected void incDistsComps(Comparable qId) {
        AtomicInteger ai = distCompsPerQueries.get(qId);
        if (ai != null) {
            ai.incrementAndGet();
        } else {
            distCompsPerQueries.put(qId, new AtomicInteger(1));
        }
    }

    protected void incDistsComps(Comparable qId, int byValue) {
        AtomicInteger ai = distCompsPerQueries.get(qId);
        if (ai != null) {
            ai.addAndGet(byValue);
        } else {
            ai = new AtomicInteger(byValue);
            distCompsPerQueries.put(qId, ai);
        }
    }

    public void resetDistComps(Comparable qId) {
        distCompsPerQueries.put(qId, new AtomicInteger());
    }

    public Map<Object, AtomicInteger> getDistCompsPerQueries() {
        return Collections.unmodifiableMap(distCompsPerQueries);
    }

    public Map<Object, AtomicLong> getTimesPerQueries() {
        return Collections.unmodifiableMap(timesPerQueries);
    }

    public int getDistCompsForQuery(Object qId) {
        return distCompsPerQueries.get(qId).get();
    }

    public long getTimeOfQuery(Object qId) {
        return timesPerQueries.get(qId).get();
    }

    public Map<Comparable, List<AtomicLong>> getAdditionalStats() {
        return Collections.unmodifiableMap(additionalStatsPerQueries);
    }

    public abstract String getResultName();

}
