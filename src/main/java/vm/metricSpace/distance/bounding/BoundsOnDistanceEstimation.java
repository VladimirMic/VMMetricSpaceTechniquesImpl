package vm.metricSpace.distance.bounding;

/**
 *
 * @author Vlada
 */
public abstract class BoundsOnDistanceEstimation {

    private final String namePrefix;

    public BoundsOnDistanceEstimation(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    public abstract float lowerBound(Object... args);

    public abstract float upperBound(Object... args);

    protected abstract String getTechName();

    public String getTechFullName() {
        return namePrefix + "_" + getTechName();
    }

}
