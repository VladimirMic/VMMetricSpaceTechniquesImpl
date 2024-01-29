package vm.metricSpace.distance.bounding.twopivots.impl;

import vm.metricSpace.distance.bounding.twopivots.TwoPivotsFilter;
import static vm.search.algorithm.impl.KNNSearchWithTwoPivotFiltering.PRINT_DETAILS;

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
        float fraction = Math.abs(bd - ef);
        float lb2 = coef * fraction;
        if (lb2 > range) {
            return lb2;
        }
        coef = coefs[p1Idx][p2Idx][0];
        fraction = bd + ef;
        float lb1 = (coef * fraction) / CONSTANT_FOR_PRECISION;

//        if (PRINT_DETAILS) {
//            System.out.print("lb1;" + lb1 + ";lb2;" + lb2 + ";");
//            if (lb1 > lb2) {
//                System.out.println(1);
//            } else {
//                System.out.println(2);
//            }
//        }
        return lb1;
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
        float ub2 = coef * fraction;
//        if (PRINT_DETAILS) {
//            System.out.println(a + ";" + distP1O + ";" + distP2O + ";" + distP1Q + ";" + distP2Q + ";;;" + bd + ";" + ef + ";;;" + fraction + ";" + coef);
//            System.out.print("ub1;" + ub1 + ";ub2;" + ub2 + ";");
//            if (ub1 < ub2) {
//                System.out.println(1);
//            } else {
//                System.out.println(2);
//            }
//        }
        return ub2;
    }

    @Override
    protected String getTechName() {
        return "data-dependent_generalised_ptolemaic_filtering";
    }

    public float getCoef(int p1Idx, int p2Idx, int coefIdx) {
        return coefs[p1Idx][p2Idx][coefIdx];
    }

}
