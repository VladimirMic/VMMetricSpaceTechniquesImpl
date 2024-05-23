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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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
public class DatasetOfCandidates<T> extends Dataset<T> {

    public static final Logger LOG = Logger.getLogger(DatasetOfCandidates.class.getName());
    public static final Integer QUERIES_COUNT_FOR_SMALLEST_DISTS = 100;
    public static final Integer MAX_RETURNED_SMALLEST_DISTS_PER_Q = 30;
    public static final Float RATIO_OF_SMALLES_DISTS = 0.4f / 100f;

    private final Dataset origDataset;
    private final Map<Object, List<Object>> mapOfQueriesToCandidates;
    private final Map<Object, List<Object>> mapOfTrainingQueriesToCandidates;
    private final Map<Object, Object> keyValueStorage;
    private int maxCandSetSize = 0;

    public DatasetOfCandidates(Dataset origDataset, String newDatasetName, QueryNearestNeighboursStoreInterface resultsStorage, String resultFolderName, String directResultFileName, String trainingResultFolderName, String trainingDirectResultFileName) {
        this.origDataset = origDataset;
        datasetName = newDatasetName;
        this.keyValueStorage = origDataset.getKeyValueStorage();
        metricSpace = new MetricSpaceWithDiskBasedMap(origDataset.getMetricSpace(), keyValueStorage);
        metricSpacesStorage = origDataset.getMetricSpacesStorage();
        Map<String, TreeSet<Map.Entry<Object, Float>>> queryResultsForDataset = resultsStorage.getQueryResultsForDataset(resultFolderName, directResultFileName, "", null);
        mapOfQueriesToCandidates = transformToList(queryResultsForDataset, true);
        if (trainingResultFolderName != null && trainingDirectResultFileName != null) {
            queryResultsForDataset = resultsStorage.getQueryResultsForDataset(trainingResultFolderName, trainingDirectResultFileName, "", null);
            mapOfTrainingQueriesToCandidates = transformToList(queryResultsForDataset, false);
        } else {
            mapOfTrainingQueriesToCandidates = null;
        }
    }

    public DatasetOfCandidates(Dataset origDataset, QueryNearestNeighboursStoreInterface resultsStorage, String resultFolderName, String directResultFileName, String trainingResultFolderName, String trainingDirectResultFileName) {
        this(origDataset, origDataset.getDatasetName(), resultsStorage, resultFolderName, directResultFileName, trainingResultFolderName, trainingDirectResultFileName);
    }

    @Override
    public Map<Object, Object> getKeyValueStorage() {
        return Collections.unmodifiableMap(keyValueStorage);
    }

    @Override
    /**
     * the first param has to be the ID of the query object
     */
    public Iterator<Object> getMetricObjectsFromDataset(Object... params) {
        if (params.length == 0) {
            Set ret = new HashSet();
            for (Map.Entry<Object, List<Object>> next : mapOfTrainingQueriesToCandidates.entrySet()) {
                ret.addAll(next.getValue());
            }
            return ret.iterator();
        }
        Object queryObjID = params[0];
        List<Object> candidatesIDs = mapOfQueriesToCandidates.get(queryObjID);
        return candidatesIDs.iterator();
    }

