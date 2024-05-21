package vm.metricSpace;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.math.Tools;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.metricSpace.distance.storedPrecomputedDistances.MainMemoryStoredPrecomputedDistances;

/**
 *
 * @author Vlada
 */
public class ToolsMetricDomain {

    private static final Logger LOG = Logger.getLogger(ToolsMetricDomain.class.getName());
    private static final Integer BATCH_FOR_MATRIX_OF_DISTANCES = 2000000;

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
     * @param dataset
     * @param objCount
     * @param distCount
     * @param idsOfRandomPairs
     * @return
     */
    public static SortedMap<Float, Float> createDistanceDensityPlot(Dataset dataset, int objCount, int distCount, List<Object[]> idsOfRandomPairs) {
        float[] distances = dataset.evaluateSampleOfRandomDistances(objCount, distCount, idsOfRandomPairs);
        return createDistanceDensityPlot(distances);
    }

    private static float basicInterval;

    public static float getBasicInterval() {
        return basicInterval;
    }

    /**
     * Evaluates distCount distances of random pairs of objects from the list
     * metricObjectsSample
     *
     * @param distances
     * @param pairsOfExaminedIDs if not null, adds all examined pairs of objects
     * @return
     */
    public static TreeMap<Float, Float> createDistanceDensityPlot(float[] distances) {
        int distCount = distances.length;
        TreeMap<Float, Float> absoluteCounts = new TreeMap<>();
        basicInterval = computeBasicDistInterval(distances);
        LOG.log(Level.INFO, "Basic interval is set to {0}", basicInterval);
        for (float distance : distances) {
            distance = Tools.round(distance, basicInterval, false);
            if (!absoluteCounts.containsKey(distance)) {
                absoluteCounts.put(distance, 1f);
            } else {
                Float count = absoluteCounts.get(distance);
                absoluteCounts.put(distance, count + 1);
            }
        }
        TreeMap<Float, Float> histogram = new TreeMap<>();
        for (Float key : absoluteCounts.keySet()) {
            histogram.put(key, absoluteCounts.get(key) / distCount);
        }
        return histogram;
    }

    public static SortedMap<Float, Float> createDistanceDensityPlot(Collection<Float> distances) {
        float[] array = vm.datatools.DataTypeConvertor.objectsToPrimitiveFloats(distances.toArray());
        return createDistanceDensityPlot(array);
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
        long t = -System.currentTimeMillis();
        for (int i = 1; metricObjects.hasNext(); i++) {
            Object metricObject = metricObjects.next();
            Object idOfMetricObject = metricSpace.getIDOfMetricObject(metricObject);
            Object dataOfMetricObject = metricSpace.getDataOfMetricObject(metricObject);
            Object value = valuesAsMetricObjectData ? dataOfMetricObject : metricSpace.createMetricObject(idOfMetricObject, dataOfMetricObject);
            if (dataOfMetricObject == null) {
                continue;
            }
            ret.put(idOfMetricObject, value);
            if (i % 100000 == 0 && t + System.currentTimeMillis() > 5000) {
                LOG.log(Level.INFO, "Loaded {0} objects into map", i);
                t = -System.currentTimeMillis();
            }
        }
        LOG.log(Level.INFO, "Finished loading map of size {0} objects", ret.size());
        return ret;
    }

    public static Object[] getData(Object[] objects, AbstractMetricSpace metricSpace) {
        Object[] ret = new Object[objects.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = metricSpace.getDataOfMetricObject(objects[i]);
        }
        return ret;
    }

    public static List<Object> getDataAsList(Iterator objects, AbstractMetricSpace metricSpace) {
        List ret = new ArrayList<>();
        while (objects.hasNext()) {
            Object next = objects.next();
            ret.add(metricSpace.getDataOfMetricObject(next));
        }
        return ret;
    }

    public static Set<Object> getIDs(Iterator<Object> objects, AbstractMetricSpace metricSpace) {
        Set<Object> ret = new HashSet<>();
        for (int i = 1; objects.hasNext(); i++) {
            Object next = objects.next();
            ret.add(metricSpace.getIDOfMetricObject(next));
            if (i % 1000000 == 0) {
                LOG.log(Level.INFO, "Loaded {0} keys", i);
            }
        }
        return ret;
    }

