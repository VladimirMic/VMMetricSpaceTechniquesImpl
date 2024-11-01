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
public class PtolemaicFilteringForStreamKNNClassifier<T> extends PtolemaicFiltering<T> implements PtolemaicFilterForVoronoiPartitioning {

    private final float[][][] dPCurrPiOverdPiPj;
    private final int pivotCount;

    public PtolemaicFilteringForStreamKNNClassifier(String resultNamePrefix, List<T> pivotsData, List<T> centroidsData, DistanceFunctionInterface<T> df, boolean queryDynamicPivotPairs) {
        super(resultNamePrefix, pivotsData, df, queryDynamicPivotPairs);
        pivotCount = pivotsData.size();
        dPCurrPiOverdPiPj = new float[centroidsData.size()][pivotCount][pivotCount];
        for (int cIdx = 0; cIdx < centroidsData.size(); cIdx++) {
            T centroidData = centroidsData.get(cIdx);
            for (int i = 0; i < pivotsData.size(); i++) {
                T piData = pivotsData.get(i);
                float dPCurrPi = df.getDistance(centroidData, piData);
                if (dPCurrPi == 0) {
                    continue;
                }
                float[] row = coefsPivotPivot[i];
                for (int j = 0; j < pivotsData.size(); j++) {
                    float dPiPjInv = row[j];
                    dPCurrPiOverdPiPj[cIdx][i][j] = dPiPjInv * dPCurrPi;
                }
            }
        }
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
        return distOPi * dPCurrPiOverdPiPj[pCur][jIdx][iIdx] + distOPj * dPCurrPiOverdPiPj[pCur][iIdx][jIdx];
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
        if (isQueryDynamicPivotPairs()) {
            return KNNSearchWithPtolemaicFiltering.identifyExtremePivotPairs(dPCurrPiOverdPiPj[pCur], pivotCount);
        }
        return KNNSearchWithPtolemaicFiltering.identifyRandomPivotPairs(pivotCount);
    }
}
