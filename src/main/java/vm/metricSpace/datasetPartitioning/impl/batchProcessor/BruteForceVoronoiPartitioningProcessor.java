/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.metricSpace.datasetPartitioning.impl.batchProcessor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class BruteForceVoronoiPartitioningProcessor<T> extends AbstractPivotBasedPartitioningProcessor<T> {

    public BruteForceVoronoiPartitioningProcessor(
            List batch,
            AbstractMetricSpace<T> metricSpace,
            DistanceFunctionInterface df,
            CountDownLatch latch,
            List<T> pivotData,
            float[] pivotLengths,
            Map<Comparable, Float> objectsLengths) {
        super(batch, metricSpace, df, latch, pivotData, pivotData.size(), pivotLengths, objectsLengths);
    }

    @Override
    protected float getDistIfSmallerThan(float radius, T oData, T pData, Float oLength, Float pLength, float[] opDists, int pIdx) {
        return df.getDistance(oData, pData, oLength, pLength);
    }

}
