package vm.metricSpace.distance.bounding.twopivots.impl;

import vm.metricSpace.distance.bounding.twopivots.TwoPivotsFilter;

/**
 *
 * @author xmic
 */
public class DataDependentGeneralisedPtolemaicFiltering extends TwoPivotsFilter {

    private final float[][][] coefs;
    public static final Integer CONSTANT_FOR_PRECISION = 1024 * 8;
    public static final Float RATIO_OF_IGNORED_SMALLEST = 0.0f / 100f; // percentile defining the minimum and the maximum. I.e., 2 times this is ignored.

    public DataDependentGeneralisedPtolemaicFiltering(String namePrefix, float[][][] coefs) {
        super(namePrefix);
        this.coefs = coefs;
    }

    @Override
    public float lowerBound(float distP1P2, float distP2O, float distP1Q, float distP1O, float distP2Q, int p1Idx, int p2Idx, Float range) {
        float ef = distP1O * distP2Q;
        float bd = distP1Q * distP2O;
        float coef = coefs[p1Idx][p2Idx][2];
        return Math.abs(bd - ef) * coef;
//        if (lb2 > range) {
//            return lb2;
//        }
//        coef = coefs[p1Idx][p2Idx][0];
//        return (coef * (bd + ef)) / CONSTANT_FOR_PRECISION;
    }

    @Override
    public float upperBound(float distP1P2, float distP2O, float distP1Q, float distP1O, float distP2Q, int p1Idx, int p2Idx, Float range) {
        float coef = coefs[p1Idx][p2Idx][1];
        float ef = distP1O * distP2Q;
        float bd = distP1Q * distP2O;
        float fraction = bd + ef;
        float ub1 = (coef * fraction) / CONSTANT_FOR_PRECISION;
        if (ub1 >= range) {
            return ub1;
        }
        coef = coefs[p1Idx][p2Idx][3];
        fraction = Math.abs(bd - ef);
        return coef * fraction;
    }

    @Override
    protected String getTechName() {
        return "data-dependent_generalised_ptolemaic_filtering";
    }

    public float getCoef(int p1Idx, int p2Idx, int coefIdx) {
        return coefs[p1Idx][p2Idx][coefIdx];
    }

    @Override
    public boolean isPivotPairValid(int p1Idx, int p2Idx) {
        return coefs[p1Idx][p2Idx][2] != 0;
    }

}
