package vm.searchSpace.datasetPartitioning.impl.batchProcessor;

import java.util.List;
import java.util.Random;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.distance.AbstractDistanceFunction;
import vm.searchSpace.distance.bounding.BoundsOnDistanceEstimation;
import vm.searchSpace.distance.bounding.onepivot.impl.TriangleInequality;
import vm.searchSpace.distance.bounding.twopivots.AbstractTwoPivotsFilter;
import vm.searchSpace.distance.bounding.twopivots.impl.PtolemaicFilterForVoronoiPartitioning;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class VoronoiPartitioningWithFilterProcessor<T> extends AbstractPivotBasedPartitioningProcessor<T> {

    private final BoundsOnDistanceEstimation filter;
    private final int maxLBCount;
    protected final float[][] pivotPivotDists;
    private final short filterType;

    public VoronoiPartitioningWithFilterProcessor(AbstractSearchSpace<T> searchSpace, AbstractDistanceFunction df, List<T> pivotData, float[][] pivotPivotDists, float[] pivotLengths, BoundsOnDistanceEstimation filter, int numberOfPivotsUsedInFiltering) {
        this(searchSpace, df, pivotData, pivotPivotDists, pivotLengths, filter, numberOfPivotsUsedInFiltering, numberOfPivotsUsedInFiltering);
    }

    public VoronoiPartitioningWithFilterProcessor(AbstractSearchSpace<T> searchSpace, AbstractDistanceFunction df, List<T> pivotData, float[][] pivotPivotDists, float[] pivotLengths, BoundsOnDistanceEstimation filter, int numberOfPivotsUsedInFiltering, int maxLBCount) {
        super(searchSpace, df, pivotData, numberOfPivotsUsedInFiltering, pivotLengths);
        this.filter = filter;
        this.maxLBCount = maxLBCount;
        this.pivotPivotDists = pivotPivotDists;
        if (filter == null) {
            filterType = 0;
        } else if (filter instanceof TriangleInequality) {
            filterType = 1;
        } else if (filter instanceof PtolemaicFilterForVoronoiPartitioning) {
            filterType = 2;
        } else if (filter instanceof AbstractTwoPivotsFilter) {
            filterType = 3;
        } else {
            throw new RuntimeException("What a filter??");
        }
    }

    public static long tCheck = 0;

    @Override
    protected float getDistIfSmallerThan(float radius, T oData, T pData, Float oLength, Float pLength, float[] opDists, int pCounter) {
        boolean canBeFilteredOut = false;
        switch (filterType) {
            case 1:
                canBeFilteredOut = evaluateOnePivotFilter(pCounter, opDists, radius);
                break;
            case 2:
                canBeFilteredOut = evaluatePtolemaicFilter(pCounter, opDists, radius);
                break;

            case 3:
                canBeFilteredOut = evaluateTwoPivotFilter(pCounter, opDists, radius);
                break;
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
            float lb = filterCast.lowerBound(distP1Q, distP2Q, pIdx, pCounter);
            lbCheckedBatch++;
            if (lb > radius) {
                return true;
            }
        }
        return false;
    }

    protected Object[] getAdditionalParamsForFilter() {
        return null;
    }

}
