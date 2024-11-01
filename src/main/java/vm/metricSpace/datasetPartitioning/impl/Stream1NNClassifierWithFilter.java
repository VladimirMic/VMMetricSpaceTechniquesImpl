package vm.metricSpace.datasetPartitioning.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.datasetPartitioning.impl.batchProcessor.AbstractPivotBasedPartitioningProcessor;
import vm.metricSpace.datasetPartitioning.impl.batchProcessor.VoronoiPartitioningWithFilterProcessor;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.metricSpace.distance.bounding.BoundsOnDistanceEstimation;

/**
 *
 * @author au734419
 * @param <T>
 */
public class Stream1NNClassifierWithFilter<T> extends VoronoiPartitioningWithoutFilter<T> {

    public static final Logger LOG = Logger.getLogger(Stream1NNClassifierWithFilter.class.getName());

    protected final BoundsOnDistanceEstimation filter;
    protected final int pivotCountForFilter;

    public Stream1NNClassifierWithFilter(AbstractMetricSpace<T> metricSpace, DistanceFunctionInterface<T> df, int pivotCountForFilter, List<Object> centroids, BoundsOnDistanceEstimation filter) {
        super(metricSpace, df, centroids);
        this.filter = filter;
        this.pivotCountForFilter = pivotCountForFilter;
    }

    @Override
    public String getName() {
        String ret = "Stream_kNN_partitioning_";
        if (filter != null) {
            ret += filter.getTechFullName();
        }
        return ret;
    }

    @Override
    protected String getSuffixForOutputFileName() {
        if (filter == null) {
            return "";
        }
        return filter.getTechFullName();
    }

    @Override
    public String getAdditionalStats(AbstractPivotBasedPartitioningProcessor[] processes) {
        int ret = 0;
        for (AbstractPivotBasedPartitioningProcessor process : processes) {
            VoronoiPartitioningWithFilterProcessor cast = (VoronoiPartitioningWithFilterProcessor) process;
            ret += cast.getLbCheckedBatch();
        }
        lastAdditionalStats = Integer.toString(ret);
        return lastAdditionalStats;
    }

    @Override
    protected AbstractPivotBasedPartitioningProcessor getBatchProcesor(List batch, AbstractMetricSpace metricSpace, CountDownLatch latch, int classesCount, float[] pivotLengths, Map<Comparable, Float> objectsLengths) {
        return new VoronoiPartitioningWithFilterProcessor(batch, metricSpace, df, latch, pivotsData, pivotLengths, objectsLengths, filter, pivotCountForFilter);
    }

}
