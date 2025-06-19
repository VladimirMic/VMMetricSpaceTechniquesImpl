package vm.searchSpace.distance.bounding.twopivots;

import vm.searchSpace.distance.bounding.BoundsOnDistanceEstimation;

/**
 *
 * @author Vlada
 */
public abstract class AbstractTwoPivotsFilter extends BoundsOnDistanceEstimation {

    public AbstractTwoPivotsFilter(String resultNamePrefix) {
        super(resultNamePrefix);
    }

    public abstract float lowerBound(float distP1P2, float distP2O, float distP1Q, float distP1O, float distP2Q, int p1Idx, int p2Idx, Float range);

    public abstract float upperBound(float distP1P2, float distP2O, float distP1Q, float distP1O, float distP2Q, int p1Idx, int p2Idx, Float range);

    @Override
    public float lowerBound(Object... args) {
        return lowerBound((float) args[0], (float) args[1], (float) args[2], (float)args[3], (float)args[4], (int) args[5], (int) args[6], (Float) args[7]);
    }

    @Override
    public float upperBound(Object... args) {
        return upperBound((float) args[0], (float) args[1], (float) args[2], (float)args[3], (float)args[4], (int) args[5], (int) args[6], (Float) args[7]);
    }

}
