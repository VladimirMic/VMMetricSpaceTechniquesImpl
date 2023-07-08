package vm.search.impl;

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
import vm.metricSpace.voronoiPartitioning.StorageLearnedVoronoiPartitioningInterface;
import vm.search.SearchingAlgorithm;

/**
 *
 * @author Vlada
 * @param <T> type of data used in the distance function
 */
public class VoronoiPartitionsCandSetIdentifier<T> extends SearchingAlgorithm<T> {

    private static final Logger LOG = Logger.getLogger(VoronoiPartitionsCandSetIdentifier.class.getName());

    private final Map<Object, Object> pivotsMap;
    private final DistanceFunctionInterface<T> df;
    private final Map<Object, TreeSet<Object>> voronoiPartitioning;

    public VoronoiPartitionsCandSetIdentifier(List pivots, DistanceFunctionInterface<T> df, String datasetName, AbstractMetricSpace<T> metricSpace, StorageLearnedVoronoiPartitioningInterface voronoiPartitioningStorage, int pivotCountUsedForVoronoiLearning) {
        pivotsMap = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(metricSpace, pivots, true);
        this.df = df;
        voronoiPartitioning = voronoiPartitioningStorage.load(datasetName, pivotCountUsedForVoronoiLearning);
    }

    public VoronoiPartitionsCandSetIdentifier(Dataset dataset, StorageLearnedVoronoiPartitioningInterface voronoiPartitioningStorage, int pivotCountUsedForVoronoiLearning) {
        this(dataset.getPivots(pivotCountUsedForVoronoiLearning), dataset.getDistanceFunction(), dataset.getDatasetName(), dataset.getMetricSpace(), voronoiPartitioningStorage, pivotCountUsedForVoronoiLearning);
    }

    @Override
    public TreeSet<Map.Entry<Object, Float>> completeKnnSearch(AbstractMetricSpace<T> metricSpace, Object queryObject, int k, Iterator<Object> objects, Object... additionalParams) {
        throw new UnsupportedOperationException("Not supported and will not be.");
    }

    /**
     *
     * @param metricSpace
     * @param fullQueryObject
     * @param k maximum size - never returnes bigger answer
     * @param ignored ignored!
     * @return
     */
    @Override
    public List<Object> candSetKnnSearch(AbstractMetricSpace<T> metricSpace, Object fullQueryObject, int k, Iterator<Object> ignored) {
        T qData = metricSpace.getDataOfMetricObject(fullQueryObject);
        Object[] pivotPerm = ToolsMetricDomain.getPivotIDsPermutation(df, pivotsMap, qData, -1);
        List<Object> ret = new ArrayList<>();
        int idxOfNext = 0;
        TreeSet<Object> nextCell = null;
        while ((nextCell == null || ret.size() + nextCell.size() < k) && idxOfNext < voronoiPartitioning.size() - 1) {
            if (nextCell != null) {
                ret.addAll(nextCell);
            }
            nextCell = voronoiPartitioning.get(pivotPerm[idxOfNext]);
            idxOfNext++;
        }
        if (ret.isEmpty()) {
            ret.addAll(nextCell);
        }
        LOG.log(Level.FINE, "Returning the cand set with {0} objects. It is made of {1} cells", new Object[]{ret.size(), idxOfNext});
        return ret;
    }

    public int getNumberOfPivots() {
        return pivotsMap.size();
    }

}
