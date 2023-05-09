package vm.metricSpace;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.math.Tools;
import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author Vlada
 */
public class ToolsMetricDomain {

    private static final Logger LOG = Logger.getLogger(ToolsMetricDomain.class.getName());

    public static float[][] transformMetricObjectsToVectorMatrix(AbstractMetricSpace<float[]> metricSpace, List<Object> metricObjects) {
        float[] first = metricSpace.getDataOfMetricObject(metricObjects.get(0));
        float[][] ret = new float[metricObjects.size()][first.length];
        for (int i = 0; i < metricObjects.size(); i++) {
            float[] vector = metricSpace.getDataOfMetricObject(metricObjects.get(i));
            ret[i] = vector;
        }
        return ret;
    }

    public static float[][] transformMetricObjectsToTransposedVectorMatrix(AbstractMetricSpace<float[]> metricSpace, List<Object> metricObjects) {
        float[] vec = metricSpace.getDataOfMetricObject(metricObjects.get(0));
        float[][] ret = new float[vec.length][metricObjects.size()];
        for (int objIdx = 0; objIdx < metricObjects.size(); objIdx++) {
            vec = metricSpace.getDataOfMetricObject(metricObjects.get(objIdx));
            for (int coordIdx = 0; coordIdx < vec.length; coordIdx++) {
                ret[coordIdx][objIdx] = vec[coordIdx];
            }
        }
        return ret;
    }

    /**
     *
     * @param metricSpace
     * @param metricObjectsSample
     * @param distanceFunction
     * @param distCount
     * @param basicInterval
     * @return
     */
    public static SortedMap<Float, Float> createDistanceDensityPlot(AbstractMetricSpace<float[]> metricSpace, List<Object> metricObjectsSample, DistanceFunctionInterface distanceFunction, int distCount, float basicInterval) {
        return createDistanceDensityPlot(metricSpace, metricObjectsSample, distanceFunction, distCount, basicInterval, null);
    }

    /**
     * Evaluates distCount distances of random pairs of objects from the list
     * metricObjectsSample
     *
     * @param metricSpace
     * @param metricObjectsSample
     * @param distanceFunction
     * @param distCount
     * @param basicInterval
     * @param pairsOfExaminedIDs if not null, adds all examined pairs of objects
     * @return
     */
    public static SortedMap<Float, Float> createDistanceDensityPlot(AbstractMetricSpace<float[]> metricSpace, List<Object> metricObjectsSample, DistanceFunctionInterface distanceFunction, int distCount, float basicInterval, List<Object[]> pairsOfExaminedIDs) {
        SortedMap<Float, Float> histogram = new TreeMap<>();
        Random r = new Random();
        for (int i = 0; i < distCount; i++) {
            Object o1 = metricObjectsSample.get(r.nextInt(metricObjectsSample.size()));
            Object o2 = metricObjectsSample.get(r.nextInt(metricObjectsSample.size()));
            Object id1 = metricSpace.getIDOfMetricObject(o1);
            Object id2 = metricSpace.getIDOfMetricObject(o2);
            if (pairsOfExaminedIDs != null) {
                pairsOfExaminedIDs.add(new Object[]{id1, id2});
            }
            o1 = metricSpace.getDataOfMetricObject(o1);
            o2 = metricSpace.getDataOfMetricObject(o2);
            float distance = distanceFunction.getDistance(o1, o2);
            distance = Tools.round(distance, basicInterval, true);
            if (!histogram.containsKey(distance)) {
                histogram.put(distance, 1f);
            } else {
                Float count = histogram.get(distance);
                histogram.put(distance, count + 1);
            }
        }
        Float lastKey = histogram.lastKey();
        while (lastKey >= 0) {
            if (!histogram.containsKey(lastKey)) {
                histogram.put(lastKey, 0f);
            } else {
                histogram.put(lastKey, histogram.get(lastKey) / distCount);
            }
            lastKey -= basicInterval;
        }
        return histogram;
    }

    public static SortedMap<Float, Float> createDistanceDensityPlot(Collection<Float> distances, float basicInterval) {
        SortedMap<Float, Float> histogram = new TreeMap<>();
        for (float distance : distances) {
            distance = Tools.round(distance, basicInterval, true);
            if (!histogram.containsKey(distance)) {
                histogram.put(distance, 1f);
            } else {
                Float count = histogram.get(distance);
                histogram.put(distance, count + 1f);
            }
        }
        Float lastKey = histogram.lastKey();
        while (lastKey >= 0) {
            if (!histogram.containsKey(lastKey)) {
                histogram.put(lastKey, 0f);
            } else {
                histogram.put(lastKey, histogram.get(lastKey) / distances.size());
            }
            lastKey -= basicInterval;
        }
        return histogram;
    }

    /**
     *
     * @param metricSpace
     * @param metricObjects
     * @param valuesAsMetricObjectData if true, then the values are extracted
     * data of the metric objects. If false, values are the whole metric objects
     * with IDs
     * @return
     */
    public static Map<Object, Object> getMetricObjectsAsIdObjectMap(AbstractMetricSpace metricSpace, Collection<Object> metricObjects, boolean valuesAsMetricObjectData) {
        return getMetricObjectsAsIdObjectMap(metricSpace, metricObjects.iterator(), valuesAsMetricObjectData);
    }

    public static Map<Object, Object> getMetricObjectsAsIdObjectMap(AbstractMetricSpace metricSpace, Iterator<Object> metricObjects, boolean valuesAsMetricObjectData) {
        Map<Object, Object> ret = new HashMap();
        for (int i = 0; metricObjects.hasNext(); i++) {
            Object metricObject = metricObjects.next();
            Object idOfMetricObject = metricSpace.getIDOfMetricObject(metricObject);
            Object value = valuesAsMetricObjectData ? metricSpace.getDataOfMetricObject(metricObject) : metricObject;
            ret.put(idOfMetricObject, value);
            if (i % 10000 == 0) {
                LOG.log(Level.INFO, "Loaded {0} objects into map", i);
            }
        }
        LOG.log(Level.INFO, "Finished loading map os size {0} objects", ret.size());
        return ret;
    }

    public static Object[] getData(Object[] fourObjects, AbstractMetricSpace metricSpace) {
        Object[] ret = new Object[fourObjects.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = metricSpace.getDataOfMetricObject(fourObjects[i]);
        }
        return ret;
    }

}