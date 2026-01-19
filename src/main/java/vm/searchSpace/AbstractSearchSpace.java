package vm.searchSpace;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import vm.searchSpace.distance.AbstractDistanceFunction;

/**
 *
 * @author Vlada
 * @param <T>
 */
public abstract class AbstractSearchSpace<T> {

    private final AbstractDistanceFunction<T> df;

    public AbstractSearchSpace(AbstractDistanceFunction<T> df) {
        this.df = df;
    }

    public AbstractDistanceFunction<T> getDistanceFunction() {
        return df;
    }

    /**
     * Get id associated with this search object o
     *
     * @param o
     * @return id of o
     */
    public abstract Comparable getIDOfObject(Object o);

    /**
     * Get actual data of the search object (i.e. usually the search object
     * without the id). For example, the coordinates of the vector, if o is a
     * vector. Use float[] to store coordinates of real-valued vector spaces.
     * See method @getSearchObjectDataAsFloatVector
     *
     * @param o search object search object o representation
     * @return
     */
    public abstract T getDataOfObject(Object o);

    public abstract Object createSearchObject(Comparable id, T data);

    public List<Comparable> getIDsOfObjects(Iterator<Object> searchObjects) {
        return ToolsSpaceDomain.getIDsAsList(searchObjects, this);
    }

    public List<T> getDataOfObjects(Collection<Object> searchObjects) {
        if (searchObjects == null) {
            return null;
        }
        return ToolsSpaceDomain.getDataAsList(searchObjects.iterator(), this);
    }

    public float[][] getDistanceMap(AbstractDistanceFunction<T> df, List<Object> list1, List<Object> list2) {
        float[][] ret = new float[list1.size()][list2.size()];
        for (int i = 0; i < list1.size(); i++) {
            Object o1 = list1.get(i);
            T o1Data = getDataOfObject(o1);
            for (int j = 0; j < list2.size(); j++) {
                Object o2 = list2.get(j);
                T o2Data = getDataOfObject(o2);
                float distance = df.getDistance(o1Data, o2Data);
                ret[i][j] = distance;
            }
        }
        return ret;
    }

    public float[][] getDistanceMap(AbstractDistanceFunction<T> df, List<T> list1, List<T> list2, int list1ObjCount, int list2ObjCount) {
        float[][] ret = new float[list1ObjCount][list2ObjCount];
        for (int i = 0; i < list1ObjCount; i++) {
            T o1Data = list1.get(i);
            for (int j = 0; j < list2ObjCount; j++) {
                T o2Data = list2.get(j);
                float distance = df.getDistance(o1Data, o2Data);
                ret[i][j] = distance;
            }
        }
        return ret;
    }

}
