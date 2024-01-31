package vm.metricSpace.distance.bounding.onepivot;

import vm.metricSpace.distance.bounding.BoundsOnDistanceEstimation;

/**
 *
 * @author Vlada
 */
public abstract class AbstractOnePivotFilter extends BoundsOnDistanceEstimation {

    public AbstractOnePivotFilter(String resultNamePreffixNumberOfPivotsPlusPivots) {
        super(resultNamePreffixNumberOfPivotsPlusPivots);
    }

    public abstract float lowerBound(float distQP, float distOP, int pivotIdx);

    public abstract float upperBound(float distQP, float distOP, int pivotIdx);

    @Override
    public float lowerBound(Object... args) {
        return lowerBound(Float.parseFloat(args[0].toString()), Float.parseFloat(args[1].toString()), (int) args[2]);
    }

    @Override
    public float upperBound(Object... args) {
        return upperBound(Float.parseFloat(args[0].toString()), Float.parseFloat(args[1].toString()), (int) args[2]);
    }

}
