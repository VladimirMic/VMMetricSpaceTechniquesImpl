/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.metricSpace;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.queryResults.QueryNearestNeighboursStoreInterface;

/**
 *
 * @author Vlada
 * @param <T>
 */
public abstract class DatasetOfCandidates<T> extends Dataset<T> {

    public static final Logger LOG = Logger.getLogger(DatasetOfCandidates.class.getName());
    public static final Integer QUERIES_COUNT_FOR_SMALLEST_DISTS = 100;
    public static final Integer SMALLEST_DISTS = 40000;
    public static final Integer MAX_RETURNED_SMALLEST_DISTS_PER_Q = 30;
    public static final Float RATIO_OF_SMALLES_DISTS = 0.4f / 100f;

    private final Dataset origDataset;
    private final Map<Comparable, Comparable[]> mapOfQueriesToCandidates;
    private final Map<Comparable, Comparable[]> mapOfTrainingQueriesToCandidates;
    private final Map<Comparable, T> keyValueStorage;

    private int maxNumberOfCandidatesToReturn = Integer.MAX_VALUE;

    public DatasetOfCandidates(Dataset origDataset, String newDatasetName, QueryNearestNeighboursStoreInterface resultsStorage, String resultFolderName, String directResultFileName, String trainingResultFolderName, String trainingDirectResultFileName) {
        super(
                newDatasetName,
                new MetricSpaceWithDiskBasedMap(origDataset.getMetricSpace(), origDataset.getKeyValueStorage()),
                origDataset.getMetricSpacesStorage()
        );
        this.origDataset = origDataset;
        this.keyValueStorage = origDataset.getKeyValueStorage();
        Map<Comparable, Comparable[]> map = getDiskBasedDatasetOfCandsMap(datasetName);
        if (map == null) {
            Map<Comparable, TreeSet<Map.Entry<Comparable, Float>>> queryResultsForDataset = null;
            try {
                queryResultsForDataset = resultsStorage.getQueryResultsForDataset(resultFolderName, directResultFileName, "", null);
            } catch (IllegalArgumentException e) {
                LOG.log(Level.WARNING, "The file with results for the dataset of candidates does not exist");
            }
            mapOfQueriesToCandidates = transformToDistBasedMap(queryResultsForDataset, true, datasetName);
        } else {
            mapOfQueriesToCandidates = map;
        }
        if (trainingResultFolderName != null && trainingDirectResultFileName != null) {
            String trainingStorageName = datasetName + "_training_data";
            map = getDiskBasedDatasetOfCandsMap(trainingStorageName);
            if (map == null) {
                Map<Comparable, TreeSet<Map.Entry<Comparable, Float>>> queryResultsForDataset = null;
                try {
                    queryResultsForDataset = resultsStorage.getQueryResultsForDataset(trainingResultFolderName, trainingDirectResultFileName, "", null);
                } catch (IllegalArgumentException e) {
                    LOG.log(Level.WARNING, "The file with training results for the dataset of candidates does not exist");
                }
                mapOfTrainingQueriesToCandidates = transformToDistBasedMap(queryResultsForDataset, false, trainingStorageName);
            } else {
                mapOfTrainingQueriesToCandidates = map;
            }
        } else {
            mapOfTrainingQueriesToCandidates = null;
        }
        System.gc();
    }

    public void setMaxNumberOfCandidatesToReturn(int maxNumberOfCandidatesToReturn) {
        this.maxNumberOfCandidatesToReturn = maxNumberOfCandidatesToReturn;
    }

    protected abstract Map<Comparable, Comparable[]> getDiskBasedDatasetOfCandsMap(String datasetName);

    protected abstract void materialiseMap(Map<Comparable, Comparable[]> map, String storageName);

    private Map<Comparable, Comparable[]> transformToDistBasedMap(Map<Comparable, TreeSet<Map.Entry<Comparable, Float>>> queryResultsForDataset, boolean storeMaxCandSetSize, String storageName) {
        if (queryResultsForDataset == null) {
            return null;
        }
        Map<Comparable, Comparable[]> ret = new TreeMap<>();
        Set<Comparable> queryIDs = queryResultsForDataset.keySet();
        int maxCandSetSize = Integer.MAX_VALUE;
        if (!storeMaxCandSetSize) {
            maxCandSetSize = mapOfQueriesToCandidates.entrySet().iterator().next().getValue().length;
        }
        for (Comparable queryID : queryIDs) {
            TreeSet<Map.Entry<Comparable, Float>> candidates = queryResultsForDataset.get(queryID);
            List<Comparable> candsIDs = new ArrayList<>();
            for (Map.Entry<Comparable, Float> candidate : candidates) {
                candsIDs.add(candidate.getKey());
            }
            if (!storeMaxCandSetSize) {
                candsIDs = candsIDs.subList(0, maxCandSetSize);
            }
            ret.put(queryID, candsIDs.toArray(new Comparable[0]));
        }
        materialiseMap(ret, storageName);
        return getDiskBasedDatasetOfCandsMap(storageName);
    }

