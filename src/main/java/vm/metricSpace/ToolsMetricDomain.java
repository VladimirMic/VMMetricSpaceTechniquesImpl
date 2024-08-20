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
    private static final Integer BATCH_FOR_MATRIX_OF_DISTANCES = 5000000;

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
        vm.math.Tools.getIDim(vm.datatools.DataTypeConvertor.floatsToDoubles(distances), true);
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
     * @param <T>
     * @param metricSpace
     * @param metricObjects
     * @return map of IDs to full metric objects, i.e., id-data object
     */
    public static <T> Map<Comparable, Object> getMetricObjectsAsIdObjectMap(AbstractMetricSpace<T> metricSpace, Iterator<Object> metricObjects) {
        Map<Comparable, Object> ret = new HashMap();
        long t = -System.currentTimeMillis();
        for (int i = 1; metricObjects.hasNext(); i++) {
            Object metricObject = metricObjects.next();
            Comparable idOfMetricObject = metricSpace.getIDOfMetricObject(metricObject);
            T dataOfMetricObject = metricSpace.getDataOfMetricObject(metricObject);
            Object value = metricSpace.createMetricObject(idOfMetricObject, dataOfMetricObject);
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

    public static <T> Map<Comparable, T> getMetricObjectsAsIdDataMap(AbstractMetricSpace<T> metricSpace, Collection<Object> metricObjects) {
        return getMetricObjectsAsIdDataMap(metricSpace, metricObjects.iterator());
    }

    public static <T> Map<Comparable, Object> getMetricObjectsAsIdObjectMap(AbstractMetricSpace<T> metricSpace, Collection<Object> metricObjects) {
        return getMetricObjectsAsIdObjectMap(metricSpace, metricObjects.iterator());
    }

    /**
     *
     * @param <T>
     * @param metricSpace
     * @param metricObjects
     * @return map of IDs to data used for the distance function
     */
    public static <T> Map<Comparable, T> getMetricObjectsAsIdDataMap(AbstractMetricSpace<T> metricSpace, Iterator<Object> metricObjects) {
        Map<Comparable, T> ret = new HashMap();
        long t = -System.currentTimeMillis();
        for (int i = 1; metricObjects.hasNext(); i++) {
            Object metricObject = metricObjects.next();
            Comparable idOfMetricObject = metricSpace.getIDOfMetricObject(metricObject);
            T dataOfMetricObject = metricSpace.getDataOfMetricObject(metricObject);
            if (dataOfMetricObject == null) {
                continue;
            }
            ret.put(idOfMetricObject, dataOfMetricObject);
            if (i % 100000 == 0 && t + System.currentTimeMillis() > 5000) {
                LOG.log(Level.INFO, "Loaded {0} objects into map", i);
                t = -System.currentTimeMillis();
            }
        }
        LOG.log(Level.INFO, "Finished loading map of size {0} objects", ret.size());
        return ret;
    }

    public static <T> T[] getData(Object[] objects, AbstractMetricSpace<T> metricSpace) {
        List<T> retList = new ArrayList<>();
        for (Object object : objects) {
            retList.add(metricSpace.getDataOfMetricObject(object));
        }
        return (T[]) retList.toArray();
    }

    public static <T> List<T> getDataAsList(Iterator objects, AbstractMetricSpace<T> metricSpace) {
        List ret = new ArrayList<>();
        while (objects.hasNext()) {
            Object next = objects.next();
            ret.add(metricSpace.getDataOfMetricObject(next));
        }
        return ret;
    }

    public static Set<Comparable> getIDs(Iterator<Object> objects, AbstractMetricSpace metricSpace) {
        Set<Comparable> ret = new HashSet<>();
        for (int i = 1; objects.hasNext(); i++) {
            Object next = objects.next();
            ret.add(metricSpace.getIDOfMetricObject(next));
            if (i % 1000000 == 0) {
                LOG.log(Level.INFO, "Loaded {0} keys", i);
            }
        }
        return ret;
    }

    public static List<Comparable> getIDsAsList(Iterator<Object> objects, AbstractMetricSpace metricSpace) {
        List<Comparable> ret = new ArrayList<>();
        for (int i = 1; objects.hasNext(); i++) {
            Object next = objects.next();
            ret.add(metricSpace.getIDOfMetricObject(next));
            if (i % 1000000 == 0) {
                LOG.log(Level.INFO, "Loaded {0} keys", i);
            }
        }
        return ret;
    }

    public static <T> Object[] getPivotPermutation(AbstractMetricSpace<T> metricSpace, DistanceFunctionInterface df, List<Object> pivots, Object referent, int prefixLength, Map<Comparable, Float> distsToPivotsStorage) {
        Map<Comparable, T> pivotsMap = ToolsMetricDomain.getMetricObjectsAsIdDataMap(metricSpace, pivots);
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
    public static <T> Comparable[] getPivotIDsPermutation(DistanceFunctionInterface df, Map<Comparable, T> pivotsMap, Object referentData, int prefixLength, Map<Comparable, Float> distsToPivotsStorage) {
        if (prefixLength < 0) {
            prefixLength = Integer.MAX_VALUE;
        }
        TreeSet<Map.Entry<Comparable, Float>> map = getPivotIDsPermutationWithDists(df, pivotsMap, referentData, prefixLength);
        if (distsToPivotsStorage != null) {
            for (Map.Entry<Comparable, Float> entry : map) {
                distsToPivotsStorage.put(entry.getKey(), entry.getValue());
            }
        }
        Comparable[] ret = new Comparable[Math.min(pivotsMap.size(), prefixLength)];
        Iterator<Map.Entry<Comparable, Float>> it = map.iterator();
        for (int i = 0; it.hasNext() && i < prefixLength; i++) {
            Map.Entry<Comparable, Float> next = it.next();
            ret[i] = next.getKey();
        }
        return ret;
    }

    public static <T> TreeSet<Map.Entry<Comparable, Float>> getPivotIDsPermutationWithDists(DistanceFunctionInterface df, Map<Comparable, T> pivotsMap, Object referentData, int prefixLength) {
        TreeSet<Map.Entry<Comparable, Float>> ret = new TreeSet<>(new vm.datatools.Tools.MapByFloatValueComparator());
        for (Map.Entry<Comparable, T> pivot : pivotsMap.entrySet()) {
            Float dist = df.getDistance(referentData, pivot.getValue());
            Map.Entry<Comparable, Float> entry = new AbstractMap.SimpleEntry<>(pivot.getKey(), dist);
            ret.add(entry);
        }
        return ret;
    }

    public static List<Object> getPrefixesOfVectors(AbstractMetricSpace metricSpace, List<Object> vectors, int finalDimensions) {
        List<Object> ret = new ArrayList<>();
        for (Object obj : vectors) {
            Object oData = metricSpace.getDataOfMetricObject(obj);
            Comparable oID = metricSpace.getIDOfMetricObject(obj);
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
        final Map<Comparable, Integer> columnHeaders = new ConcurrentHashMap<>();
        final Map<Comparable, Integer> rowHeaders = new ConcurrentHashMap<>();
        for (int i = 0; i < pivots.size(); i++) {
            Object p = pivots.get(i);
            Comparable pID = metricSpace.getIDOfMetricObject(p);
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
                int limit = objCount > 0 ? objCount : Integer.MAX_VALUE;
                limit = Math.min(BATCH_FOR_MATRIX_OF_DISTANCES, limit - batchCount * BATCH_FOR_MATRIX_OF_DISTANCES);
                limit = Math.max(limit, 0);
                if (limit == 0) {
                    break;
                }
                List<Object> batch = vm.datatools.Tools.getObjectsFromIterator(metricObjectsFromDataset, limit, -1);
                CountDownLatch latch = new CountDownLatch(batch.size());
                float[][] distsForBatch = new float[batch.size()][pivots.size()];
                int rowCounter;
                for (rowCounter = 0; rowCounter < batch.size(); rowCounter++) {
                    final int rowCounterFinal = rowCounter + batchCount * BATCH_FOR_MATRIX_OF_DISTANCES;
                    final int batchRowCounter = rowCounter;
                    threadPool.execute(() -> {
                        Object o = batch.get(batchRowCounter);
                        Comparable oID = metricSpace.getIDOfMetricObject(o);
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

    public static Map<Comparable, Float> getVectorsLength(List batch, AbstractMetricSpace metricSpace) {
        Map<Comparable, Float> ret = new HashMap<>();
        for (Object object : batch) {
            Comparable id = metricSpace.getIDOfMetricObject(object);
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

    public static <T> Map<Comparable, Float> evaluateDistsToPivots(T qData, Map<Comparable, T> pivotsMap, DistanceFunctionInterface<T> df) {
        Map<Comparable, Float> ret = new HashMap<>();
        for (Map.Entry<Comparable, T> entry : pivotsMap.entrySet()) {
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
            Comparable idOfMetricObject = metricSpace.getIDOfMetricObject(obj);
            if (idsSet.contains(idOfMetricObject)) {
                ret.add(obj);
            }
        });
        return ret;
    }

    public static Object transformMetricObjectToOtherRepresentation(Object object, AbstractMetricSpace metricSpaceSource, AbstractMetricSpace metricSpaceDest) {
        Comparable id = metricSpaceSource.getIDOfMetricObject(object);
        Object data = metricSpaceSource.getDataOfMetricObject(object);
        Object o = metricSpaceDest.createMetricObject(id, data);
        return o;
    }

    public static List<Object> transformMetricObjectsToOtherRepresentation(List<Object> objects, AbstractMetricSpace metricSpaceSource, AbstractMetricSpace metricSpaceDest) {
        return transformMetricObjectsToOtherRepresentation(objects, metricSpaceSource, metricSpaceDest, null);
    }

    public static List transformMetricObjectsToOtherRepresentation(List objects, AbstractMetricSpace metricSpaceSource, AbstractMetricSpace metricSpaceDest, Set<Comparable> alreadyDoneIDs) {
        List<Object> ret = new ArrayList<>();
        for (Object object : objects) {
            if (alreadyDoneIDs != null && !alreadyDoneIDs.isEmpty()) {
                Comparable id = metricSpaceSource.getIDOfMetricObject(object);
                if (alreadyDoneIDs.contains(id)) {
                    continue;
                }
            }
            Object o = transformMetricObjectToOtherRepresentation(object, metricSpaceSource, metricSpaceDest);
            ret.add(o);
        }
        return ret;
    }

    public static <T> List getObjectsForIDs(Collection<Comparable> setOfIDs, Dataset<T> dataset) {
        AbstractMetricSpace<T> metricSpace = dataset.getMetricSpace();
        Map<Comparable, T> keyValueStorage = null;
        if (dataset.hasKeyValueStorage()) {
            keyValueStorage = dataset.getKeyValueStorage();
        }
        if (keyValueStorage == null) {
            keyValueStorage = ToolsMetricDomain.getMetricObjectsAsIdDataMap(metricSpace, dataset.getMetricObjectsFromDataset());
        }
        List<Object> ret = new ArrayList<>();
        for (Comparable id : setOfIDs) {
            T data = keyValueStorage.get(id);
            if (data == null) {
                throw new IllegalArgumentException("Storage does not contain id " + id + ". SStorage contains " + keyValueStorage.size() + " objects");
            }
            Object o = metricSpace.createMetricObject(id, data);
            ret.add(o);
        }
        return ret;
    }

}
