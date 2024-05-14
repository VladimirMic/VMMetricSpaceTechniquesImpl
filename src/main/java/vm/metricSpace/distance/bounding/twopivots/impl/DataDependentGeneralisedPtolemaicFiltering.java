package vm.metricSpace.distance.bounding.twopivots.impl;

import vm.metricSpace.distance.bounding.twopivots.AbstractPtolemaicBasedFiltering;

/**
 *
 * @author xmic
 */
public class DataDependentGeneralisedPtolemaicFiltering extends AbstractPtolemaicBasedFiltering {

    private final float[][][] coefsPivotPivot;
    public static final Integer CONSTANT_FOR_PRECISION = 1024 * 8;

    public DataDependentGeneralisedPtolemaicFiltering(String namePrefix, float[][][] coefsPivotPivot) {
        super(namePrefix);
        this.coefsPivotPivot = coefsPivotPivot;
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
        return "data-dependent_generalised_ptolemaic_filtering_pivot_array_selection";
    }

    @Override
    public float getCoefPivotPivotForLB(int p1Idx, int p2Idx) {
        return coefsPivotPivot[p1Idx][p2Idx][2];
    }

    @Override
    public float getCoefPivotPivotForUB(int p1Idx, int p2Idx) {
        return coefsPivotPivot[p1Idx][p2Idx][1];
    }

}
