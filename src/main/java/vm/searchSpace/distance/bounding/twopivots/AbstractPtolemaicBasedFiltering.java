package vm.searchSpace.distance.bounding.twopivots;

import vm.searchSpace.distance.bounding.BoundsOnDistanceEstimation;

/**
 *
 * @author Vlada
 */
public abstract class AbstractPtolemaicBasedFiltering extends BoundsOnDistanceEstimation {

    public AbstractPtolemaicBasedFiltering(String resultNamePrefix) {
        super(resultNamePrefix);
    }

    public abstract float lowerBound(float distP2O, float distP1QMultipliedByCoef, float distP1O, float distP2QMultipliedByCoef);

    public abstract float upperBound(float distP2O, float distP1QMultipliedByCoef, float distP1O, float distP2QMultipliedByCoef);

    public abstract float getCoefPivotPivotForLB(int p1Idx, int p2Idx);

    public abstract float getCoefPivotPivotForUB(int p1Idx, int p2Idx);

    @Override
    public float lowerBound(Object... args) {
        return lowerBound((float) args[0], (float) args[1], (float) args[2], (float) args[3]);
    }

    @Override
    public float upperBound(Object... args) {
        return upperBound((float) args[0], (float) args[1], (float) args[2], (float) args[3]);
    }

}
