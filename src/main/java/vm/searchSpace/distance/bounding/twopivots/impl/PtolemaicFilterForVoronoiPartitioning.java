package vm.searchSpace.distance.bounding.twopivots.impl;

/**
 *
 * @author Vlada
 */
public interface PtolemaicFilterForVoronoiPartitioning {

    public float lowerBound(float distOPi, float distOPj, int pIdx, int pCur);

    public float upperBound(float distOPi, float distOPj, int pIdx, int pCur);

    public boolean isQueryDynamicPivotPairs();

    public int[] pivotsOrderForLB(int pCur);
}
