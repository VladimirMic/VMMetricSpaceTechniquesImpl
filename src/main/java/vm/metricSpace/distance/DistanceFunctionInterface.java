package vm.metricSpace.distance;

/**
 *
 * @author Vlada
 * @param <T>
 */
public abstract class DistanceFunctionInterface<T> {

    /**
     *
     * @param obj1 data of metric objects (without ID) to be used to compare the
     * distance. E.g. float[] or others. See implementations for details.
     * @param obj2 data of metric objects (without ID) to be used to compare the
     * distance. E.g. float[] or others. See implementations for details.
     * @return
     */
    public abstract float getDistance(T obj1, T obj2);

    public float getDistance(T obj1, T obj2, Object... additionalParams) {
        return getDistance(obj1, obj2);
    }

}
