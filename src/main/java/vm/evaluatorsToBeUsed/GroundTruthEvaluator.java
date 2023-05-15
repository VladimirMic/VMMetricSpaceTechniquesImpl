package vm.evaluatorsToBeUsed;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.queryResults.QueryNearestNeighboursStoreInterface;

/**
 *
 * @author Vlada
 */
public class GroundTruthEvaluator {

    private static final Logger LOG = Logger.getLogger(GroundTruthEvaluator.class.getName());
    public static final Integer BATCH_SIZE = 50000;
    public static final Integer PARALELISM = 1;

    private final AbstractMetricSpace metricSpace;
    private final DistanceFunctionInterface distanceFunction;
    private final QueryNearestNeighboursStoreInterface storage;
    private final List<Object> queryObjectsIDs;
    private final List<Object> queryObjectsData;
    private final int k;
    private final float range;

    public GroundTruthEvaluator(AbstractMetricSpace metricSpace, DistanceFunctionInterface distanceFunction, List<Object> queryObjects, int k, QueryNearestNeighboursStoreInterface storage) {
        this(metricSpace, distanceFunction, queryObjects, k, Float.MAX_VALUE, storage);
    }

    public GroundTruthEvaluator(AbstractMetricSpace metricSpace, DistanceFunctionInterface distanceFunction, List<Object> queryObjects, float range, QueryNearestNeighboursStoreInterface storage) {
        this(metricSpace, distanceFunction, queryObjects, Integer.MAX_VALUE, range, storage);
    }

    /**
     *
     * @param metricSpace implementation of your matric space
     * @param distanceFunction
     * @param queryObjects
     * @param k
     * @param range
     * @param storage Interface used to storeMetricObject the computed PCA
     * transformation
     */
    public GroundTruthEvaluator(AbstractMetricSpace metricSpace, DistanceFunctionInterface distanceFunction, List<Object> queryObjects, int k, float range, QueryNearestNeighboursStoreInterface storage) {
        this.metricSpace = metricSpace;
        this.storage = storage;
        this.queryObjectsIDs = getIDsOfObjects(queryObjects);
        this.queryObjectsData = getDataOfObjects(queryObjects);
        this.k = k;
        this.range = range;
        this.distanceFunction = distanceFunction;
    }

    public TreeSet<Entry<Object, Float>>[] evaluateIteratorInParallel(Iterator<Object> itOverMetricObjects, Object... paramsToStoreWithGroundTruth) {
        ExecutorService threadPool = vm.javatools.Tools.initExecutor(PARALELISM);
        TreeSet<Entry<Object, Float>>[] queryResults = initKNNResultingMaps(queryObjectsData.size());
        int counter = 0;
        while (itOverMetricObjects.hasNext()) {
            List<Object> batch = Tools.getObjectsFromIterator(0, BATCH_SIZE, itOverMetricObjects);
            queryResults = processBatch(batch, queryResults, threadPool);
            counter += batch.size();
            LOG.log(Level.INFO, "Evaluated queries for {0} objects from the dataset", counter);
        }
        if (storage != null) {
            String datasetName = null;
            String querySetName = null;
            if (paramsToStoreWithGroundTruth.length > 1) {
                datasetName = paramsToStoreWithGroundTruth[0].toString();
                querySetName = paramsToStoreWithGroundTruth[1].toString();
            }
            storage.storeQueryResults(queryObjectsIDs, queryResults, datasetName, querySetName, "ground_truth");
        }
        threadPool.shutdown();
        return queryResults;
    }

    private TreeSet<Entry<Object, Float>>[] processBatch(List<Object> batch, TreeSet<Entry<Object, Float>>[] queryResults, ExecutorService threadPool) {
        try {
            LOG.log(Level.INFO, "Start parallel evaluation of queries. Batch size: {0} metric objects", batch.size());
            CountDownLatch latch = new CountDownLatch(queryObjectsData.size());
            for (int i = 0; i < queryObjectsData.size(); i++) {
                final Object queryObject = queryObjectsData.get(i);
                final TreeSet<Entry<Object, Float>> queryResult = queryResults[i];
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        for (final Object obj : batch) {
                            updateSimQueryAnswer(queryObject, obj, queryResult);
                        }
                        latch.countDown();
                    }
                });
            }
            latch.await();
            LOG.log(Level.INFO, "Batch processed");
            return queryResults;
        } catch (InterruptedException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private TreeSet<Entry<Object, Float>>[] initKNNResultingMaps(int size) {
        TreeSet<Entry<Object, Float>>[] ret = new TreeSet[size];
        for (int i = 0; i < size; i++) {
            ret[i] = new TreeSet<>(new Tools.MapByValueComparator());
        }
        return ret;
    }

    private void updateSimQueryAnswer(Object queryObjectData, Object obj, TreeSet<Entry<Object, Float>> queryResult) {
        Object datasetObjData = metricSpace.getDataOfMetricObject(obj);
        float distance = distanceFunction.getDistance(queryObjectData, datasetObjData);
        if (distance > range) {
            return;
        }
        Object idOfMetricObject = metricSpace.getIDOfMetricObject(obj);
        Entry entry = new AbstractMap.SimpleEntry<>(idOfMetricObject, distance);
        if (queryResult.size() < k) {
            queryResult.add(entry);
            return;
        }
        float distThreshold = queryResult.last().getValue();
        if (distance < distThreshold) {
            queryResult.add(entry);
            queryResult.remove(queryResult.last());
        }
    }

    private List<Object> getDataOfObjects(List<Object> queryObjects) {
        List<Object> ret = new ArrayList<>();
        for (Object queryObject : queryObjects) {
            ret.add(metricSpace.getDataOfMetricObject(queryObject));
        }
        return ret;
    }

    private List<Object> getIDsOfObjects(List<Object> queryObjects) {
        List<Object> ret = new ArrayList<>();
        for (Object queryObject : queryObjects) {
            ret.add(metricSpace.getIDOfMetricObject(queryObject));
        }
        return ret;
    }

}
