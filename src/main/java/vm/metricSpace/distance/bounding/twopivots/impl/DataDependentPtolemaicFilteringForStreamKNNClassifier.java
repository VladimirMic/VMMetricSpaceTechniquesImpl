/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.metricSpace.distance.bounding.twopivots.impl;

import java.util.List;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.search.algorithm.impl.KNNSearchWithPtolemaicFiltering;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class DataDependentPtolemaicFilteringForStreamKNNClassifier<T> extends DataDependentPtolemaicFiltering implements PtolemaicFilterForVoronoiPartitioning {

    private final float[][][] dPCurrPiOverdPiPj;
    private final int pivotCount;

    public DataDependentPtolemaicFilteringForStreamKNNClassifier(String namePrefix, float[][][] coefsPivotPivot, List<T> pivotsData, DistanceFunctionInterface<T> df, boolean queryDynamicPivotPairs) {
        super(namePrefix, coefsPivotPivot, queryDynamicPivotPairs);
        pivotCount = pivotsData.size();
        dPCurrPiOverdPiPj = new float[pivotsData.size()][pivotCount][pivotCount];
        for (int pCurr = 0; pCurr < pivotsData.size(); pCurr++) {
            T pCurrData = pivotsData.get(pCurr);
            for (int i = 0; i < pivotsData.size(); i++) {
                T piData = pivotsData.get(i);
                float dPCurrPi = df.getDistance(pCurrData, piData);
                if (dPCurrPi == 0) {
                    continue;
                }
                for (int j = 0; j < pivotsData.size(); j++) {
                    float coefLB = getCoefPivotPivotForLB(i, j);
                    dPCurrPiOverdPiPj[pCurr][i][j] = coefLB * dPCurrPi;
                }
            }
        }
        super.coefsPivotPivot = null;
        System.gc();
    }

    @Override
    public float lowerBound(Object... args) {
        return lowerBound((float) args[0], (float) args[1], (int) args[2], (int) args[3], (int) args[4]);
    }

    @Override
    public float upperBound(Object... args) {
        return upperBound((float) args[0], (float) args[1], (int) args[2], (int) args[3], (int) args[4]);
    }

    @Override
    public float lowerBound(float distOPi, float distOPj, int iIdx, int jIdx, int pCur) {
        return Math.abs(distOPi * dPCurrPiOverdPiPj[pCur][jIdx][iIdx] - distOPj * dPCurrPiOverdPiPj[pCur][iIdx][jIdx]);
    }

    @Override
    public float upperBound(float distOPi, float distOPj, int iIdx, int jIdx, int pCur) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected String getTechName() {
        String ret = "data-dependent_ptolemaic_filtering";
        if (!isQueryDynamicPivotPairs()) {
            ret += "_random_pivots";
        }
        return ret;
    }

    @Override
    public int[] pivotsOrderForLB(int pCur) {
        if (isQueryDynamicPivotPairs()) {
            return KNNSearchWithPtolemaicFiltering.identifyExtremePivotPairs(dPCurrPiOverdPiPj[pCur], pivotCount);
        }
        return KNNSearchWithPtolemaicFiltering.identifyRandomPivotPairs(pivotCount);
    }

}