    public static List<Object> getIDsAsList(Iterator<Object> objects, AbstractMetricSpace metricSpace) {
        List<Object> ret = new ArrayList<>();
        for (int i = 1; objects.hasNext(); i++) {
            Object next = objects.next();
            ret.add(metricSpace.getIDOfMetricObject(next));
            if (i % 1000000 == 0) {
                LOG.log(Level.INFO, "Loaded {0} keys", i);
            }
        }
        return ret;
    }

    public static Object[] getPivotPermutation(AbstractMetricSpace metricSpace, DistanceFunctionInterface df, List<Object> pivots, Object referent, int prefixLength, Map<Object, Float> distsToPivotsStorage) {
        Map<Object, Object> pivotsMap = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(metricSpace, pivots, true);
        Object referentData = metricSpace.getDataOfMetricObject(referent);
        return getPivotIDsPermutation(df, pivotsMap, referentData, prefixLength, distsToPivotsStorage);
    }

    public static int[] getPivotPermutationIndexes(float[] dists, int prefixLength) {
        if (prefixLength < 0) {
            prefixLength = Integer.MAX_VALUE;
        }
        Comparator comp = new vm.datatools.Tools.MapByFloatValueComparator<Integer>();
        SortedSet<Map.Entry<Integer, Float>> perm = new TreeSet<>(comp);
        for (int i = 0; i < dists.length; i++) {
            float distance = dists[i];
            perm.add(new AbstractMap.SimpleEntry<>(i, distance));
        }
        prefixLength = Math.min(prefixLength, dists.length);
        int[] ret = new int[prefixLength];
        Iterator<Map.Entry<Integer, Float>> it = perm.iterator();
        for (int i = 0; i < prefixLength && it.hasNext(); i++) {
            ret[i] = it.next().getKey();
        }
        return ret;

    }

    public static int[] getPivotPermutationIndexes(AbstractMetricSpace metricSpace, DistanceFunctionInterface df, List pivotsData, Object referentData, int prefixLength) {
        if (prefixLength < 0) {
            prefixLength = Integer.MAX_VALUE;
        }
        Comparator comp = new vm.datatools.Tools.MapByFloatValueComparator<Integer>();
        SortedSet<Map.Entry<Integer, Float>> perm = new TreeSet<>(comp);
        for (int i = 0; i < pivotsData.size(); i++) {
            float distance = df.getDistance(pivotsData.get(i), referentData);
            perm.add(new AbstractMap.SimpleEntry<>(i, distance));
        }
        prefixLength = Math.min(prefixLength, pivotsData.size());
        int[] ret = new int[prefixLength];
        Iterator<Map.Entry<Integer, Float>> it = perm.iterator();
        for (int i = 0; i < prefixLength && it.hasNext(); i++) {
            ret[i] = it.next().getKey();
        }
        return ret;
    }

    /**
     *
     * @param df
     * @param pivotsMap
     * @param referentData
     * @param prefixLength
     * @param distsToPivotsStorage
     * @return ids of the closest pivots
     */
    public static Object[] getPivotIDsPermutation(DistanceFunctionInterface df, Map pivotsMap, Object referentData, int prefixLength, Map<Object, Float> distsToPivotsStorage) {
        if (prefixLength < 0) {
            prefixLength = Integer.MAX_VALUE;
        }
        TreeSet<Map.Entry<Object, Float>> map = getPivotIDsPermutationWithDists(df, pivotsMap, referentData, prefixLength);
        if (distsToPivotsStorage != null) {
            for (Map.Entry<Object, Float> entry : map) {
                distsToPivotsStorage.put(entry.getKey(), entry.getValue());
            }
        }
        Object[] ret = new Object[Math.min(pivotsMap.size(), prefixLength)];
        Iterator<Map.Entry<Object, Float>> it = map.iterator();
        for (int i = 0; it.hasNext() && i < prefixLength; i++) {
            Map.Entry<Object, Float> next = it.next();
            ret[i] = next.getKey();
        }
        return ret;
    }

    public static TreeSet<Map.Entry<Object, Float>> getPivotIDsPermutationWithDists(DistanceFunctionInterface df, Map<Object, Object> pivotsMap, Object referentData, int prefixLength) {
        TreeSet<Map.Entry<Object, Float>> ret = new TreeSet<>(new vm.datatools.Tools.MapByFloatValueComparator());
        for (Map.Entry<Object, Object> pivot : pivotsMap.entrySet()) {
            Float dist = df.getDistance(referentData, pivot.getValue());
            Map.Entry<Object, Float> entry = new AbstractMap.SimpleEntry<>(pivot.getKey(), dist);
            ret.add(entry);
        }
        return ret;
    }

