package vm.metricSpace.distance.bounding.twopivots.impl;

import java.util.List;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.metricSpace.distance.bounding.twopivots.AbstractPtolemaicBasedFiltering;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class PtolemaicFiltering<T> extends AbstractPtolemaicBasedFiltering {

    private final float[][] coefsPivotPivot;
    private final boolean queryDynamicPivotPairs;

    public PtolemaicFiltering(String resultNamePrefix, List<T> pivotsData, DistanceFunctionInterface<T> df, boolean queryDynamicPivotPairs) {
        super(resultNamePrefix);
        coefsPivotPivot = new float[pivotsData.size()][pivotsData.size()];
        for (int i = 0; i < pivotsData.size() - 1; i++) {
            T p1 = pivotsData.get(i);
            for (int j = i + 1; j < pivotsData.size(); j++) {
                T p2 = pivotsData.get(j);
                float coef = 1 / df.getDistance(p1, p2);
                coefsPivotPivot[i][j] = coef;
                coefsPivotPivot[j][i] = coef;
            }
        }
        this.queryDynamicPivotPairs = queryDynamicPivotPairs;
    }

    @Override
    public float lowerBound(float distP2O, float distP1QMultipliedByCoef, float distP1O, float distP2QMultipliedByCoef) {
        return Math.abs(distP1O * distP2QMultipliedByCoef - distP2O * distP1QMultipliedByCoef);
    }

    @Override
    public float upperBound(float distP2O, float distP1QMultipliedByCoef, float distP1O, float distP2QMultipliedByCoef) {
        return distP1O * distP2QMultipliedByCoef + distP2O * distP1QMultipliedByCoef;
    }

    @Override
    public String getTechName() {
        return "ptolemaios";
    }

    @Override
    public float getCoefPivotPivotForLB(int p1Idx, int p2Idx) {
        return coefsPivotPivot[p1Idx][p2Idx];
    }

    @Override
    public float getCoefPivotPivotForUB(int p1Idx, int p2Idx) {
        return coefsPivotPivot[p1Idx][p2Idx];
    }

    public boolean getQueryDynamicPivotPairs() {
        return queryDynamicPivotPairs;
    }

}
