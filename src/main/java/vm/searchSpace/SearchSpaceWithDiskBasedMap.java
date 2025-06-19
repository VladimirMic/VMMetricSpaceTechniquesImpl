package vm.searchSpace;

import java.util.Map;
import vm.searchSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class SearchSpaceWithDiskBasedMap<T> extends AbstractSearchSpace<T> {

    private final AbstractSearchSpace<T> origSearchSpace;
    private final Map<Object, T> diskBasedStorage;

    public SearchSpaceWithDiskBasedMap(AbstractSearchSpace<T> origSearchSpace, Map<Object, T> diskBasedStorage) {
        super(origSearchSpace.getDistanceFunction());
        this.origSearchSpace = origSearchSpace;
        this.diskBasedStorage = diskBasedStorage;
    }

    @Override
    public DistanceFunctionInterface<T> getDistanceFunction() {
        return origSearchSpace.getDistanceFunction();
    }

    @Override
    public Comparable getIDOfObject(Object o) {
        try {
            return origSearchSpace.getIDOfObject(o); // because of pivots
        } catch (Exception e) {
        }
        return (Comparable) o;
    }

    @Override
    public T getDataOfObject(Object o) {
        try {
            return origSearchSpace.getDataOfObject(o);
        } catch (Exception e) {
        }
        return diskBasedStorage.get(o);
    }

    @Override
    public Object createSearchObject(Comparable id, T data) {
        return origSearchSpace.createSearchObject(id, data);
    }

}
