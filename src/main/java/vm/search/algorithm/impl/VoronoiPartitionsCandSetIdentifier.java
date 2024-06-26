package vm.search.algorithm.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.search.algorithm.SearchingAlgorithm;
import vm.metricSpace.datasetPartitioning.StorageDatasetPartitionsInterface;

/**
 *
 * @author Vlada
 * @param <T> type of data used in the distance function
 */
public class VoronoiPartitionsCandSetIdentifier<T> extends SearchingAlgorithm<T> {

    private static final Logger LOG = Logger.getLogger(VoronoiPartitionsCandSetIdentifier.class.getName());

    protected final Map<Comparable, T> pivotsMap;
    protected final DistanceFunctionInterface<T> df;
    protected final Map<Comparable, TreeSet<Comparable>> datasetPartitioning;

    public VoronoiPartitionsCandSetIdentifier(List pivots, DistanceFunctionInterface<T> df, String datasetName, AbstractMetricSpace<T> metricSpace, StorageDatasetPartitionsInterface voronoiPartitioningStorage, int pivotCountUsedForVoronoiLearning) {
        pivotsMap = ToolsMetricDomain.getMetricObjectsAsIdDataMap(metricSpace, pivots);
        this.df = df;
        datasetPartitioning = voronoiPartitioningStorage.load(datasetName, pivotCountUsedForVoronoiLearning);
    }

    public VoronoiPartitionsCandSetIdentifier(Dataset dataset, StorageDatasetPartitionsInterface voronoiPartitioningStorage, int pivotCountUsedForVoronoiLearning) {
        this(dataset.getPivots(pivotCountUsedForVoronoiLearning), dataset.getDistanceFunction(), dataset.getDatasetName(), dataset.getMetricSpace(), voronoiPartitioningStorage, pivotCountUsedForVoronoiLearning);
    }

    /**
     *
     * @param metricSpace
     * @param fullQueryObject
     * @param k maximum size - never returnes bigger answer
     * @param ignored ignored!
     * @param additionalParams
     * @return
     */
    @Override
    public List<Comparable> candSetKnnSearch(AbstractMetricSpace<T> metricSpace, Object fullQueryObject, int k, Iterator<Object> ignored, Object... additionalParams) {
        T qData = metricSpace.getDataOfMetricObject(fullQueryObject);
        Map<Object, Float> distsToPivots = null;
        for (Object param : additionalParams) {
            if (param instanceof Map) {
                distsToPivots = (Map<Object, Float>) param;
            }
        }
        Comparable[] priorityQueue = evaluateKeyOrdering(df, pivotsMap, qData, distsToPivots);
        List<Comparable> ret = new ArrayList<>();
        int idxOfNext = 0;
        TreeSet<Comparable> nextCell = null;
        while ((nextCell == null || ret.size() + nextCell.size() < k) && idxOfNext < priorityQueue.length) {
            if (nextCell != null) {
                ret.addAll(nextCell);
            }
            nextCell = datasetPartitioning.get(priorityQueue[idxOfNext]);
            idxOfNext++;
        }
        if (ret.isEmpty()) {
            ret.addAll(nextCell);
        }
        LOG.log(Level.FINE, "Returning candSet with {0} objects. It is made of {1} cells", new Object[]{ret.size(), idxOfNext});
        return ret;
    }

    public Comparable[] evaluateKeyOrdering(DistanceFunctionInterface<T> df, Map<Comparable, T> pivotsMap, T qData, Object... params) {
        Map<Comparable, Float> distsToPivots = null;
        if (params.length > 0) {
            distsToPivots = (Map<Comparable, Float>) params[1];
        }
        return ToolsMetricDomain.getPivotIDsPermutation(df, pivotsMap, qData, -1, distsToPivots);
    }

    public int getNumberOfPivots() {
        return pivotsMap.size();
    }

    @Override
    public String getResultName() {
        return "VoronoiPartitionsCandSetIdentifier";
    }

}
