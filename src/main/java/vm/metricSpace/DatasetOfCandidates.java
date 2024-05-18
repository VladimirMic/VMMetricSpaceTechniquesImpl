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
        this.keyValueStorage = origDataset.getKeyValueStorage();        metricSpace = origDataset.getMetricSpace();
        metricSpacesStorage = origDataset.getMetricSpacesStorage();
        Map<String, TreeSet<Map.Entry<Object, Float>>> queryResultsForDataset = resultsStorage.getQueryResultsForDataset(resultFolderName, directResultFileName, "", null);
        mapOfQueriesToCandidates = transformToList(queryResultsForDataset);
    }

    public DatasetOfCandidates(Dataset origDataset, QueryNearestNeighboursStoreInterface resultsStorage, String resultFolderName) {
        this.origDataset = origDataset;
        datasetName = origDataset.getDatasetName();
        this.keyValueStorage = origDataset.getKeyValueStorage();
        metricSpace = origDataset.getMetricSpace();
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
        return new IteratorOverCandidates(candidatesIDs.iterator());
    }

    @Override
    public List<Object> getSampleOfDataset(int objCount) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Object> getQueryObjects(Object... params) {
        return origDataset.getQueryObjects(1000);
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
        throw new UnsupportedOperationException();
    }

    @Override
    public int updateDatasetSize() {
        throw new UnsupportedOperationException();
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

    private class IteratorOverCandidates implements Iterator<Object> {

        private final Iterator<Object> candsIDs;

        public IteratorOverCandidates(Iterator<Object> candsIDs) {
            this.candsIDs = candsIDs;
        }

        @Override
        public boolean hasNext() {
            return candsIDs.hasNext();
        }

        @Override
        public Object next() {
            if (!candsIDs.hasNext()) {
                throw new NoSuchElementException("No more objects in the map");
            }
            Object id = candsIDs.next();
            T value = (T) keyValueStorage.get(id);
            return metricSpace.createMetricObject(id, value);
        }

    }

}
