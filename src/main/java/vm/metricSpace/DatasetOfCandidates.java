/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.metricSpace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.queryResults.QueryNearestNeighboursStoreInterface;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class DatasetOfCandidates<T> extends Dataset<T> {

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
     * the first param has the be the ID of the query object
     */
    public Iterator<Object> getMetricObjectsFromDataset(Object... params) {
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
        Logger.getLogger(DatasetOfCandidates.class.getName()).log(Level.INFO, "Returning the size of original dataset");
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

}
