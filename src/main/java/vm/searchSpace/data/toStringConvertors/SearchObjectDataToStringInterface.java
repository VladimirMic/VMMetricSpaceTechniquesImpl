package vm.searchSpace.data.toStringConvertors;

/**
 *
 * @author Vlada
 * @param <T>
 */
public interface SearchObjectDataToStringInterface<T> {

    public T parseString(String dbString);

    public String searchObjectDataToString(T searchObjectData);
}
