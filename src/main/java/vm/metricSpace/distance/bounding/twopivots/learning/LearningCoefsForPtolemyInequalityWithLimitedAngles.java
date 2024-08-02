package vm.metricSpace.distance.bounding.twopivots.learning;

import java.util.AbstractMap;
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
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.distance.DistanceFunctionInterface;
import static vm.metricSpace.distance.bounding.twopivots.impl.DataDependentGeneralisedPtolemaicFiltering.CONSTANT_FOR_PRECISION;
import vm.metricSpace.distance.bounding.twopivots.storeLearned.PtolemyInequalityWithLimitedAnglesCoefsStoreInterface;
import vm.metricSpace.distance.storedPrecomputedDistances.AbstractPrecomputedPairsOfDistancesStorage;

/**
 *
 * @author xmic
 * @param <T>
 */
public class LearningCoefsForPtolemyInequalityWithLimitedAngles<T> {

    public static final Logger LOG = Logger.getLogger(LearningCoefsForPtolemyInequalityWithLimitedAngles.class.getName());

    private final String resultName;
    private final Dataset<T> dataset;
    private final AbstractMetricSpace<T> metricSpace;
    private final DistanceFunctionInterface<T> df;
    private final List<Object> pivots;
    private final PtolemyInequalityWithLimitedAnglesCoefsStoreInterface storage;
    private final TreeSet<Map.Entry<String, Float>> smallDistsOfSampleObjectsAndQueries;

    private final boolean allPivotPairs;

    public LearningCoefsForPtolemyInequalityWithLimitedAngles(Dataset<T> dataset, List<Object> pivots, int objectsCount, int queriesCount, TreeSet<Map.Entry<String, Float>> smallDistsOfSampleObjectsAndQueries, PtolemyInequalityWithLimitedAnglesCoefsStoreInterface storage, String datasetName, boolean allPivotPairs) {
        this.dataset = dataset;
        this.metricSpace = dataset.getMetricSpace();
        this.df = dataset.getDistanceFunction();
        this.pivots = pivots;
        this.storage = storage;
        this.smallDistsOfSampleObjectsAndQueries = smallDistsOfSampleObjectsAndQueries;
        this.allPivotPairs = allPivotPairs;
        this.resultName = storage.getResultDescription(datasetName, AbstractPrecomputedPairsOfDistancesStorage.IMPLICIT_K, objectsCount, queriesCount, pivots.size(), allPivotPairs);
    }

    public Map<Object, float[]> execute() {
        ConcurrentHashMap<Object, float[]> results = new ConcurrentHashMap<>();
        ExecutorService threadPool = vm.javatools.Tools.initExecutor();
        CountDownLatch latch = new CountDownLatch(pivots.size());
        try {
            Set<Comparable> setOfIDs = AbstractPrecomputedPairsOfDistancesStorage.getIDsOfObjects(smallDistsOfSampleObjectsAndQueries);
            List objectsWithSmallestDists = ToolsMetricDomain.getObjectsForIDs(setOfIDs, dataset);
            Map<Comparable, Object> metricObjectsAsIdObjectMap = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(metricSpace, objectsWithSmallestDists);
            for (int p1 = 0; p1 < pivots.size(); p1++) {
                int finalP1 = p1;
                threadPool.execute(() -> {
                    Object[] fourObjects = new Object[4];
                    Object[] fourObjectsData = new Object[4];
                    fourObjects[0] = pivots.get(finalP1);
                    fourObjectsData[0] = metricSpace.getDataOfMetricObject(fourObjects[0]);

                    if (allPivotPairs) {
                        if (finalP1 != pivots.size() - 1) {
                            for (int p2 = finalP1 + 1; p2 < pivots.size(); p2++) {
                                fourObjects[1] = pivots.get(p2);
                                fourObjectsData[1] = metricSpace.getDataOfMetricObject(fourObjects[1]);
                                float[] extremes = learnForPivots(fourObjects, fourObjectsData, metricObjectsAsIdObjectMap);
                                synchronized (LearningCoefsForPtolemyInequalityWithLimitedAngles.class) {
                                    String pivotPairsID = metricSpace.getIDOfMetricObject(fourObjects[0]).toString() + "-" + metricSpace.getIDOfMetricObject(fourObjects[1]).toString();
                                    results.put(pivotPairsID, extremes);
                                    LOG.log(Level.INFO, "Evaluated coefs for pivot pairs {0} with the starting pivot {6}. Results: {1}; {2}; {3}; {4}. Notice first two numbers multiplied by {5} for a sake of numerical precision.", new Object[]{pivotPairsID, extremes[0], extremes[1], extremes[2], extremes[3], CONSTANT_FOR_PRECISION, finalP1});
                                }
                            }
                        }
                    } else {
                        fourObjects[1] = pivots.get((finalP1 + 1) % pivots.size());
                        fourObjectsData[1] = metricSpace.getDataOfMetricObject(fourObjects[1]);
                        float[] extremes = learnForPivots(fourObjects, fourObjectsData, metricObjectsAsIdObjectMap);
                        String pivotPairsID = metricSpace.getIDOfMetricObject(fourObjects[0]).toString() + "-" + metricSpace.getIDOfMetricObject(fourObjects[1]).toString();
                        results.put(pivotPairsID, extremes);
//                        LOG.log(Level.INFO, "Evaluated coefs for pivot pairs {0} with the starting pivot {6}. Results: {1}; {2}; {3}; {4}. Notice first two numbers multiplied by {5} for a sake of numerical precision.", new Object[]{pivotPairsID, extremes[0], extremes[1], extremes[2], extremes[3], CONSTANT_FOR_PRECISION, finalP1});
                    }
                    latch.countDown();
                    LOG.log(Level.INFO, "Remains {0} primary pivots to check. Results size: {1}", new Object[]{latch.getCount(), results.size()});
                });
            }
            latch.await();
        } catch (InterruptedException ex) {
            Logger.getLogger(LearningCoefsForPtolemyInequalityWithLimitedAngles.class.getName()).log(Level.SEVERE, null, ex);
        }
        threadPool.shutdown();
        storage.storeCoefficients(results, resultName);
        return results;
    }

    private float[] learnForPivots(Object[] fourObjects, Object[] fourObjectsData, Map metricObjectsAsIdObjectMap) {
        float[] extremes = new float[4]; // minSum, maxSum, minDiff, maxDiff
        extremes[0] = Float.MAX_VALUE;
        extremes[2] = Float.MAX_VALUE;
        for (Map.Entry<String, Float> smallDist : smallDistsOfSampleObjectsAndQueries) {
            String[] qoIDs = smallDist.getKey().split(";");
            fourObjects[2] = metricObjectsAsIdObjectMap.get(qoIDs[0]);
            fourObjects[3] = metricObjectsAsIdObjectMap.get(qoIDs[1]);
            fourObjectsData[2] = ((AbstractMap.SimpleEntry) fourObjects[2]).getValue();
            fourObjectsData[3] = ((AbstractMap.SimpleEntry) fourObjects[3]).getValue();
            float[] sixDists = ToolsMetricDomain.getPairwiseDistsOfFourObjects(df, false, fourObjectsData);
            if (sixDists == null || Tools.isZeroInArray(sixDists)) {
                continue;
            }
            float c = Math.abs(smallDist.getValue());
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

}