    @Override
    public List<Object> getSampleOfDataset(int objCount) {
        if (objCount < 0) {
            objCount = Integer.MAX_VALUE;
        }
        List<Object> ret = new ArrayList<>();
        Iterator<Map.Entry<Object, List<Object>>> it = mapOfTrainingQueriesToCandidates.entrySet().iterator();
        while (ret.size() < objCount && it.hasNext()) {
            List<Object> objs = it.next().getValue();
            ret.addAll(objs);
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

    private Map<Object, List<Object>> transformToList(Map<String, TreeSet<Map.Entry<Object, Float>>> queryResultsForDataset, boolean storeMaxCandSetSize) {
        Map<Object, List<Object>> ret = new HashMap<>();
        Set<String> queryIDs = queryResultsForDataset.keySet();
        for (String queryID : queryIDs) {
            TreeSet<Map.Entry<Object, Float>> candidates = queryResultsForDataset.get(queryID);
            List<Object> candsIDs = new ArrayList<>();
            for (Map.Entry<Object, Float> candidate : candidates) {
                candsIDs.add(candidate.getKey());
            }
            if (storeMaxCandSetSize) {
                maxCandSetSize = Math.max(maxCandSetSize, candsIDs.size());
            } else {
                candsIDs = candsIDs.subList(0, maxCandSetSize);
            }
            ret.put(queryID, candsIDs);
        }
        return ret;
    }

    @Override
    public float[] evaluateSampleOfRandomDistances(int objectCount, int distCount, List<Object[]> listWhereAddExaminedPairs) {
        float[] ret = new float[distCount];
        DistanceFunctionInterface df = getDistanceFunction();
        List<Object> queries = getQueryObjects(mapOfTrainingQueriesToCandidates.size());
        Random r = new Random();
        int qSize = queries.size();
        Map<Object, T> cache = new HashMap<>();
        for (int i = 0; i < ret.length; i++) {
            Object q = queries.get(r.nextInt(qSize));
            Object qID = metricSpace.getIDOfMetricObject(q);
            T qData;
            if (cache.containsKey(qID)) {
                qData = cache.get(qID);
            } else {
                qData = metricSpace.getDataOfMetricObject(q);
                cache.put(qID, qData);
            }
            List<Object> cands = mapOfTrainingQueriesToCandidates.get(qID);
            Object o = cands.get(r.nextInt(cands.size()));
            T oData;
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

// random distances between objects from clusters. Rather big distances, but hardly any meaning.
//    @Override
//    public TreeSet<Map.Entry<String, Float>> evaluateSmallestDistances(int objectCount, int queriesCount, int retSize) {
//        List<Object> metricObjects = getSampleOfDataset(objectCount + queriesCount);
//        List<Object> queriesSamples = metricObjects.subList(0, queriesCount);
//        List<Object> sampleObjects = metricObjects.subList(queriesCount, objectCount + queriesCount);
////        List<Object> sampleObjects = metricObjects.subList(0, objectCount);
////        List<Object> queriesSamples = metricObjects.subList(objectCount, objectCount + queriesCount);
//        DistanceFunctionInterface df = getDistanceFunction();
//        Comparator<Map.Entry<String, Float>> comp = new Tools.MapByFloatValueComparator<>();
//        TreeSet<Map.Entry<String, Float>> result = new TreeSet(comp);
//        Map<Object, T> cache = new HashMap<>();
//        for (int i = 0; i < sampleObjects.size(); i++) {
//            Object o = sampleObjects.get(i);
//            T oData;
//            if (cache.containsKey(o)) {
//                oData = cache.get(o);
//            } else {
//                oData = metricSpace.getDataOfMetricObject(o);
//                cache.put(o, oData);
//            }
//            Object oID = metricSpace.getIDOfMetricObject(o);
//            for (Object q : queriesSamples) {
//                Object qID = metricSpace.getIDOfMetricObject(q);
//                T qData;
//                if (cache.containsKey(qID)) {
//                    qData = cache.get(qID);
//                } else {
//                    qData = metricSpace.getDataOfMetricObject(q);
//                    cache.put(qID, qData);
//                }
//                if (qID.equals(oID)) {
//                    continue;
//                }
//                float dist = df.getDistance(oData, qData);
//                String key = oID + ";" + qID;
//                AbstractMap.SimpleEntry<String, Float> e = new AbstractMap.SimpleEntry(key, dist);
//                result.add(e);
//                while (result.size() > retSize) {
//                    result.remove(result.last());
//                }
//            }
//            if ((i + 1) % 500 == 0) {
//                LOG.log(Level.INFO, "Processed object {0} out of {1}", new Object[]{i + 1, sampleObjects.size()});
//            }
//        }
//        return result;
//    }
// Random sampling
    @Override
    public TreeSet<Map.Entry<String, Float>> evaluateSmallestDistances(int objectCount, int queriesCount, int retSize) {
        // all params here are ignored
        queriesCount = QUERIES_COUNT_FOR_SMALLEST_DISTS;
        int qRetSize = MAX_RETURNED_SMALLEST_DISTS_PER_Q;
        List<Object> queries = origDataset.getSampleOfDataset(Math.min(mapOfTrainingQueriesToCandidates.size(), queriesCount));
        Map<Object, Object> qMap = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(metricSpace, queries, true);
        DistanceFunctionInterface df = getDistanceFunction();
        Comparator<Map.Entry<String, Float>> comp = new Tools.MapByFloatValueComparator<>();
        TreeSet<Map.Entry<String, Float>> result = new TreeSet(comp);
        Map<Object, T> cache = new HashMap<>();
        int counter = 0;
        for (int i = 0; i < queriesCount; i++) {
            Object q = queries.get(i);
            Object qID = metricSpace.getIDOfMetricObject(q);
            Object qData = qMap.get(qID);
            counter++;
            List<Object> cands = mapOfTrainingQueriesToCandidates.get(qID);
            for (int j = 0; j < qRetSize; j++) {
                Object o = Tools.randomObject(cands);
                T oData;
                if (cache.containsKey(o)) {
                    oData = cache.get(o);
                } else {
                    oData = metricSpace.getDataOfMetricObject(o);
                    cache.put(o, oData);
                }
                Object oID = metricSpace.getIDOfMetricObject(o);
                if (qID.equals(oID)) {
                    continue;
                }
                float distance = df.getDistance(qData, oData);
                String key = oID + ";" + qID;
                if (distance == 0) {
                    continue;
                }
                AbstractMap.SimpleEntry<String, Float> distToNotice = new AbstractMap.SimpleEntry(key, distance);
                result.add(distToNotice);
            }
            if (counter % 50 == 0) {
                LOG.log(Level.INFO, "Processed {0} sampled queries out of {1}", new Object[]{counter, queries.size()});
            }
        }
        cache.clear();
        LOG.log(Level.INFO, "Evaluated {0} distances out of all {1} asked - diff is possible.", new Object[]{counter});
        return result;
    }

}
