package vm.searchSpace.datasetPartitioning.impl;

import java.util.List;
import java.util.logging.Logger;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.datasetPartitioning.impl.batchProcessor.AbstractPivotBasedPartitioningProcessor;
import vm.searchSpace.datasetPartitioning.impl.batchProcessor.VoronoiPartitioningWithFilterProcessor;
import vm.searchSpace.distance.DistanceFunctionInterface;
import vm.searchSpace.distance.bounding.BoundsOnDistanceEstimation;

/**
 *
 * @author au734419
 * @param <T>
 */
public class Stream1NNClassifierWithFilter<T> extends VoronoiPartitioningWithoutFilter<T> {

    public static final Logger LOG = Logger.getLogger(Stream1NNClassifierWithFilter.class.getName());

    protected final BoundsOnDistanceEstimation filter;
    protected final int pivotCountForFilter;
    protected final float[][] pivotPivotDists;

    public Stream1NNClassifierWithFilter(AbstractSearchSpace<T> searchSpace, DistanceFunctionInterface<T> df, int pivotCountForFilter, List<Object> centroids, BoundsOnDistanceEstimation filter) {
        super(searchSpace, df, centroids);
        this.filter = filter;
        this.pivotCountForFilter = pivotCountForFilter;
        pivotPivotDists = searchSpace.getDistanceMap(df, pivotsData, pivotsData, pivotCountForFilter, pivotsData.size());
    }

    @Override
    public String getName() {
        String ret = "Stream_kNN_partitioning_";
        if (filter != null) {
            ret += filter.getTechFullName();
        }
        return ret + getParalelism() + "par";
    }

    @Override
    protected String getSuffixForOutputFileName() {
        if (filter == null) {
            return "";
        }
        return filter.getTechFullName();
    }

    @Override
    public void setAdditionalStats(AbstractPivotBasedPartitioningProcessor[] processes) {
        float ret = 0;
        for (AbstractPivotBasedPartitioningProcessor process : processes) {
            VoronoiPartitioningWithFilterProcessor cast = (VoronoiPartitioningWithFilterProcessor) process;
            ret += cast.getLbCheckedBatchAvgC();
        }
        lastAdditionalStats = Float.toString(ret);
    }

    @Override
    protected AbstractPivotBasedPartitioningProcessor getBatchProcesor(AbstractSearchSpace searchSpace, int classesCount, float[] pivotLengths) {
        return new VoronoiPartitioningWithFilterProcessor(searchSpace, df, pivotsData, pivotPivotDists, pivotLengths, filter, pivotCountForFilter);
    }

}
