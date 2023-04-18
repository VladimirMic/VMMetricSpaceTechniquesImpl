package vm.metricSpace.distance.bounding.onepivot;

import vm.metricSpace.distance.bounding.BoundsOnDistanceEstimation;

/**
 *
 * @author Vlada
 */
public abstract class OnePivotFiltering extends BoundsOnDistanceEstimation {

    public OnePivotFiltering(String resultNamePreffixNumberOfPivotsPlusPivots) {
        super(resultNamePreffixNumberOfPivotsPlusPivots);
    }

    public abstract float lowerBound(float distQP, float distOP, String pivotID);

    public abstract float upperBound(float distQP, float distOP, String pivotID);

    @Override
    public float lowerBound(Object... args) {
        return lowerBound(Float.parseFloat(args[0].toString()), Float.parseFloat(args[1].toString()), args[2].toString());
    }

    @Override
    public float upperBound(Object... args) {
        return upperBound(Float.parseFloat(args[0].toString()), Float.parseFloat(args[1].toString()), args[2].toString());
    }

}
