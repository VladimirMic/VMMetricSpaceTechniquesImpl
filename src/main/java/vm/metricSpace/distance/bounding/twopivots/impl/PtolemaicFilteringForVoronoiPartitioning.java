/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.metricSpace.distance.bounding.twopivots.impl;

import java.util.List;
import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class PtolemaicFilteringForVoronoiPartitioning<T> extends PtolemaicFiltering<T> {

    private final float[][][] dPCurrPiOverdPiPj;

    public PtolemaicFilteringForVoronoiPartitioning(String resultNamePrefix, List<T> pivotsData, DistanceFunctionInterface<T> df, boolean queryDynamicPivotPairs) {
        super(resultNamePrefix, pivotsData, df, queryDynamicPivotPairs);
        dPCurrPiOverdPiPj = new float[pivotsData.size()][pivotsData.size()][pivotsData.size()];
        for (int pCurr = 0; pCurr < pivotsData.size(); pCurr++) {
            T pCurrData = pivotsData.get(pCurr);
            for (int i = 0; i < pivotsData.size(); i++) {
                float[] row = coefsPivotPivot[i];
                T piData = pivotsData.get(i);
                for (int j = 0; j < pivotsData.size(); j++) {
                    float dPCurrPi = df.getDistance(pCurrData, piData);
                    float dPiPjInv = row[j];
                    dPCurrPiOverdPiPj[pCurr][i][j] = dPCurrPi * dPiPjInv;
                }
            }
        }
    }

    @Override
    public String getTechName() {
        return "ptolemaios_for_voronoi_partitioning";
    }

    @Override
    public float lowerBound(Object... args) {
        return lowerBound((float) args[0], (float) args[1], (int) args[2], (int) args[3], (int) args[4]);
    }

    @Override
    public float upperBound(Object... args) {
        return upperBound((float) args[0], (float) args[1], (int) args[2], (int) args[3], (int) args[4]);
    }

    public float lowerBound(float distOPi, float distOPj, int iIdx, int jIdx, int pCur) {
        return Math.abs(distOPi * dPCurrPiOverdPiPj[pCur][jIdx][iIdx] - distOPj * dPCurrPiOverdPiPj[pCur][iIdx][jIdx]);
    }

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
}
