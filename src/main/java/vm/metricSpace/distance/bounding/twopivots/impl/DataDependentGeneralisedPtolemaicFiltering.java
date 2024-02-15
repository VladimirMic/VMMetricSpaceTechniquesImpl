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

//    @Override
//    public float lowerBound(float distP1P2, float distP2O, float distP1Q, float distP1O, float distP2Q, int p1Idx, int p2Idx, Float range) {
//        float ef = distP1O * distP2Q;
//        float bd = distP1Q * distP2O;
//        float coef = coefs[p1Idx][p2Idx][2];
//        return Math.abs(bd - ef) * coef;
////        if (lb2 > range) {
////            return lb2;
////        }
////        coef = coefs[p1Idx][p2Idx][0];
////        return (coef * (bd + ef)) / CONSTANT_FOR_PRECISION;
//    }
//
//    @Override
//    public float upperBound(float distP1P2, float distP2O, float distP1Q, float distP1O, float distP2Q, int p1Idx, int p2Idx, Float range) {
//        float coef = coefs[p1Idx][p2Idx][1];
//        float ef = distP1O * distP2Q;
//        float bd = distP1Q * distP2O;
//        float fraction = bd + ef;
//        float ub1 = (coef * fraction) / CONSTANT_FOR_PRECISION;
//        if (ub1 >= range) {
//            return ub1;
//        }
//        coef = coefs[p1Idx][p2Idx][3];
//        fraction = Math.abs(bd - ef);
//        return coef * fraction;
//    }
    @Override
    protected String getTechName() {
        return "data-dependent_generalised_ptolemaic_filtering_pivot_selection";
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
