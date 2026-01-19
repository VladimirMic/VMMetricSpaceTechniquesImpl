/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.searchSpace.datasetPartitioning.impl.batchProcessor;

import java.util.List;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.distance.AbstractDistanceFunction;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class BruteForceVoronoiPartitioningProcessor<T> extends AbstractPivotBasedPartitioningProcessor<T> {

    public BruteForceVoronoiPartitioningProcessor(
            AbstractSearchSpace<T> searchSpace,
            AbstractDistanceFunction df,
            List<T> pivotData,
            float[] pivotLengths) {
        super(searchSpace, df, pivotData, pivotData.size(), pivotLengths);
    }

    @Override
    protected float getDistIfSmallerThan(float radius, T oData, T pData, Float oLength, Float pLength, float[] opDists, int pIdx) {
        return df.getDistance(oData, pData, oLength, pLength);
    }

}
