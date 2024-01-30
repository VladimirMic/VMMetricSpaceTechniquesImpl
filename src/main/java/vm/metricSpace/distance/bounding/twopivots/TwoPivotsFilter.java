package vm.metricSpace.distance.bounding.twopivots;

import vm.metricSpace.distance.bounding.BoundsOnDistanceEstimation;

/**
 *
 * @author Vlada
 */
public abstract class TwoPivotsFilter extends BoundsOnDistanceEstimation {

    public TwoPivotsFilter(String resultNamePrefix) {
        super(resultNamePrefix);
    }

    public abstract boolean isPivotPairValid(int p1Idx, int p2Idx);

    public abstract float lowerBound(float distP1P2, float distP2O, float distP1Q, float distP1O, float distP2Q, int p1Idx, int p2Idx, Float range);

    public abstract float upperBound(float distP1P2, float distP2O, float distP1Q, float distP1O, float distP2Q, int p1Idx, int p2Idx, Float range);

    @Override
    public float lowerBound(Object... args) {
        return lowerBound(Float.parseFloat(args[0].toString()), Float.parseFloat(args[1].toString()), Float.parseFloat(args[2].toString()), Float.parseFloat(args[3].toString()), Float.parseFloat(args[4].toString()), (int) args[5], (int) args[6], (Float) args[6]);
    }

    @Override
    public float upperBound(Object... args) {
        return upperBound(Float.parseFloat(args[0].toString()), Float.parseFloat(args[1].toString()), Float.parseFloat(args[2].toString()), Float.parseFloat(args[3].toString()), Float.parseFloat(args[4].toString()), (int) args[5], (int) args[6], (Float) args[6]);
    }

}
