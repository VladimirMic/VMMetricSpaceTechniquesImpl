package vm.searchSpace.distance.bounding.twopivots.impl;

import java.util.List;
import vm.search.algorithm.impl.KNNSearchWithPtolemaicFiltering;
import vm.searchSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class PtolemaicFilteringForStreamKNNClassifier<T> extends PtolemaicFiltering<T> implements PtolemaicFilterForVoronoiPartitioning {

    private final int[][] pivotPairsForCentroids;
    private final float[][] coefsForLB;
    private final int pivotCount;

    public PtolemaicFilteringForStreamKNNClassifier(String resultNamePrefix, List<T> pivotsData, List<T> centroidsData, DistanceFunctionInterface<T> df, boolean queryDynamicPivotPairs) {
        super(resultNamePrefix, pivotsData, df, queryDynamicPivotPairs);
        pivotCount = pivotsData.size();
        int centroidCount = centroidsData.size();
        float[][][] dPCurrPiOverdPiPj = new float[centroidCount][pivotCount][pivotCount];
        for (int cIdx = 0; cIdx < centroidCount; cIdx++) {
            T centroidData = centroidsData.get(cIdx);
            for (int i = 0; i < pivotCount; i++) {
                T piData = pivotsData.get(i);
                float dPCurrPi = df.getDistance(centroidData, piData);
                if (dPCurrPi == 0) {
                    continue;
                }
                float[] row = coefsPivotPivot[i];
                for (int j = 0; j < pivotCount; j++) {
                    float dPiPjInv = row[j];
                    dPCurrPiOverdPiPj[cIdx][i][j] = dPiPjInv * dPCurrPi;
                }
            }
        }
        boolean randomPivot = !isQueryDynamicPivotPairs();
        coefsForLB = new float[centroidCount][pivotCount * 2];
        pivotPairsForCentroids = new int[centroidCount][pivotCount * 2];
        transformToCentroidPermutationAndLBCoefsArrays(dPCurrPiOverdPiPj, pivotPairsForCentroids, coefsForLB, randomPivot);
    }

    @Override
    public String getTechName() {
        String ret = "ptolemaios_for_voronoi_partitioning";
        if (!isQueryDynamicPivotPairs()) {
            ret += "_random_pivots";
        }
        return ret;
    }

    @Override
    public float lowerBound(Object... args) {
        return lowerBound((float) args[0], (float) args[1], (int) args[2], (int) args[3]);
    }

    @Override
    public float upperBound(Object... args) {
        return upperBound((float) args[0], (float) args[1], (int) args[2], (int) args[3]);
    }

    @Override
    public float lowerBound(float distOPi, float distOPj, int pIdx, int pCur) {
        return Math.abs(distOPi * coefsForLB[pCur][pIdx + 1] - distOPj * coefsForLB[pCur][pIdx]);
    }

    @Override
    public float upperBound(float distOPi, float distOPj, int pIdx, int pCur) {
        throw new UnsupportedOperationException();
    }

    @Override
    public float lowerBound(float distP2O, float distP1QMultipliedByCoef, float distP1O, float distP2QMultipliedByCoef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public float upperBound(float distP2O, float distP1QMultipliedByCoef, float distP1O, float distP2QMultipliedByCoef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int[] pivotsOrderForLB(int pCur) {
        return pivotPairsForCentroids[pCur];
    }

    protected static final void transformToCentroidPermutationAndLBCoefsArrays(float[][][] dPCurrPiOverdPiPj, int[][] pivotPairsForCentroids, float[][] coefsForLB, boolean randomPivotOrder) {
        int pivotCount = dPCurrPiOverdPiPj[0].length;
        for (int cIdx = 0; cIdx < pivotPairsForCentroids.length; cIdx++) {
            if (randomPivotOrder) {
                pivotPairsForCentroids[cIdx] = KNNSearchWithPtolemaicFiltering.identifyRandomPivotPairs(pivotCount);
            } else {
                pivotPairsForCentroids[cIdx] = KNNSearchWithPtolemaicFiltering.identifyExtremePivotPairs(dPCurrPiOverdPiPj[cIdx], pivotCount);
            }
        }
        for (int cIdx = 0; cIdx < pivotPairsForCentroids.length; cIdx++) {
            int[] pivotPairsForC = pivotPairsForCentroids[cIdx];
            float[] coefsForC = coefsForLB[cIdx];
            float[][] extremeValuesForC = dPCurrPiOverdPiPj[cIdx];
            for (int pIdx = 0; pIdx < pivotCount; pIdx++) {
                int p0Idx = 2 * pIdx;
                int p1Idx = p0Idx + 1;
                int i = pivotPairsForC[p0Idx];
                int j = pivotPairsForC[p1Idx];
                coefsForC[p0Idx] = extremeValuesForC[i][j];
                coefsForC[p1Idx] = extremeValuesForC[j][i];
            }
        }
    }
}
