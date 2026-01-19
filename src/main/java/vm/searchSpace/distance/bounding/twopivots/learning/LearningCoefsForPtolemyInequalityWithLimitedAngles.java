package vm.searchSpace.distance.bounding.twopivots.learning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.Dataset;
import vm.searchSpace.ToolsSpaceDomain;
import vm.searchSpace.distance.AbstractDistanceFunction;
import static vm.searchSpace.distance.bounding.twopivots.impl.DataDependentPtolemaicFiltering.CONSTANT_FOR_PRECISION;
import vm.searchSpace.distance.bounding.twopivots.storeLearned.PtolemyInequalityWithLimitedAnglesCoefsStoreInterface;
import vm.searchSpace.distance.storedPrecomputedDistances.AbstractPrecomputedPairsOfDistancesStorage;

/**
 *
 * @author xmic
 * @param <T>
 */
public class LearningCoefsForPtolemyInequalityWithLimitedAngles<T> {

    public static final Logger LOG = Logger.getLogger(LearningCoefsForPtolemyInequalityWithLimitedAngles.class.getName());

    private final String resultName;
    private final AbstractSearchSpace<T> searchSpace;
    private final AbstractDistanceFunction<T> df;
    private final List<Object> pivots;
    private final PtolemyInequalityWithLimitedAnglesCoefsStoreInterface storage;
    private final List dataPairsForSmallestDists;

    private final boolean allPivotPairs;

    public LearningCoefsForPtolemyInequalityWithLimitedAngles(Dataset<T> dataset, List<Object> pivots, int objectsCount, int queriesCount, TreeSet<Map.Entry<String, Float>> smallDistsOfSampleObjectsAndQueries, PtolemyInequalityWithLimitedAnglesCoefsStoreInterface storage, String datasetName, boolean allPivotPairs) {
        this.searchSpace = dataset.getSearchSpace();
        this.df = dataset.getDistanceFunction();
        this.pivots = pivots;
        this.storage = storage;
        this.allPivotPairs = allPivotPairs;
        this.resultName = storage.getResultDescription(datasetName, AbstractPrecomputedPairsOfDistancesStorage.IMPLICIT_K, objectsCount, queriesCount, pivots.size(), allPivotPairs);
        Set<Comparable> setOfIDs = AbstractPrecomputedPairsOfDistancesStorage.getIDsOfObjects(smallDistsOfSampleObjectsAndQueries);
        List objectsWithSmallestDists = ToolsSpaceDomain.getObjectsForIDs(setOfIDs, dataset);
        Map<Comparable, T> searchObjectsAsIdObjectMap = ToolsSpaceDomain.getSearchObjectsAsIdDataMap(searchSpace, objectsWithSmallestDists);
        this.dataPairsForSmallestDists = transformToListOfDataPairs(smallDistsOfSampleObjectsAndQueries, searchObjectsAsIdObjectMap);
    }

