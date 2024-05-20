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

    private final Dataset origDataset;
    private final Map<Object, List<Object>> mapOfQueriesToCandidates;
    private final Map<Object, Object> keyValueStorage;

    public DatasetOfCandidates(Dataset origDataset, String newDatasetName, QueryNearestNeighboursStoreInterface resultsStorage, String resultFolderName, String directResultFileName) {
        this.origDataset = origDataset;
        datasetName = newDatasetName;
        this.keyValueStorage = origDataset.getKeyValueStorage();
        metricSpace = new MetricSpaceWithDiskBasedMap(origDataset.getMetricSpace(), keyValueStorage);
        metricSpacesStorage = origDataset.getMetricSpacesStorage();
        Map<String, TreeSet<Map.Entry<Object, Float>>> queryResultsForDataset = resultsStorage.getQueryResultsForDataset(resultFolderName, directResultFileName, "", null);
        mapOfQueriesToCandidates = transformToList(queryResultsForDataset);
    }

    public DatasetOfCandidates(Dataset origDataset, QueryNearestNeighboursStoreInterface resultsStorage, String resultFolderName) {
        this.origDataset = origDataset;
        datasetName = origDataset.getDatasetName();
        this.keyValueStorage = origDataset.getKeyValueStorage();
        metricSpace = new MetricSpaceWithDiskBasedMap(origDataset.getMetricSpace(), keyValueStorage);
        metricSpacesStorage = origDataset.getMetricSpacesStorage();
        Map<String, TreeSet<Map.Entry<Object, Float>>> queryResultsForDataset = resultsStorage.getQueryResultsForDataset(resultFolderName, getDatasetName(), getQuerySetName(), null);
        mapOfQueriesToCandidates = transformToList(queryResultsForDataset);
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
            for (Map.Entry<Object, List<Object>> next : mapOfQueriesToCandidates.entrySet()) {
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
        Iterator<Map.Entry<Object, List<Object>>> it = mapOfQueriesToCandidates.entrySet().iterator();
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

    private Map<Object, List<Object>> transformToList(Map<String, TreeSet<Map.Entry<Object, Float>>> queryResultsForDataset) {
        Map<Object, List<Object>> ret = new HashMap<>();
        Set<String> queryIDs = queryResultsForDataset.keySet();
        for (String queryID : queryIDs) {
            TreeSet<Map.Entry<Object, Float>> candidates = queryResultsForDataset.get(queryID);
            List<Object> candsIDs = new ArrayList<>();
            for (Map.Entry<Object, Float> candidate : candidates) {
                candsIDs.add(candidate.getKey());
            }
            ret.put(queryID, candsIDs);
        }
        return ret;
    }

    @Override
    public float[] evaluateSampleOfRandomDistances(int objectCount, int distCount, List<Object[]> listWhereAddExaminedPairs) {
        float[] ret = new float[distCount];
        DistanceFunctionInterface df = getDistanceFunction();
        List<Object> queries = getQueryObjects(mapOfQueriesToCandidates.size());
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
            List<Object> cands = mapOfQueriesToCandidates.get(qID);
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
    @Override
    public TreeSet<Map.Entry<String, Float>> evaluateSmallestDistances(int objectCount, int queriesCount, int retSize) {
        DistanceFunctionInterface df = getDistanceFunction();
        List<Object> queries = getQueryObjects(mapOfQueriesToCandidates.size());
        Random r = new Random();
        int qSize = queries.size();
        Comparator<Map.Entry<String, Float>> comp = new Tools.MapByFloatValueComparator<>();
        TreeSet<Map.Entry<String, Float>> ret = new TreeSet(comp);
        long end = objectCount * queriesCount;
        int i = 0;
        Map<Object, T> cache = new HashMap<>();
        while (i < end) {
            Object q = queries.get(r.nextInt(qSize));
            Object qID = metricSpace.getIDOfMetricObject(q);
            List<Object> cands = mapOfQueriesToCandidates.get(qID);
            Object o = cands.get(r.nextInt(cands.size()));
            Object oID = metricSpace.getIDOfMetricObject(o);
            if (qID.equals(oID)) {
                continue;
            }
            T qData;
            if (cache.containsKey(qID)) {
                qData = cache.get(qID);
            } else {
                qData = metricSpace.getDataOfMetricObject(q);
                cache.put(qID, qData);
            }
            T oData;
            if (cache.containsKey(o)) {
                oData = cache.get(o);
            } else {
                oData = metricSpace.getDataOfMetricObject(o);
                cache.put(o, oData);
            }
            i++;
            float distance = df.getDistance(qData, oData);
            String key = oID + ";" + qID;
            AbstractMap.SimpleEntry<String, Float> e = new AbstractMap.SimpleEntry(key, distance);
            ret.add(e);
            while (ret.size() > retSize) {
                ret.remove(ret.last());
            }
            if (i % 100000 == 0) {
                LOG.log(Level.INFO, "Evaluated {0} distances out of {1}", new Object[]{i, end});
            }
        }
        cache.clear();
        LOG.log(Level.INFO, "Evaluated {0} distances", new Object[]{end});
        return ret;
    }

}
