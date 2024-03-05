package vm.metricSpace.data.toStringConvertors;

/**
 *
 * @author Vlada
 * @param <T>
 */
public interface MetricObjectDataToStringInterface<T> {

    public T parseString(String dbString);

    public String metricObjectDataToString(T metricObjectData);
}
