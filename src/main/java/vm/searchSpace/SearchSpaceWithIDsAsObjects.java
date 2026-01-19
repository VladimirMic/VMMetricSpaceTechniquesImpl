package vm.searchSpace;

import vm.searchSpace.distance.AbstractDistanceFunction;

/**
 *
 * @author xmic
 * @param <T>
 */
public class SearchSpaceWithIDsAsObjects<T> extends AbstractSearchSpace<T> {

    public SearchSpaceWithIDsAsObjects(AbstractDistanceFunction<T> df) {
        super(df);
    }

    @Override
    public Comparable getIDOfObject(Object o) {
        return (Comparable) o;
    }

    @Override
    public T getDataOfObject(Object o) {
        return (T) o;
    }

    @Override
    public Object createSearchObject(Comparable id, T data) {
        return id;
    }

}