    public static List<Object> getPrefixesOfVectors(AbstractMetricSpace metricSpace, List<Object> vectors, int finalDimensions) {
        List<Object> ret = new ArrayList<>();
        for (Object obj : vectors) {
            Object oData = metricSpace.getDataOfMetricObject(obj);
            Object oID = metricSpace.getIDOfMetricObject(obj);
            Object vec = null;
            if (oData instanceof float[]) {
                vec = new float[finalDimensions];
            } else if (oData instanceof double[]) {
                vec = new double[finalDimensions];
            }
            System.arraycopy(oData, 0, vec, 0, finalDimensions);
            ret.add(new AbstractMap.SimpleEntry<>(oID, vec));
        }
        return ret;
    }

    public static MainMemoryStoredPrecomputedDistances evaluateMatrixOfDistances(Iterator metricObjectsFromDataset, List pivots, AbstractMetricSpace metricSpace, DistanceFunctionInterface df) {
        return evaluateMatrixOfDistances(metricObjectsFromDataset, pivots, metricSpace, df, -1);
    }

    public static MainMemoryStoredPrecomputedDistances evaluateMatrixOfDistances(Iterator metricObjectsFromDataset, List pivots, AbstractMetricSpace metricSpace, DistanceFunctionInterface df, int objCount) {
        final Map<Object, Integer> columnHeaders = new ConcurrentHashMap<>();
        final Map<Object, Integer> rowHeaders = new ConcurrentHashMap<>();
        for (int i = 0; i < pivots.size(); i++) {
            Object p = pivots.get(i);
            Object pID = metricSpace.getIDOfMetricObject(p);
            columnHeaders.put(pID.toString(), i);
        }
        ExecutorService threadPool = vm.javatools.Tools.initExecutor(vm.javatools.Tools.PARALELISATION);
        float[][] ret = null;
        if (objCount > 0) {
            ret = new float[objCount][pivots.size()];
        }
        int curAnsIdx = 0;
        for (int batchCount = 0; metricObjectsFromDataset.hasNext(); batchCount++) {
            System.gc();
            try {
                List<Object> batch = vm.datatools.Tools.getObjectsFromIterator(metricObjectsFromDataset, BATCH_FOR_MATRIX_OF_DISTANCES);
                CountDownLatch latch = new CountDownLatch(batch.size());
                float[][] distsForBatch = new float[batch.size()][pivots.size()];
                int rowCounter;
                for (rowCounter = 0; rowCounter < batch.size(); rowCounter++) {
                    final int rowCounterFinal = rowCounter + batchCount * BATCH_FOR_MATRIX_OF_DISTANCES;
                    final int batchRowCounter = rowCounter;
                    threadPool.execute(() -> {
                        Object o = batch.get(batchRowCounter);
                        Object oID = metricSpace.getIDOfMetricObject(o);
                        Object oData = metricSpace.getDataOfMetricObject(o);
                        rowHeaders.put(oID.toString(), rowCounterFinal);
                        float[] row = new float[pivots.size()];
                        for (int i = 0; i < pivots.size(); i++) {
                            Object p = pivots.get(i);
                            Object pData = metricSpace.getDataOfMetricObject(p);
                            row[i] = df.getDistance(oData, pData);
                        }
                        distsForBatch[batchRowCounter] = row;
                        if ((rowCounterFinal + 1) % (BATCH_FOR_MATRIX_OF_DISTANCES / 10) == 0) {
                            LOG.log(Level.INFO, "Evaluated dists between {0} o and {1} pivots", new Object[]{(rowCounterFinal + 1), pivots.size()});
                        }
                        latch.countDown();
                    });
                }
                latch.await();
                if (ret == null) {
                    ret = distsForBatch;
                } else {
                    if (objCount > 0) {
                        System.arraycopy(distsForBatch, 0, ret, curAnsIdx, distsForBatch.length);
                        curAnsIdx += distsForBatch.length;
                    } else {
                        float[][] newRet = new float[ret.length + distsForBatch.length][pivots.size()];
                        System.arraycopy(ret, 0, newRet, 0, ret.length);
                        System.arraycopy(distsForBatch, 0, newRet, ret.length, distsForBatch.length);
                        ret = newRet;
                        System.gc();
                    }
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(ToolsMetricDomain.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        threadPool.shutdown();
        MainMemoryStoredPrecomputedDistances pd = new MainMemoryStoredPrecomputedDistances(ret, columnHeaders, rowHeaders);
        return pd;
    }

    public static Map<Object, Float> getVectorsLength(List batch, AbstractMetricSpace metricSpace) {
        Map<Object, Float> ret = new HashMap<>();
        for (Object object : batch) {
            Object id = metricSpace.getIDOfMetricObject(object);
            float length = 0;
            float[] vector = (float[]) metricSpace.getDataOfMetricObject(object); // must be the space of floats
            for (int i = 0; i < vector.length; i++) {
                float f = vector[i];
                length += f * f;
            }
            length = (float) Math.sqrt(length);
            ret.put(id, length);
        }
        return ret;
    }

    public static final float[] getPairwiseDistsOfFourObjects(DistanceFunctionInterface df, boolean enforceEFgeqBD, Object... fourObjects) {
        if (fourObjects.length < 4) {
            throw new IllegalArgumentException("At least four objects must be provided");
        }
        float[] ret = new float[6];
        ret[4] = df.getDistance(fourObjects[0], fourObjects[2]);
        ret[5] = df.getDistance(fourObjects[1], fourObjects[3]);
        for (int i = 0; i < 6; i++) {
            if (i < 4) {
                ret[i] = df.getDistance(fourObjects[i], fourObjects[(i + 1) % 4]);
            }
            if (ret[i] == 0) {
                return null;
            }
        }
        if (enforceEFgeqBD && ret[4] * ret[5] < ret[1] * ret[3]) {
            float b = ret[4];
            float d = ret[5];
            float e = ret[1];
            float f = ret[3];
            ret[1] = b;
            ret[3] = d;
            ret[4] = e;
            ret[5] = f;
        }
        return ret;
    }

    public static <T> Map<Object, Float> evaluateDistsToPivots(T qData, Map<Object, T> pivotsMap, DistanceFunctionInterface<T> df) {
        Map<Object, Float> ret = new HashMap<>();
        for (Map.Entry<Object, T> entry : pivotsMap.entrySet()) {
            float dist = df.getDistance(qData, entry.getValue());
            ret.put(entry.getKey(), dist);
        }
        return ret;
    }

    private static float computeBasicDistInterval(float[] distances) {
        float max = (float) Tools.getMax(distances);
        return computeBasicDistInterval(max);
    }

    public static float computeBasicDistInterval(float max) {
        int exp = 0;
        while (max < 1) {
            max *= 10;
            exp++;
        }
        float maxCopy = max;
        int untilZero = (int) maxCopy;
        float prev;
        int counter = 0;
        if (untilZero < 0) {
            while (untilZero != maxCopy) {
                untilZero = (int) (maxCopy * 10);
                maxCopy *= 10;
                counter--;
            }
        }
        prev = (int) maxCopy;
        untilZero = (int) (maxCopy / 10);
        while (untilZero != 0) {
            prev = untilZero;
            untilZero = (int) (maxCopy / 10);
            maxCopy /= 10;
            counter++;
        }
        counter -= 3;
        float ret = (float) (prev * Math.pow(10, counter));
        while (80 * ret > max) {
            ret /= 1.2;
        }
        while (200 * ret < max) {
            ret *= 1.2;
        }
        ret = (float) (ret / Math.pow(10, exp));
        ret = Tools.ifSmallerThanOneRoundToFirstNonzeroFloatingNumber(ret);
        return ret;
    }

    public static List filterObjectsByIDs(AbstractMetricSpace metricSpace, List objects, Object... ids) {
        List ret = new ArrayList<>();
        Set idsSet = new HashSet();
        idsSet.addAll(Arrays.asList(ids));
        objects.forEach((Object obj) -> {
            Object idOfMetricObject = metricSpace.getIDOfMetricObject(obj);
            if (idsSet.contains(idOfMetricObject)) {
                ret.add(obj);
            }
        });
        return ret;
    }

}
