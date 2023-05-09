package vm.search;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author Vlada
 * @param <T>
 */
public abstract class SearchingAlgorithm<T> {

    private static final Logger LOG = Logger.getLogger(SearchingAlgorithm.class.getName());
    public static final Integer PARALELISATION = 14;
    public static final Integer BATCH_SIZE = 100000;

    private final Map<Object, AtomicInteger> distCompsPerQueries = new HashMap<>();
    private final Map<Object, AtomicLong> timesPerQueries = new HashMap<>();

    public abstract TreeSet<Map.Entry<Object, Float>> completeKnnSearch(AbstractMetricSpace<T> metricSpace, Object queryObject, int k, Iterator<Object> objects, Object... additionalParams);

    public abstract List<Object> candSetKnnSearch(AbstractMetricSpace<T> metricSpace, Object queryObject, int k, Iterator<Object> objects);

    public Map.Entry<Object, Float> adjustAndReturnLastEntry(TreeSet<Map.Entry<Object, Float>> currAnswer, int k) {
        int size = currAnswer.size();
        if (size < k) {
            return null;
        }
        while (currAnswer.size() > k) {
            currAnswer.remove(currAnswer.last());
        }
        return currAnswer.last();
    }

    public float adjustAndReturnSearchRadius(TreeSet<Map.Entry<Object, Float>> currAnswer, int k) {
        int size = currAnswer.size();
        if (size < k) {
            return Float.MAX_VALUE;
        }
        while (currAnswer.size() > k) {
            currAnswer.remove(currAnswer.last());
        }
        return currAnswer.last().getValue();
    }

    public TreeSet<Map.Entry<Object, Float>> rerankCandidateSet(AbstractMetricSpace<T> metricSpace, Object queryObj, int k, String datasetName, Map<Object, Object> mapOfAllFullObjects, List<Object> candsIDs) {
        DistanceFunctionInterface df = metricSpace.getDistanceFunctionForDataset(datasetName);
        T queryObjData = metricSpace.getDataOfMetricObject(queryObj);
        TreeSet<Map.Entry<Object, Float>> ret = new TreeSet<>(new Tools.MapByValueComparator());
        if (mapOfAllFullObjects == null) {
            for (int i = 0; i < Math.min(candsIDs.size(), k); i++) {
                Object id = candsIDs.get(i);
                ret.add(new AbstractMap.SimpleEntry<>(id, (float) i));
            }
            return ret;
        }
        for (Object candID : candsIDs) {
            T metricObjectData;
            try {
                metricObjectData = (T) mapOfAllFullObjects.get(candID);
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Something wrong happened in the data map. Trying to repeat it. CandID: " + candID, ex);
                continue;
            }
            float distance = df.getDistance(queryObjData, metricObjectData);
            ret.add(new AbstractMap.SimpleEntry<>(candID, distance));
            if (ret.size() > k) {
                ret.remove(ret.last());
            }
        }
        return ret;
    }

    /**
     * @param metricSpace
     * @param queryObjects
     * @param k
     * @param objects
     * @param paramsToExtractDataFromMetricObject
     * @return
     */
    public TreeSet<Map.Entry<Object, Float>>[] completeKnnSearchOfQuerySet(AbstractMetricSpace<T> metricSpace, List<Object> queryObjects, int k, Iterator<Object> objects, Object... paramsToExtractDataFromMetricObject) {
        final TreeSet<Map.Entry<Object, Float>>[] ret = new TreeSet[queryObjects.size()];
        final List<Object> batch = new ArrayList<>();
        for (int i = 0; i < queryObjects.size(); i++) {
            Object qID = metricSpace.getIDOfMetricObject(queryObjects.get(i));
            timesPerQueries.put(qID, new AtomicLong());
            ret[i] = new TreeSet<>(new Tools.MapByValueComparator());
        }
        ExecutorService threadPool = vm.javatools.Tools.initExecutor(PARALELISATION);
        int batchCounter = 0;
        while (objects.hasNext()) {
            try {
                batch.clear();
                for (int i = 0; i < BATCH_SIZE && objects.hasNext(); i++) {
                    batch.add(objects.next());
                }
                if (batch.isEmpty()) {
                    break;
                }
                batchCounter++;
                CountDownLatch latch = new CountDownLatch(queryObjects.size());
                final AbstractMetricSpace<T> metricSpaceFinal = metricSpace;
                final int batchFinal = batchCounter;
                for (int i = 0; i < queryObjects.size(); i++) {
                    final Object queryObject = queryObjects.get(i);
                    Object qId = metricSpace.getIDOfMetricObject(queryObject);
                    final TreeSet<Map.Entry<Object, Float>> map = ret[i];
                    final int iFinal = i + 1;
                    threadPool.execute(() -> {
                        AtomicLong time = timesPerQueries.get(qId);
                        TreeSet<Map.Entry<Object, Float>> completeKnnSearch = completeKnnSearch(metricSpaceFinal, queryObject, k, batch.iterator(), map);
                        map.addAll(completeKnnSearch);
                        latch.countDown();
                        adjustAndReturnSearchRadius(map, k);
                        LOG.log(Level.INFO, "Query obj {0} evaluated in the batch {1} (batch size: {2})", new Object[]{iFinal, batchFinal, BATCH_SIZE});
                    });
                }
                latch.await();
                LOG.log(Level.INFO, "Batch {0} processed", batchCounter);
            } catch (InterruptedException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
        threadPool.shutdown();
        return ret;
    }

    public void incTime(Object qId, long time) {
        AtomicLong ai = timesPerQueries.get(qId);
        if (ai != null) {
            ai.addAndGet(time);
        } else {
            timesPerQueries.put(qId, new AtomicLong(time));
        }
    }

    protected void incDistsComps(Object qId) {
        AtomicInteger ai = distCompsPerQueries.get(qId);
        if (ai != null) {
            ai.incrementAndGet();
        } else {
            distCompsPerQueries.put(qId, new AtomicInteger(1));
        }
    }

    protected void incDistsComps(Object qId, int byValue) {
        AtomicInteger ai = distCompsPerQueries.get(qId);
        if (ai != null) {
            ai.addAndGet(byValue);
        } else {
            distCompsPerQueries.put(qId, new AtomicInteger(byValue));
        }
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

}