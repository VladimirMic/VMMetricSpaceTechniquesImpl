package vm.metricSpace.dataToStringConvertors;

/**
 *
 * @author Vlada
 * @param <T>
 */
public interface MetricObjectDataToStringInterface<T> {

    public T parseString(String dbString);

    public String metricObjectDataToString(T metricObjectData);
}
