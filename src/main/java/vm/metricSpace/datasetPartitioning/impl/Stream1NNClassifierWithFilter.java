package vm.metricSpace.datasetPartitioning.impl;

import java.util.List;
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
    protected final float[][] pivotPivotDists;

    public Stream1NNClassifierWithFilter(AbstractMetricSpace<T> metricSpace, DistanceFunctionInterface<T> df, int pivotCountForFilter, List<Object> centroids, BoundsOnDistanceEstimation filter) {
        super(metricSpace, df, centroids);
        this.filter = filter;
        this.pivotCountForFilter = pivotCountForFilter;
        pivotPivotDists = metricSpace.getDistanceMap(df, pivotsData, pivotsData, pivotCountForFilter, pivotsData.size());
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
    protected AbstractPivotBasedPartitioningProcessor getBatchProcesor(AbstractMetricSpace metricSpace, int classesCount, float[] pivotLengths) {
        return new VoronoiPartitioningWithFilterProcessor(metricSpace, df, pivotsData, pivotPivotDists, pivotLengths, filter, pivotCountForFilter);
    }

}
