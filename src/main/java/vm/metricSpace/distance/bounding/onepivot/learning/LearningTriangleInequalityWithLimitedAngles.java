package vm.metricSpace.distance.bounding.onepivot.learning;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.metricSpace.distance.bounding.onepivot.storeLearned.TriangleInequalityWithLimitedAnglesCoefsStoreInterface;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class LearningTriangleInequalityWithLimitedAngles<T> {

    public static final Logger LOG = Logger.getLogger(LearningTriangleInequalityWithLimitedAngles.class.getName());
    public static final Float RATIO_OF_SMALLEST_DISTS = 0.4f / 100f;

    private final String resultName;
    private final AbstractMetricSpace<T> metricSpace;
    private final DistanceFunctionInterface<T> df;
    private final List<Object> pivots;
    private final List<Object> sampleObjects;
    private final List<Object> sampleQueries;
    private final TriangleInequalityWithLimitedAnglesCoefsStoreInterface storage;

    public LearningTriangleInequalityWithLimitedAngles(AbstractMetricSpace<T> metricSpace, DistanceFunctionInterface<T> df, List<Object> pivots, List<Object> sampleObjects, List<Object> sampleQueries, TriangleInequalityWithLimitedAnglesCoefsStoreInterface storage, String resultName) {
        this.metricSpace = metricSpace;
        this.df = df;
        this.pivots = pivots;
        this.sampleObjects = sampleObjects;
        this.sampleQueries = sampleQueries;
        this.storage = storage;
        this.resultName = resultName;
    }

    public Map<Object, Float> execute() {
        SortedSet<Map.Entry> dists = distances(sampleObjects, sampleQueries);
        int lastIndex = (int) (RATIO_OF_SMALLEST_DISTS * dists.size());
        Map<Object, Float> ret = new HashMap<>();
        for (Object pivot : pivots) {
            T pivotData = metricSpace.getDataOfMetricObject(pivot);
            Iterator<Map.Entry> it = dists.iterator();
            float coefForP = Float.MAX_VALUE;
            for (int i = 0; i < lastIndex && it.hasNext(); i++) {
                Map.Entry dist = it.next();
                T[] oqData = (T[]) dist.getKey();
                float c = (float) dist.getValue();
                float dPO = df.getDistance(pivotData, oqData[0]);
                float dPQ = df.getDistance(pivotData, oqData[1]);
                if (dPO == 0 || dPQ == 0 || c == 0) {
                    continue;
                }
                float coef = c / Math.abs(dPO - dPQ);
                coefForP = Math.min(coefForP, coef);
            }
            Object pivotID = metricSpace.getIDOfMetricObject(pivot);
            ret.put(pivotID, coefForP);
            LOG.log(Level.INFO, "Evaluated pivot coef {0} for pivot {1}", new Object[]{coefForP, pivotID.toString()});
        }
        String description = storage.getResultDescription(resultName, pivots.size(), sampleObjects.size(), sampleQueries.size(), RATIO_OF_SMALLEST_DISTS);
        storage.storeCoefficients(ret, description);
        return ret;
    }

    private SortedSet<Map.Entry> distances(List<Object> sample1, List<Object> sample2) {
        TreeSet ret = new TreeSet<>(new Tools.MapByValueComparatorWithOwnKeyComparator<>(new Tools.ObjectArrayIdentityComparator()));
        for (Object o1 : sample1) {
            T o1Data = metricSpace.getDataOfMetricObject(o1);
            for (Object o2 : sample2) {
                T o2Data = metricSpace.getDataOfMetricObject(o2);
                float dist = df.getDistance(o1Data, o2Data);
                Object[] key = new Object[]{o1Data, o2Data};
                ret.add(new AbstractMap.SimpleEntry(key, dist));
            }
        }
        return ret;
    }

}
