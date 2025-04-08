/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.metricSpace.distance.bounding.twopivots.impl;

import java.util.List;
import vm.metricSpace.distance.DistanceFunctionInterface;
import static vm.metricSpace.distance.bounding.twopivots.impl.PtolemaicFilteringForStreamKNNClassifier.transformToCentroidPermutationAndLBCoefsArrays;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class DataDependentPtolemaicFilteringForStreamKNNClassifier<T> extends DataDependentPtolemaicFiltering implements PtolemaicFilterForVoronoiPartitioning {

    private final int[][] pivotPairsForCentroids;
    private final float[][] coefsForLB;

    private final int pivotCount;

    public DataDependentPtolemaicFilteringForStreamKNNClassifier(String namePrefix, float[][][] coefsPivotPivot, List<T> centroidsData, DistanceFunctionInterface<T> df, boolean queryDynamicPivotPairs) {
        super(namePrefix, coefsPivotPivot, queryDynamicPivotPairs);
        pivotCount = coefsPivotPivot.length;
        int centroidCount = centroidsData.size();
        float[][][] dPCurrPiOverdPiPj = new float[centroidCount][pivotCount][pivotCount];
        for (int pCurr = 0; pCurr < centroidCount; pCurr++) {
            T pCurrData = centroidsData.get(pCurr);
            for (int i = 0; i < pivotCount; i++) {
                T piData = centroidsData.get(i);
                float dPCurrPi = df.getDistance(pCurrData, piData);
                if (dPCurrPi == 0) {
                    continue;
                }
                for (int j = 0; j < pivotCount; j++) {
                    float coefLB = getCoefPivotPivotForLB(i, j);
                    dPCurrPiOverdPiPj[pCurr][i][j] = coefLB * dPCurrPi;
                }
            }
        }
        super.coefsPivotPivot = null;
        System.gc();
        boolean randomPivot = !isQueryDynamicPivotPairs();
        coefsForLB = new float[centroidCount][pivotCount * 2];
        pivotPairsForCentroids = new int[centroidCount][pivotCount * 2];
        transformToCentroidPermutationAndLBCoefsArrays(dPCurrPiOverdPiPj, pivotPairsForCentroids, coefsForLB, randomPivot);
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
    public float upperBound(float distOPi, float distOPj, int iIdx, int pCur) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getTechName() {
        String ret = "data-dependent_ptolemaic_filtering";
        if (!isQueryDynamicPivotPairs()) {
            ret += "_random_pivots";
        }
        return ret;
    }

    @Override
    public int[] pivotsOrderForLB(int pCur) {
        return pivotPairsForCentroids[pCur];
    }
}
