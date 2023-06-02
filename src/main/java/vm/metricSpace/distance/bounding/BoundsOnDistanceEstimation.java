package vm.metricSpace.distance.bounding;

/**
 *
 * @author Vlada
 */
public abstract class BoundsOnDistanceEstimation {

    private final String namePrefix;

    public BoundsOnDistanceEstimation(String namePrefix) {
        this.namePrefix = namePrefix == null ? "" : namePrefix;
    }

    public abstract float lowerBound(Object... args);

    public abstract float upperBound(Object... args);

    protected abstract String getTechName();

    public String getTechFullName() {
        String ret = namePrefix;
        if (!ret.isEmpty()) {
            ret += "_";
        }
        ret += getTechName();
        return ret;
    }

}
