package vm.metricSpace.distance.bounding.twopivots.impl;

import vm.metricSpace.distance.bounding.twopivots.AbstractPtolemaicBasedFiltering;

/**
 *
 * @author xmic
 */
public class DataDependentPtolemaicFiltering extends AbstractPtolemaicBasedFiltering {

    public static final Integer CONSTANT_FOR_PRECISION = 1024 * 8;
    protected float[][][] coefsPivotPivot;
    private final boolean queryDynamicPivotPairs;

    public DataDependentPtolemaicFiltering(String namePrefix, float[][][] coefsPivotPivot) {
        this(namePrefix, coefsPivotPivot, true);
    }

    public DataDependentPtolemaicFiltering(String namePrefix, float[][][] coefsPivotPivot, boolean queryDynamicPivotPairs) {
        super(namePrefix);
        this.coefsPivotPivot = coefsPivotPivot;
        this.queryDynamicPivotPairs = queryDynamicPivotPairs;
    }

    @Override
    public float lowerBound(float distP2O, float distP1QMultipliedByCoef, float distP1O, float distP2QMultipliedByCoef) {
        return Math.abs(distP1O * distP2QMultipliedByCoef - distP2O * distP1QMultipliedByCoef);
    }

    @Override
    public float upperBound(float distP2O, float distP1QMultipliedByCoef, float distP1O, float distP2QMultipliedByCoef) {
        return (distP1O * distP2QMultipliedByCoef + distP2O * distP1QMultipliedByCoef) / CONSTANT_FOR_PRECISION;
    }

    @Override
    protected String getTechName() {
        return "data-dependent_ptolemaic_filtering";
    }

    @Override
    public float getCoefPivotPivotForLB(int p1Idx, int p2Idx) {
        return coefsPivotPivot[p1Idx][p2Idx][2];
    }

    @Override
    public float getCoefPivotPivotForUB(int p1Idx, int p2Idx) {
        return coefsPivotPivot[p1Idx][p2Idx][1];
    }

    public boolean isQueryDynamicPivotPairs() {
        return queryDynamicPivotPairs;
    }

}
