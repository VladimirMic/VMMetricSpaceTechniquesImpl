package vm.search.algorithm.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.Dataset;
import vm.searchSpace.ToolsSpaceDomain;
import vm.searchSpace.distance.AbstractDistanceFunction;
import vm.search.algorithm.SearchingAlgorithm;
import vm.searchSpace.datasetPartitioning.StorageDatasetPartitionsInterface;

/**
 *
 * @author Vlada
 * @param <T> type of data used in the distance function
 */
public class VoronoiPartitionsCandSetIdentifier<T> extends SearchingAlgorithm<T> {

    private static final Logger LOG = Logger.getLogger(VoronoiPartitionsCandSetIdentifier.class.getName());

    protected final Map<Comparable, T> pivotsMap;
    protected final AbstractDistanceFunction<T> df;
    protected final Map<Comparable, TreeSet<Comparable>> datasetPartitioning;

    public VoronoiPartitionsCandSetIdentifier(List pivots, AbstractDistanceFunction<T> df, String datasetName, AbstractSearchSpace<T> searchSpace, StorageDatasetPartitionsInterface voronoiPartitioningStorage, int pivotCountUsedForVoronoiLearning) {
        pivotsMap = ToolsSpaceDomain.getSearchObjectsAsIdDataMap(searchSpace, pivots);
        this.df = df;
        datasetPartitioning = voronoiPartitioningStorage.loadAsTreeSets(datasetName, pivotCountUsedForVoronoiLearning);
    }

    public VoronoiPartitionsCandSetIdentifier(Dataset dataset, StorageDatasetPartitionsInterface voronoiPartitioningStorage, int pivotCountUsedForVoronoiLearning) {
        this(dataset.getPivots(pivotCountUsedForVoronoiLearning), dataset.getDistanceFunction(), dataset.getDatasetName(), dataset.getSearchSpace(), voronoiPartitioningStorage, pivotCountUsedForVoronoiLearning);
    }

    /**
     *
     * @param searchSpace
     * @param fullQueryObject
     * @param k maximum size - never returnes bigger answer
     * @param ignored ignored!
     * @param additionalParams
     * @return
     */
    @Override
    public List<Comparable> candSetKnnSearch(AbstractSearchSpace<T> searchSpace, Object fullQueryObject, int k, Iterator<Object> ignored, Object... additionalParams) {
        T qData = searchSpace.getDataOfObject(fullQueryObject);
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

    public Comparable[] evaluateKeyOrdering(AbstractDistanceFunction<T> df, Map<Comparable, T> pivotsMap, T qData, Object... params) {
        Map<Comparable, Float> distsToPivots = null;
        if (params.length > 0) {
            distsToPivots = (Map<Comparable, Float>) params[1];
        }
        return ToolsSpaceDomain.getPivotIDsPermutation(df, pivotsMap, qData, -1, distsToPivots);
    }

    public int getNumberOfPivots() {
        return pivotsMap.size();
    }

    @Override
    public String getResultName() {
        return "VoronoiPartitionsCandSetIdentifier";
    }

}
