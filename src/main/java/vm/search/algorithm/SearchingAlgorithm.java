package vm.search.algorithm;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author Vlada
 * @param <T>
 */
public abstract class SearchingAlgorithm<T> {

    private static final Logger LOG = Logger.getLogger(SearchingAlgorithm.class.getName());
    public static final Integer K_IMPLICIT_FOR_QUERIES = 30;
    public static final Integer BATCH_SIZE = 5000000; //  5000000 simulates independent queries as data are not effectively cached in the CPU cache

    protected final ConcurrentHashMap<Comparable, AtomicInteger> distCompsPerQueries = new ConcurrentHashMap();
    protected final ConcurrentHashMap<Comparable, AtomicLong> timesPerQueries = new ConcurrentHashMap();
    protected final ConcurrentHashMap<Comparable, float[]> qpDistsCached = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<Comparable, int[]> qPivotPermutationCached = new ConcurrentHashMap<>();

    public abstract List<Comparable> candSetKnnSearch(AbstractMetricSpace<T> metricSpace, Object queryObject, int k, Iterator<Object> objects, Object... additionalParams);

    /**
     * Implicit implementation that just reranks the candidate set defined in
     * this algorithm. Feel free to override with another complete search.
     *
     * @param metricSpace
     * @param queryObject
     * @param k
     * @param objects
     * @param additionalParams
     * @return
     */
    public TreeSet<Map.Entry<Comparable, Float>> completeKnnSearch(AbstractMetricSpace<T> metricSpace, Object queryObject, int k, Iterator<Object> objects, Object... additionalParams) {
        List<Comparable> candSet = candSetKnnSearch(metricSpace, queryObject, k, objects, additionalParams);
        Dataset dataset = (Dataset) additionalParams[0];
        return rerankCandidateSet(metricSpace, queryObject, k, dataset.getDistanceFunction(), dataset.getKeyValueStorage(), candSet);
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

    public static float adjustAndReturnSearchRadiusAfterAddingOne(TreeSet<Map.Entry<Comparable, Float>> currAnswer, int k, float searchRadius) {
        int size = currAnswer.size();
        if (size < k) {
            return searchRadius;
        }
        if (size > k) {
            currAnswer.remove(currAnswer.last());
        }
        return currAnswer.last().getValue();
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

    public TreeSet<Map.Entry<Comparable, Float>> rerankCandidateSet(AbstractMetricSpace<T> metricSpace, Object queryObj, int k, DistanceFunctionInterface df, Map<Comparable, T> mapOfAllFullObjects, List<Comparable> candsIDs) {
        T queryObjData = metricSpace.getDataOfMetricObject(queryObj);
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
            T metricObjectData;
            try {
                metricObjectData = (T) mapOfAllFullObjects.get(candID);
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Something wrong happened in the data map. Trying to repeat it. CandID: " + candID, ex);
                continue;
            }
            float distance = df.getDistance(queryObjData, metricObjectData);
            if (distance < qRange) {
                ret.add(new AbstractMap.SimpleEntry<>(candID, distance));
                qRange = adjustAndReturnSearchRadiusAfterAddingOne(ret, k, qRange);
            }
        }
        return ret;
    }

    /**
     * @param metricSpace
     * @param queryObjects
     * @param k
     * @param objects
     * @param additionalParams -- voluntarily the first one can be the
     * parallelisation.
     * @return evaluates all query objects in parallel. Parallelisation is done
     * over the query objects
     */
    public TreeSet<Map.Entry<Comparable, Float>>[] completeKnnFilteringWithQuerySet(AbstractMetricSpace<T> metricSpace, List<Object> queryObjects, int k, Iterator<Object> objects, Object... additionalParams) {
        final TreeSet<Map.Entry<Comparable, Float>>[] ret = new TreeSet[queryObjects.size()];
        final List<Object> batch = new ArrayList<>();
        for (int i = 0; i < queryObjects.size(); i++) {
            Comparable qID = metricSpace.getIDOfMetricObject(queryObjects.get(i));
            timesPerQueries.put(qID, new AtomicLong());
            ret[i] = new TreeSet<>(new Tools.MapByFloatValueComparator());
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
                final AbstractMetricSpace<T> metricSpaceFinal = metricSpace;
                t = -System.currentTimeMillis();
                for (int i = 0; i < queryObjects.size(); i++) {
                    final Object queryObject = queryObjects.get(i);
                    final TreeSet<Map.Entry<Comparable, Float>> answerToQuery = ret[i];
                    threadPool.execute(() -> {
                        TreeSet<Map.Entry<Comparable, Float>> completeKnnSearch = completeKnnSearch(metricSpaceFinal, queryObject, k, batch.iterator(), answerToQuery, additionalParams);
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
     * @param metricSpace
     * @param queryObjects
     * @param k
     * @param kCandSetMaxSize
     * @param keyValueStorage
     * @param additionalParams
     * @return
     */
    public TreeSet<Map.Entry<Comparable, Float>>[] completeKnnSearchWithPartitioningForQuerySet(AbstractMetricSpace<T> metricSpace, List<Object> queryObjects, int k, int kCandSetMaxSize, Map<Object, T> keyValueStorage, Object... additionalParams) {
        final TreeSet<Map.Entry<Comparable, Float>>[] ret = new TreeSet[queryObjects.size()];
        ExecutorService threadPool = vm.javatools.Tools.initExecutor(vm.javatools.Tools.PARALELISATION);
        try {
            CountDownLatch latch = new CountDownLatch(queryObjects.size());
            final AbstractMetricSpace<T> metricSpaceFinal = metricSpace;
            for (int i = 0; i < queryObjects.size(); i++) {
                final Object queryObject = queryObjects.get(i);
                final int iFinal = i;
                threadPool.execute(() -> {
                    Object[] params = Tools.concatArrays(new Object[]{keyValueStorage, kCandSetMaxSize}, additionalParams);
                    ret[iFinal] = completeKnnSearch(metricSpaceFinal, queryObject, k, null, params);
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

    public TreeSet<Map.Entry<Object, Float>>[] evaluateIteratorsSequentiallyForEachQuery(Dataset dataset, List queryObjects, int k) {
        AbstractMetricSpace metricSpace = dataset.getMetricSpace();
        final TreeSet<Map.Entry<Object, Float>>[] ret = new TreeSet[queryObjects.size()];
        LOG.log(Level.INFO, "Warming up disk storage");
        for (int i = 0; i < 20; i++) { // for the sake of disk caching
            Object q = queryObjects.get(queryObjects.size() - 1 - i);
            Comparable qID = metricSpace.getIDOfMetricObject(q);
            Iterator candsIt = dataset.getMetricObjectsFromDataset(qID);
            while (candsIt.hasNext()) {
                Object cand = candsIt.next();
                Object tmp = metricSpace.getDataOfMetricObject(cand);
            }
        }
        for (int i = 0; i < queryObjects.size(); i++) {
            Object q = queryObjects.get(i);
            Comparable qID = metricSpace.getIDOfMetricObject(q);
            Iterator candsIt = dataset.getMetricObjectsFromDataset(qID);
            ret[i] = completeKnnSearch(metricSpace, q, k, candsIt);
            long timeOfQuery = getTimeOfQuery(qID);
            int dc = getDistCompsForQuery(qID);
            LOG.log(Level.INFO, "Evaluated query {0} in {1} ms with {2} dc", new Object[]{i, timeOfQuery, dc});
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

    public Map<Object, AtomicLong>[] getAddditionalStats() {
        return new HashMap[0];
    }

    public abstract String getResultName();

}