    public Map<Object, float[]> execute() {
        Map<Object, float[]> results = new HashMap<>();
        ExecutorService threadPool = vm.javatools.Tools.initExecutor();
        CountDownLatch latch = new CountDownLatch(pivots.size());
        Map<Object, float[]>[] partialResults = new Map[pivots.size()];
        try {
            for (int p1 = 0; p1 < pivots.size(); p1++) {
                int finalP1 = p1;
                partialResults[p1] = new HashMap();
                threadPool.execute(() -> {
                    Object[] fourObjects = new Object[4];
                    Comparable[] fourObjectsIDs = new Comparable[4];
                    T[] fourObjectsData = (T[]) new Object[4];
                    fourObjects[0] = pivots.get(finalP1);
                    fourObjectsIDs[0] = searchSpace.getIDOfObject(fourObjects[0]);
                    fourObjectsData[0] = searchSpace.getDataOfObject(fourObjects[0]);
                    if (allPivotPairs) {
                        if (finalP1 != pivots.size() - 1) {
                            for (int p2 = finalP1 + 1; p2 < pivots.size(); p2++) {
                                fourObjects[1] = pivots.get(p2);
                                fourObjectsIDs[1] = searchSpace.getIDOfObject(fourObjects[1]);
                                fourObjectsData[1] = searchSpace.getDataOfObject(fourObjects[1]);
                                float[] extremes = learnForPivots(fourObjectsData);
                                partialResults[finalP1].put(fourObjectsIDs[0] + "-" + fourObjectsIDs[1], extremes);
//                                LOG.log(Level.INFO, "Evaluated coefs for pivot pairs {0} with the starting pivot {6}. Results: {1}; {2}; {3}; {4}. Notice first two numbers multiplied by {5} for a sake of numerical precision.", new Object[]{fourObjectsIDs[0] + "-" + fourObjectsIDs[1], extremes[0], extremes[1], extremes[2], extremes[3], CONSTANT_FOR_PRECISION, finalP1});
                            }
                            LOG.log(Level.INFO, "Evaluated coefs for all pivot pairs with the starting pivot {0}", new Object[]{finalP1});
                        }
                    } else {
                        fourObjectsData[1] = searchSpace.getDataOfObject(fourObjects[1]);
                        float[] extremes = learnForPivots(fourObjectsData);
                        String pivotPairsID = searchSpace.getIDOfObject(fourObjects[0]).toString() + "-" + searchSpace.getIDOfObject(fourObjects[1]).toString();
                        partialResults[finalP1].put(pivotPairsID, extremes);
//                        LOG.log(Level.INFO, "Evaluated coefs for pivot pairs {0} with the starting pivot {6}. Results: {1}; {2}; {3}; {4}. Notice first two numbers multiplied by {5} for a sake of numerical precision.", new Object[]{pivotPairsID, extremes[0], extremes[1], extremes[2], extremes[3], CONSTANT_FOR_PRECISION, finalP1});
                    }
                    latch.countDown();
                    LOG.log(Level.INFO, "Remains {0} primary pivots to check", new Object[]{latch.getCount()});
                });
            }
            latch.await();
            for (Map<Object, float[]> partialResult : partialResults) {
                results.putAll(partialResult);
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(LearningCoefsForPtolemyInequalityWithLimitedAngles.class.getName()).log(Level.SEVERE, null, ex);
        }
        threadPool.shutdown();
        storage.storeCoefficients(results, resultName);
        return results;
    }

    private final ConcurrentHashMap<T, Map<T, Float>> cache = new ConcurrentHashMap<>();

    private float getDistWithCaching(T o1, T o2) {
        if (!cache.containsKey(o1)) {
            cache.put(o1, new ConcurrentHashMap<>());
        }
        Map<T, Float> map = cache.get(o1);
        if (map.containsKey(o2)) {
            return map.get(o2);
        }
        float ret = df.getDistance(o1, o2);
        map.put(o2, ret);
        return ret;
    }

    private float[] learnForPivots(T[] fourObjectsData) {
        float[] extremes = new float[4]; // minSum, maxSum, minDiff, maxDiff
        extremes[0] = Float.MAX_VALUE;
        extremes[2] = Float.MAX_VALUE;
        float[] sixDists = new float[6];
        sixDists[0] = df.getDistance(fourObjectsData[0], fourObjectsData[1]);
        for (int i = 0; i < dataPairsForSmallestDists.size(); i += 3) {
            T o2Data = (T) dataPairsForSmallestDists.get(i);
            T o3Data = (T) dataPairsForSmallestDists.get(i + 1);
            fourObjectsData[2] = o2Data;
            fourObjectsData[3] = o3Data;
            sixDists[2] = (float) dataPairsForSmallestDists.get(i + 2);
            sixDists[3] = getDistWithCaching(fourObjectsData[0], fourObjectsData[3]);
            sixDists[4] = getDistWithCaching(fourObjectsData[0], fourObjectsData[2]);
            sixDists[5] = getDistWithCaching(fourObjectsData[1], fourObjectsData[3]);
            sixDists[1] = getDistWithCaching(fourObjectsData[1], fourObjectsData[2]);
//            sixDists[3] = df.getDistance(fourObjectsData[0], fourObjectsData[3]);
//            sixDists[4] = df.getDistance(fourObjectsData[0], fourObjectsData[2]);
//            sixDists[5] = df.getDistance(fourObjectsData[1], fourObjectsData[3]);
//            sixDists[1] = df.getDistance(fourObjectsData[1], fourObjectsData[2]);

            if (sixDists == null || Tools.isZeroInArray(sixDists)) {
                continue;
            }
            float c = sixDists[2];
            float ef = Math.abs(sixDists[4] * sixDists[5]);
            float bd = Math.abs(sixDists[1] * sixDists[3]);
            float fractionSum = CONSTANT_FOR_PRECISION * c / (bd + ef);
            float fractionDiff = c / Math.abs(ef - bd);
            // minSum, maxSum, minDiff, maxDiff
            extremes[0] = Math.min(extremes[0], fractionSum);
            extremes[1] = Math.max(extremes[1], fractionSum);
            extremes[2] = Math.min(extremes[2], fractionDiff);
            extremes[3] = Math.max(extremes[3], fractionDiff);
        }
        return extremes;
    }

    private List transformToListOfDataPairs(TreeSet<Map.Entry<String, Float>> smallDists, Map<Comparable, T> searchObjectsAsIdObjectMap) {
        List ret = new ArrayList<>();
        for (Map.Entry<String, Float> entry : smallDists) {
            String[] ids = entry.getKey().split(";");
            ret.add(searchObjectsAsIdObjectMap.get(ids[0]));
            ret.add(searchObjectsAsIdObjectMap.get(ids[1]));
            ret.add(entry.getValue());
        }
        return Collections.unmodifiableList(ret);
    }

}
