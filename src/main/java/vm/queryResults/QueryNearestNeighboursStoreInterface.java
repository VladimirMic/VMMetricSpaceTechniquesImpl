package vm.queryResults;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import vm.search.algorithm.impl.GroundTruthEvaluator;
import vm.metricSpace.AbstractMetricSpace;

/**
 * Stores (found) nearest neighbours of the query object
 *
 * @author Vlada
 */
public abstract class QueryNearestNeighboursStoreInterface {

    public abstract void storeQueryResult(Object queryObjectID, TreeSet<Map.Entry<Object, Float>> queryResults, Integer k, String datasetName, String querySetName, String resultsName);

    public abstract Map<String, TreeSet<Map.Entry<Object, Float>>> getQueryResultsForDataset(String resultSetName, String datasetName, String querySetName, Integer k);

    public void storeQueryResults(List<Object> queryObjectsIDs, TreeSet<Map.Entry<Object, Float>>[] queryResults, Integer k, String datasetName, String querySetName, String resultsName) {
        for (int i = 0; i < queryObjectsIDs.size(); i++) {
            storeQueryResult(queryObjectsIDs.get(i), queryResults[i], k, datasetName, querySetName, resultsName);
        }
    }

    public void storeQueryResults(AbstractMetricSpace metricSpace, List<Object> queries, TreeSet<Map.Entry<Object, Float>>[] queryResults, Integer k, String datasetName, String querySetName, String resultsName) {
        List<Object> queryObjectsIDs = new ArrayList<>();
        for (int i = 0; i < queries.size(); i++) {
            queryObjectsIDs.add(metricSpace.getIDOfMetricObject(queries.get(i)));
        }
        storeQueryResults(queryObjectsIDs, queryResults, k, datasetName, querySetName, resultsName);
    }

    public Map<String, TreeSet<Map.Entry<Object, Float>>> getGroundTruthForDataset(String datasetName, String querySetName) {
        return getQueryResultsForDataset("ground_truth", datasetName, querySetName, GroundTruthEvaluator.K_IMPLICIT_FOR_GROUND_TRUTH);
    }

}
