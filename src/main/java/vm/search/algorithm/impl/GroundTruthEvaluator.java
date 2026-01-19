package vm.search.algorithm.impl;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.Dataset;
import vm.searchSpace.DatasetOfCandidates;
import vm.searchSpace.distance.AbstractDistanceFunction;
import vm.search.algorithm.SearchingAlgorithm;
import static vm.search.algorithm.SearchingAlgorithm.adjustAndReturnSearchRadiusAfterAddingOne;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class GroundTruthEvaluator<T> extends SearchingAlgorithm<T> {

    private static final Logger LOG = Logger.getLogger(GroundTruthEvaluator.class.getName());

    public static final Integer K_IMPLICIT_FOR_GROUND_TRUTH = 1000;
    private final AbstractSearchSpace searchSpace;
    private final AbstractDistanceFunction distanceFunction;
    private final List<Object> queryObjects;
    private final int k;
    private final float range;

    public GroundTruthEvaluator(Dataset<T> dataset, int k) {
        this(dataset, k, Float.MAX_VALUE, -1);
    }

    public GroundTruthEvaluator(Dataset<T> dataset, float range) {
        this(dataset, Integer.MAX_VALUE, range, -1);
    }

    /**
     *
     * @param dataset
     * @param k
     * @param range transformation
     * @param maxQueryCount
     */
    public GroundTruthEvaluator(Dataset<T> dataset, int k, float range, int maxQueryCount) {
        this.searchSpace = dataset.getSearchSpace();
        this.queryObjects = dataset.getQueryObjects(maxQueryCount);
        this.k = k;
        this.range = range;
        this.distanceFunction = dataset.getDistanceFunction();
    }

    public GroundTruthEvaluator(AbstractDistanceFunction<T> distanceFunction) {
        this.distanceFunction = distanceFunction;
        searchSpace = null;
        queryObjects = null;
        k = -1;
        range = Float.MAX_VALUE;
    }

    /**
     * 
     * @param dataset
     * @return For each query objects returns a map of IDs and distances.
     */
    public TreeSet<Entry<Comparable, Float>>[] evaluateIteratorSequentially(Dataset dataset) {
        TreeSet<Map.Entry<Comparable, Float>>[] ret = null;
        int repetitions = SearchingAlgorithm.getNumberOfRepetitionsDueToCaching(dataset);
        if (dataset instanceof DatasetOfCandidates) {
            int precomputedDatasetSize = dataset.getPrecomputedDatasetSize();
            Map<Integer, TreeSet<Entry<Comparable, Float>>[]> allWithSteps = null;
            for (int i = 0; i < repetitions; i++) {
                allWithSteps = evaluateIteratorsSequentiallyForEachQuery(dataset, queryObjects, k, true, precomputedDatasetSize);
            }
            ret = allWithSteps.get(precomputedDatasetSize);
        } else {
            for (int i = 0; i < repetitions; i++) {
                ret = completeKnnFilteringWithQuerySet(searchSpace, queryObjects, k, dataset.getSearchObjectsFromDataset(), 1);
            }
        }
        return ret;
    }

    public TreeSet<Entry<Object, Float>>[] evaluateIteratorInParallel(Iterator<Object> itOverSearchObjects, Object... paramsToStoreWithGroundTruth) {
        Object[] concatArrays = Tools.addToArray(vm.javatools.Tools.PARALELISATION, paramsToStoreWithGroundTruth);
        return completeKnnFilteringWithQuerySet(searchSpace, queryObjects, k, itOverSearchObjects, concatArrays);
    }

    @Override
    public TreeSet<Map.Entry<Comparable, Float>> completeKnnSearch(AbstractSearchSpace<T> searchSpace, Object q, int k, Iterator<Object> objects, Object... params) {
        long t = -System.currentTimeMillis();
        TreeSet<Map.Entry<Comparable, Float>> ret = params.length == 0 || params[0] == null ? new TreeSet<>(new Tools.MapByFloatValueComparator()) : (TreeSet<Map.Entry<Comparable, Float>>) params[0];
        T qData = searchSpace.getDataOfObject(q);
        Comparable qId = searchSpace.getIDOfObject(q);
        int distComps = 0;
        float qRange = adjustAndReturnSearchRadiusAfterAddingOne(ret, k, range);
        objectsLoop:
        while (objects.hasNext()) {
            Object o = objects.next();
            Comparable oId = searchSpace.getIDOfObject(o);
            T oData = searchSpace.getDataOfObject(o);
            distComps++;
            float distance = distanceFunction.getDistance(qData, oData);
            if (distance < qRange) {
                ret.add(new AbstractMap.SimpleEntry<>(oId, distance));
                qRange = adjustAndReturnSearchRadiusAfterAddingOne(ret, k, range);
            }
        }
        t += System.currentTimeMillis();
        incTime(qId, t);
        incDistsComps(qId, distComps);
        LOG.log(Level.INFO, "Query executed in {0} ms", t);
        return ret;
    }

//    public TreeSet<Entry<Object, Float>>[] evaluateIterator(ExecutorService threadPool, Iterator<Object> itOverSearchObjects, Object... paramsToStoreWithGroundTruth) {
//        long t = -System.currentTimeMillis();
//        TreeSet<Entry<Object, Float>>[] queryResults = initKNNResultSets(queryObjectsData.size());
//        int counter = 0;
//        while (itOverSearchObjects.hasNext()) {
//            List<Object> batch = Tools.getObjectsFromIterator(0, BATCH_SIZE, itOverSearchObjects);
//            queryResults = processBatch(batch, queryResults, threadPool);
//            counter += batch.size();
//            LOG.log(Level.INFO, "Evaluated queries for {0} objects from the dataset", counter);
//            System.gc();
//        }
//        if (storage != null) {
//            String datasetName = null;
//            String querySetName = null;
//            if (paramsToStoreWithGroundTruth.length > 1) {
//                datasetName = paramsToStoreWithGroundTruth[0].toString();
//                querySetName = paramsToStoreWithGroundTruth[1].toString();
//            }
//            storage.storeQueryResults(queryObjectsIDs, queryResults, k, datasetName, querySetName, "ground_truth");
//        }
//        if (threadPool != null) {
//            threadPool.shutdown();
//        }
//        return queryResults;
//    }
//    private TreeSet<Entry<Object, Float>>[] processBatch(List<Object> batch, TreeSet<Entry<Object, Float>>[] queryResults, ExecutorService threadPool) {
//        try {
//            LOG.log(Level.INFO, "Start parallel evaluation of queries. Batch size: {0} search objects", batch.size());
//            CountDownLatch latch = new CountDownLatch(queryObjectsData.size());
//            for (int i = 0; i < queryObjectsData.size(); i++) {
//                final Object queryObjectData = queryObjectsData.get(i);
//                final Object qID = queryObjectsIDs.get(i);
//                final TreeSet<Entry<Object, Float>> queryResult = queryResults[i];
//                UpdateAnswers updateAnswers = new UpdateAnswers(batch, latch, queryObjectData, qID, queryResult);
//                if (threadPool != null) {
//                    threadPool.execute(updateAnswers);
//                } else {
//                    updateAnswers.run();
//                }
//            }
//            latch.await();
//            System.gc();
//            return queryResults;
//        } catch (Throwable ex) {
//            LOG.log(Level.SEVERE, null, ex);
//        }
//        return null;
//    }
    private void updateSimQueryAnswer(Object queryObjectData, Object obj, TreeSet<Entry<Object, Float>> queryResult) {
        Object datasetObjData = searchSpace.getDataOfObject(obj);
        float distance = distanceFunction.getDistance(queryObjectData, datasetObjData);
        if (distance > range) {
            return;
        }
        Object idOfSearchObject = searchSpace.getIDOfObject(obj);
        Entry entry = new AbstractMap.SimpleEntry<>(idOfSearchObject, distance);
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

    @Override
    public List candSetKnnSearch(AbstractSearchSpace spaceSpace, Object queryObject, int k, Iterator objects, Object... additionalParams) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public String getResultName() {
        return "ground_truth";
    }

    private class UpdateAnswers implements Runnable {

        private final List<Object> batch;
        private final CountDownLatch latch;
        private final Object qData;
        private final Object qID;
        private final TreeSet<Entry<Object, Float>> queryResult;

        public UpdateAnswers(List<Object> batch, CountDownLatch latch, Object qData, Object qID, TreeSet<Entry<Object, Float>> queryResult) {
            this.batch = batch;
            this.latch = latch;
            this.qData = qData;
            this.qID = qID;
            this.queryResult = queryResult;
        }

        @Override
        public void run() {
            long t = -System.currentTimeMillis();
            for (int i1 = 0; i1 < batch.size(); i1++) {
                final Object obj = batch.get(i1);
                updateSimQueryAnswer(qData, obj, queryResult);
            }
            t += System.currentTimeMillis();
            LOG.log(Level.INFO, "Batch for a query {2} evaluated in {1} ms. Remains {0} queries", new Object[]{latch.getCount(), t, qID});
            latch.countDown();
        }

    }

}