    public DatasetOfCandidates(Dataset origDataset, QueryNearestNeighboursStoreInterface resultsStorage, String resultFolderName, String directResultFileName, String trainingResultFolderName, String trainingDirectResultFileName) {
        this(origDataset, origDataset.getDatasetName(), resultsStorage, resultFolderName, directResultFileName, trainingResultFolderName, trainingDirectResultFileName);
    }

    @Override
    public Map<Comparable, T> getKeyValueStorage() {
        return Collections.unmodifiableMap(keyValueStorage);
    }

    @Override
    /**
     * the first param has to be the ID of the query object
     */
    public Iterator<Object> getMetricObjectsFromDataset(Object... params) {
        if (params.length == 0) {
            Comparable[] ret = new Comparable[0];
            for (Map.Entry<Comparable, Comparable[]> next : mapOfTrainingQueriesToCandidates.entrySet()) {
                Tools.concatArrays(ret, next.getValue());
            }
            return new vm.javatools.Tools.ArrayIterator(ret);
        }
        Comparable queryObjID = (Comparable) params[0];
        Comparable[] ret = mapOfQueriesToCandidates.get(queryObjID);
        if (ret == null) {
            LOG.log(Level.SEVERE, "The dataset of candidates does not contain candidates for query {0}", queryObjID.toString());
            throw new IllegalArgumentException();
        }
        return new vm.javatools.Tools.ArrayIterator(ret, maxNumberOfCandidatesToReturn);
    }

    @Override
    public List<Object> getSampleOfDataset(int objCount) {
        if (objCount < 0) {
            objCount = Integer.MAX_VALUE;
        }
        List<Object> ret = new ArrayList<>();
        Iterator<Map.Entry<Comparable, Comparable[]>> it = mapOfTrainingQueriesToCandidates.entrySet().iterator();
        while (ret.size() < objCount && it.hasNext()) {
            Comparable[] objs = it.next().getValue();
            ret.addAll(Tools.arrayToList(objs));
        }
        ret = ret.subList(0, objCount);
        return ret;
    }

    @Override
    public List<Object> getQueryObjects(Object... params) {
        return origDataset.getQueryObjects(params);
    }

    @Override
    public List<Object> getPivots(int objCount) {
        return origDataset.getPivots(objCount);
    }

    @Override
    public DistanceFunctionInterface getDistanceFunction() {
        return origDataset.getDistanceFunction();
    }

    @Override
    public String getQuerySetName() {
        return origDataset.getQuerySetName();
    }

    @Override
    public String getPivotSetName() {
        return origDataset.getPivotSetName();
    }

    public Dataset getOrigDataset() {
        return origDataset;
    }

    @Override
    public int getPrecomputedDatasetSize() {
        LOG.log(Level.INFO, "Returning the size of original dataset");
        return origDataset.getPrecomputedDatasetSize();
    }

    @Override
    public int updateDatasetSize() {
        return origDataset.updateDatasetSize();
    }

    @Override
    public void storePivots(List<Object> pivots, String pivotSetNane, Object... additionalParamsToStoreWithNewPivotSet) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void storeQueryObjects(List<Object> queryObjs, String querySetName, Object... additionalParamsToStoreWithNewQuerySet) {
        throw new UnsupportedOperationException();
    }

