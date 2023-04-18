package vm.metricSpace.distance;

/**
 *
 * @author Vlada
 * @param <T>
 */
public interface DistanceFunctionInterface<T> {

    /**
     *
     * @param obj1 data of metric objects (without ID) to be used to compare the
     * distance. E.g. float[] or others. See implementations for details.
     * @param obj2 data of metric objects (without ID) to be used to compare the
     * distance. E.g. float[] or others. See implementations for details.
     * @return
     */
    public float getDistance(T obj1, T obj2);

}
