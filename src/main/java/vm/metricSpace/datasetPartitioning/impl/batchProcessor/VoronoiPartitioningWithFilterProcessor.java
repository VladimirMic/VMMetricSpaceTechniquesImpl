/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.metricSpace.datasetPartitioning.impl.batchProcessor;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.metricSpace.distance.bounding.BoundsOnDistanceEstimation;
import vm.metricSpace.distance.bounding.onepivot.impl.TriangleInequality;
import vm.metricSpace.distance.bounding.twopivots.AbstractTwoPivotsFilter;
import vm.metricSpace.distance.bounding.twopivots.impl.PtolemaicFilterForVoronoiPartitioning;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class VoronoiPartitioningWithFilterProcessor<T> extends AbstractPivotBasedPartitioningProcessor<T> {

    private final BoundsOnDistanceEstimation filter;
    private final int maxLBCount;
    protected final float[][] pivotPivotDists;

    public VoronoiPartitioningWithFilterProcessor(List batch, AbstractMetricSpace<T> metricSpace, DistanceFunctionInterface df, CountDownLatch latch, List<T> pivotData, float[] pivotLengths, Map<Comparable, Float> objectsLengths, BoundsOnDistanceEstimation filter, int numberOfPivotsUsedInFiltering) {
        this(batch, metricSpace, df, latch, pivotData, pivotLengths, objectsLengths, filter, numberOfPivotsUsedInFiltering, numberOfPivotsUsedInFiltering);
    }

    public VoronoiPartitioningWithFilterProcessor(List batch, AbstractMetricSpace<T> metricSpace, DistanceFunctionInterface df, CountDownLatch latch, List<T> pivotData, float[] pivotLengths, Map<Comparable, Float> objectsLengths, BoundsOnDistanceEstimation filter, int numberOfPivotsUsedInFiltering, int maxLBCount) {
        super(batch, metricSpace, df, latch, pivotData, numberOfPivotsUsedInFiltering, pivotLengths, objectsLengths);
        this.filter = filter;
        this.maxLBCount = maxLBCount;
        if (filter != null) {
            pivotPivotDists = metricSpace.getDistanceMap(df, pivotData, pivotData, numberOfPivotsUsedInFiltering, pivotData.size());
        } else {
            pivotPivotDists = null;
        }
    }

    @Override
    protected float getDistIfSmallerThan(float radius, T oData, T pData, Float oLength, Float pLength, float[] opDists, int pCounter) {
        boolean canBeFilteredOut = false;
        if (filter != null) {
            if (filter instanceof TriangleInequality) {
                canBeFilteredOut = evaluateOnePivotFilter(pCounter, opDists, radius);
            } else if (filter instanceof PtolemaicFilterForVoronoiPartitioning) {
                canBeFilteredOut = evaluatePtolemaicFilter(pCounter, opDists, radius);
            } else if (filter instanceof AbstractTwoPivotsFilter) {
                canBeFilteredOut = evaluateTwoPivotFilter(pCounter, opDists, radius);
            }
        }
        if (canBeFilteredOut) {
            return -1000;
        }
        dcOfPartitioningBatch++;
        return df.getDistance(oData, pData, oLength, pLength);
    }

    // One pivot filter
    // notation with respect to search:
    // here the o is q in the search
    // here the p0 is p in the search
    // here the p[pCount] is o in the search
    // here d(o, p0)        is d(q, p)   
    // here d(p0, p[pCount]) is d(p, o)   
    // here d(o, p[pCount]) is d(q, o)   
    private boolean evaluateOnePivotFilter(int pCounter, float[] opDists, float radius) {
        TriangleInequality filterCast = (TriangleInequality) filter;
        for (int p0 = 0; p0 < opDists.length; p0++) {
            float distQP = opDists[p0];
            float distPO = pivotPivotDists[p0][pCounter];
            float lb = filterCast.lowerBound(distQP, distPO, p0);
            lbCheckedBatch++;
            if (lb > radius) {
                return true;
            }
        }
        return false;
    }

    // notation with respect to search:
    // here the o is q in the search
    // here the p0 is p1 in the search
    // here the p1 is p2 in the search
    // here the p[pCount] is o in the search
    // here d(o, p0)        is d(q, p1)   
    // here d(o, p1)        is d(q, p2)   
    // here d(p0, p[pCount]) is d(p1, o)   
    // here d(p1, p[pCount]) is d(p2, o)   
    // here d(p0, p1) is d(p1, p2)   
    // here d(o, p[pCount]) is d(q, o)   
    private Random r = new Random();

    private boolean evaluateTwoPivotFilter(int pCounter, float[] opDists, float radius) {
        int limit = 2 * maxLBCount;
        for (int pIdx = 0; pIdx < limit; pIdx += 2) {
            int p0Idx = r.nextInt(opDists.length);
            int p1Idx = r.nextInt(opDists.length);

            float distP1Q = opDists[p0Idx];
            float distP1O = pivotPivotDists[p0Idx][pCounter];
            float distP2Q = opDists[p1Idx];
            float distP1P2 = pivotPivotDists[p0Idx][p1Idx];
            float distP2O = pivotPivotDists[p1Idx][pCounter];
            //distP1P2, distP2O, distP1Q, distP1O, distP2Q,
            //int p1Idx, int p2Idx, Float range
            float lb = filter.lowerBound(distP1P2, distP2O, distP1Q, distP1O, distP2Q, -1, -1, radius);
            lbCheckedBatch++;
            if (lb > radius) {
                return true;
            }
        }
        return false;
    }

    // notation with respect to search:
    // here the o is q in the search
    // here the p0 is p1 in the search
    // here the p1 is p2 in the search
    // here the p[pCount] is o in the search
    // here d(o, p0)        is d(q, p1)   
    // here d(o, p1)        is d(q, p2)   
    // here d(p0, p[pCount]) is d(p1, o)   
    // here d(p1, p[pCount]) is d(p2, o)   
    // here d(p0, p1) is d(p1, p2)   
    // here d(o, p[pCount]) is d(q, o)   
    private boolean evaluatePtolemaicFilter(int pCounter, float[] opDists, float radius) {
        PtolemaicFilterForVoronoiPartitioning filterCast = (PtolemaicFilterForVoronoiPartitioning) filter;
        int[] pivotPermutation = filterCast.pivotsOrderForLB(pCounter);
        for (int pIdx = 0; pIdx < pivotPermutation.length; pIdx += 2) {
            int p0Idx = pivotPermutation[pIdx];
            int p1Idx = pivotPermutation[pIdx + 1];
            float distP1Q = opDists[p0Idx];
            float distP2Q = opDists[p1Idx];
            float lb = filterCast.lowerBound(distP1Q, distP2Q, p0Idx, p1Idx, pCounter);
            lbCheckedBatch++;
            if (lb > radius) {
                return true;
            }
        }
        return false;
    }

}