    @Override
    public float[] evaluateSampleOfRandomDistances(int objectCount, int distCount, List<Object[]> listWhereAddExaminedPairs) {
        float[] ret = new float[distCount];
        DistanceFunctionInterface df = getDistanceFunction();
        List<Object> queries = new ArrayList<>(mapOfTrainingQueriesToCandidates.keySet());

        Random r = new Random();
        int qSize = queries.size();
        Map<Comparable, T> qCache = ToolsMetricDomain.getMetricObjectsAsIdDataMap(metricSpace, queries);
        Map<Comparable, T> cache = new HashMap<>();
        for (int i = 0; i < ret.length; i++) {
            Object q = queries.get(r.nextInt(qSize));
            Comparable qID = metricSpace.getIDOfMetricObject(q);
            T qData;
            if (qCache.containsKey(qID)) {
                qData = qCache.get(qID);
            } else {
                qData = metricSpace.getDataOfMetricObject(q);
                qCache.put(qID, qData);
            }
            Comparable[] cands = mapOfTrainingQueriesToCandidates.get(qID);
            Comparable o = cands[r.nextInt(cands.length)];
            T oData;
            if (vm.javatools.Tools.getRatioOfConsumedRam(false) > 0.95) {
                cache.clear();
                System.gc();
            }
            if (cache.containsKey(o)) {
                oData = cache.get(o);
            } else {
                oData = metricSpace.getDataOfMetricObject(o);
                cache.put(o, oData);
            }
            float distance = df.getDistance(qData, oData);
            ret[i] = distance;
            if (i % 100000 == 0) {
                LOG.log(Level.INFO, "Evaluated {0} distances out of {1}", new Object[]{i, ret.length});
            }
        }
        cache.clear();
        LOG.log(Level.INFO, "Returning {0} distances", new Object[]{ret.length});
        return ret;
    }
// random dists between queries and objects. Produces really small distances
//    @Override
//    public TreeSet<Map.Entry<String, Float>> evaluateSmallestDistances(int objectCount, int queriesCount, int retSize) {
//        DistanceFunctionInterface df = getDistanceFunction();
//        List<Object> queries = getQueryObjects(Math.min(mapOfQueriesToCandidates.size(), queriesCount));
//        Comparator<Map.Entry<String, Float>> comp = new Tools.MapByFloatValueComparator<>();
//        TreeSet<Map.Entry<String, Float>> ret = new TreeSet(comp);
//        Map<Object, T> cache = new HashMap<>();
//        int counter = 0;
//        for (int qIdx = 0; qIdx < queries.size(); qIdx++) {
//            Object q = queries.get(qIdx);
//            Object qID = metricSpace.getIDOfMetricObject(q);
//            T qData;
//            if (cache.containsKey(qID)) {
//                qData = cache.get(qID);
//            } else {
//                qData = metricSpace.getDataOfMetricObject(q);
//                cache.put(qID, qData);
//            }
//            List<Object> cands = mapOfQueriesToCandidates.get(qID);
//            for (int i = 0; i < Math.min(cands.size(), objectCount); i++) {
//                Object o = cands.get(i);
//                Object oID = metricSpace.getIDOfMetricObject(o);
//                if (qID.equals(oID)) {
//                    continue;
//                }
//                T oData;
//                if (cache.containsKey(o)) {
//                    oData = cache.get(o);
//                } else {
//                    oData = metricSpace.getDataOfMetricObject(o);
//                    cache.put(o, oData);
//                }
//                counter++;
//                float distance = df.getDistance(qData, oData);
//                String key = oID + ";" + qID;
//                AbstractMap.SimpleEntry<String, Float> e = new AbstractMap.SimpleEntry(key, distance);
//                ret.add(e);
//                while (ret.size() > retSize) {
//                    ret.remove(ret.last());
//                }
//                if (qIdx == 0 && i % 500 == 0) {
//                    LOG.log(Level.INFO, "Processed {0} sampled objects of the first query out of {1}", new Object[]{i, cands.size()});
//                }
//            }
//            if (qIdx % 50 == 0) {
//                LOG.log(Level.INFO, "Processed {0} sampled queries out of {1}", new Object[]{qIdx, queries.size()});
//            }
//        }
//        cache.clear();
//        LOG.log(Level.INFO, "Evaluated {0} distances out of all {1} asked - diff is possible.", new Object[]{counter, objectCount * queriesCount});
//        return ret;
//    }

// Random sampling
//    @Override
//    public TreeSet<Map.Entry<String, Float>> evaluateSmallestDistances(int objectCount, int queriesCount, int retSize) {
//        // all params here are ignored
//        queriesCount = QUERIES_COUNT_FOR_SMALLEST_DISTS;
//        int qRetSize = MAX_RETURNED_SMALLEST_DISTS_PER_Q;
//        List<Object> queries = origDataset.getSampleOfDataset(Math.min(mapOfTrainingQueriesToCandidates.size(), queriesCount));
//        Map<Object, Object> qMap = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(metricSpace, queries, true);
//        DistanceFunctionInterface df = getDistanceFunction();
//        Comparator<Map.Entry<String, Float>> comp = new Tools.MapByFloatValueComparator<>();
//        TreeSet<Map.Entry<String, Float>> result = new TreeSet(comp);
//        Map<Object, T> cache = new HashMap<>();
//        int counter = 0;
//        for (int i = 0; i < queriesCount; i++) {
//            Object q = queries.get(i);
//            Object qID = metricSpace.getIDOfMetricObject(q);
//            Object qData = qMap.get(qID);
//            counter++;
//            List<Object> cands = mapOfTrainingQueriesToCandidates.get(qID);
//            for (int j = 0; j < qRetSize; j++) {
//                Object o = Tools.randomObject(cands);
//                T oData;
//                if (cache.containsKey(o)) {
//                    oData = cache.get(o);
//                } else {
//                    oData = metricSpace.getDataOfMetricObject(o);
//                    cache.put(o, oData);
//                }
//                Object oID = metricSpace.getIDOfMetricObject(o);
//                if (qID.equals(oID)) {
//                    continue;
//                }
//                float distance = df.getDistance(qData, oData);
//                String key = oID + ";" + qID;
//                if (distance == 0) {
//                    continue;
//                }
//                AbstractMap.SimpleEntry<String, Float> distToNotice = new AbstractMap.SimpleEntry(key, distance);
//                result.add(distToNotice);
//            }
//            if (counter % 50 == 0) {
//                LOG.log(Level.INFO, "Processed {0} sampled queries out of {1}", new Object[]{counter, queries.size()});
//            }
//        }
//        cache.clear();
//        LOG.log(Level.INFO, "Evaluated {0} distances out of all {1} asked - diff is possible.", new Object[]{counter});
//        return result;
//    }
    @Override
    public TreeSet<Map.Entry<String, Float>> evaluateSmallestDistances(int objectCount, int queriesCount, int retSize) {
        // queriesCount and retSize here are ignored
        // objectCount is used as the maximum of the candidate set size per query
        queriesCount = QUERIES_COUNT_FOR_SMALLEST_DISTS;
        int qRetSize = MAX_RETURNED_SMALLEST_DISTS_PER_Q;
        DistanceFunctionInterface df = getDistanceFunction();
        Comparator<Map.Entry<String, Float>> comp = new Tools.MapByFloatValueComparator<>();
        TreeSet<Map.Entry<String, Float>> result = new TreeSet(comp);
        Map<Object, T> cache = new HashMap<>();
        int distCounter = 0;
        int qCount = 0;
        for (Map.Entry<Comparable, Comparable[]> entry : mapOfTrainingQueriesToCandidates.entrySet()) {
            qCount++;
            if (qCount == queriesCount) {
                break;
            }
            TreeSet<Map.Entry<String, Float>> qResult = new TreeSet(comp);
            Object qID = entry.getKey();
            T qData = metricSpace.getDataOfMetricObject(qID);
            if (qData == null) {
                LOG.log(Level.SEVERE, "Sample queries has to be from the test dataset");
            }
            Comparable[] cands = entry.getValue();
            for (int oCount = 0; oCount < Math.min(objectCount, cands.length); oCount++) {
                Comparable o = cands[oCount];
                Object oID = metricSpace.getIDOfMetricObject(o);
                if (qID.equals(oID)) {
                    continue;
                }
                T oData;
                if (cache.containsKey(o)) {
                    oData = cache.get(o);
                } else {
                    if (oCount % 1000 == 0) {
//                        System.err.print("oCount " + oCount + " out of " + objectCount + ", ");
                        if (vm.javatools.Tools.getRatioOfConsumedRam(false) > 0.95) {
                            System.gc();
                        }
                    }
                    oData = metricSpace.getDataOfMetricObject(o);
                    cache.put(o, oData);
                }
                distCounter++;
                float distance = df.getDistance(qData, oData);
                String key = oID + ";" + qID;
                if (distance == 0) {
                    continue;
                }
                AbstractMap.SimpleEntry<String, Float> distToNotice = new AbstractMap.SimpleEntry(key, distance);
                qResult.add(distToNotice);
                while (qResult.size() > qRetSize) {
                    qResult.remove(qResult.first());
                }
            }
            LOG.log(Level.INFO, "Processed query {0} out of {1}", new Object[]{qCount, queriesCount});
            result.addAll(qResult);
        }
        while (result.size() > SMALLEST_DISTS) {
            LOG.log(Level.INFO, "Removing  dist {0}. Min / max dist is {1} / {2}", new Object[]{result.first().getValue(), result.first().getValue(), result.last().getValue()});
            result.remove(result.first());
        }
        cache.clear();
        LOG.log(Level.INFO, "Evaluated {0} distances", new Object[]{distCounter});
        return result;

    }

    @Override
    public void deleteKeyValueStorage() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public int getRecommendedNumberOfPivotsForFiltering() {
        return -1;
    }

}
