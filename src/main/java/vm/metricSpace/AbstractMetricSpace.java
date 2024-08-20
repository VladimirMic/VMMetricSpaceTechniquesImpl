package vm.metricSpace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author Vlada
 * @param <T>
 */
public abstract class AbstractMetricSpace<T> {

    /**
     *
     * @param datasetName voluntary, if it helps you to distinguish your
     * datasets
     * @param params volutary, again, if you want to use them
     * @return instance of the distance functon for this dataset
     */
    public abstract DistanceFunctionInterface<T> getDistanceFunctionForDataset(String datasetName, Object... params);

    /**
     * Get id associated with this metric object o
     *
     * @param o
     * @return id of o
     */
    public abstract Comparable getIDOfMetricObject(Object o);

    /**
     * Get actual data of the metric object (i.e. usually the metric object
     * without the id). For example, the coordinates of the vector, if o is a
     * vector. Use float[] to store coordinates of real-valued vector spaces.
     * See method @getMetricObjectDataAsFloatVector
     *
     * @param o metric object metric object o representation
     * @return
     */
    public abstract T getDataOfMetricObject(Object o);

    public abstract Object createMetricObject(Comparable id, T data);

    public List<Comparable> getIDsOfMetricObjects(Iterator<Object> metricObjects) {
        return ToolsMetricDomain.getIDsAsList(metricObjects, this);
    }

    public List<T> getDataOfMetricObjects(Collection<Object> metricObjects) {
        if (metricObjects == null) {
            return null;
        }
        return ToolsMetricDomain.getDataAsList(metricObjects.iterator(), this);
    }

    public float[][] getDistanceMap(DistanceFunctionInterface<T> df, List<Object> list1, List<Object> list2) {
        float[][] ret = new float[list1.size()][list2.size()];
        for (int i = 0; i < list1.size(); i++) {
            Object o1 = list1.get(i);
            T o1Data = getDataOfMetricObject(o1);
            for (int j = 0; j < list2.size(); j++) {
                Object o2 = list2.get(j);
                T o2Data = getDataOfMetricObject(o2);
                float distance = df.getDistance(o1Data, o2Data);
                ret[i][j] = distance;
            }
        }
        return ret;
    }

}
