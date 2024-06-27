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
    private final Map<Comparable, T> keyValueStorage;
    private int maxCandSetSize = 0;

    public DatasetOfCandidates(Dataset origDataset, String newDatasetName, QueryNearestNeighboursStoreInterface resultsStorage, String resultFolderName, String directResultFileName, String trainingResultFolderName, String trainingDirectResultFileName) {
        this.origDataset = origDataset;
        datasetName = newDatasetName;
        this.keyValueStorage = origDataset.getKeyValueStorage();
        metricSpace = new MetricSpaceWithDiskBasedMap(origDataset.getMetricSpace(), keyValueStorage);
        metricSpacesStorage = origDataset.getMetricSpacesStorage();
        Map<Comparable, TreeSet<Map.Entry<Comparable, Float>>> queryResultsForDataset = resultsStorage.getQueryResultsForDataset(resultFolderName, directResultFileName, "", null);
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
    public Map<Comparable, T> getKeyValueStorage() {
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

    private Map<Object, List<Object>> transformToList(Map<Comparable, TreeSet<Map.Entry<Comparable, Float>>> queryResultsForDataset, boolean storeMaxCandSetSize) {
        Map<Object, List<Object>> ret = new HashMap<>();
        Set<Comparable> queryIDs = queryResultsForDataset.keySet();
        for (Comparable queryID : queryIDs) {
            TreeSet<Map.Entry<Comparable, Float>> candidates = queryResultsForDataset.get(queryID);
            List<Object> candsIDs = new ArrayList<>();
            for (Map.Entry<Comparable, Float> candidate : candidates) {
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
    public TreeSet<Map.Entry<String, Float>> evaluateSmallestDistances(int objectCount, int queriesCount, int retSize) {
        //objectCount and queriesCount here are ignored
        queriesCount = QUERIES_COUNT_FOR_SMALLEST_DISTS;
        int qRetSize = MAX_RETURNED_SMALLEST_DISTS_PER_Q;
        List<Object> queries = origDataset.getSampleOfDataset(Math.min(mapOfTrainingQueriesToCandidates.size(), queriesCount));
        DistanceFunctionInterface df = getDistanceFunction();
        Comparator<Map.Entry<String, Float>> comp = new Tools.MapByFloatValueComparator<>();
        TreeSet<Map.Entry<String, Float>> result = new TreeSet(comp);
        Map<Object, T> cache = new HashMap<>();
        Map<Comparable, T> qMap = ToolsMetricDomain.getMetricObjectsAsIdDataMap(metricSpace, queries);
        int distCounter = 0;
        int skippedQ = 0;
        for (Map.Entry<Object, List<Object>> entry : mapOfTrainingQueriesToCandidates.entrySet()) {
            TreeSet<Map.Entry<String, Float>> qResult = new TreeSet(comp);
            Object qID = entry.getKey();
            if (!qMap.containsKey(qID)) {
                skippedQ++;
                LOG.log(Level.INFO, "Skipped query object {0} during the learning ({1} in total)", new Object[]{qID, skippedQ});
                continue;
            }
            T qData = qMap.get(qID);
            List<Object> cands = entry.getValue();
            for (Object o : cands) {
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
            result.addAll(qResult);
        }
        retSize = (int) (RATIO_OF_SMALLES_DISTS * distCounter);

        while (result.size() > retSize) {
            LOG.log(Level.INFO, "Removing  dist {0}. Min / max dist is {1} / {2}", new Object[]{result.first().getValue(), result.first().getValue(), result.last().getValue()});
            result.remove(result.first());
        }
        cache.clear();
        LOG.log(Level.INFO, "Evaluated {0} distances", new Object[]{distCounter});
        return result;

    }

    @Override
    public boolean hasKeyValueStorage() {
        return true;
    }

    @Override
    public void deleteKeyValueStorage() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
