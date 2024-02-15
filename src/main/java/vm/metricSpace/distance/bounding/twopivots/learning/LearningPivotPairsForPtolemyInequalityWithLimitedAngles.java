/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.metricSpace.distance.bounding.twopivots.learning;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.metricSpace.distance.bounding.twopivots.impl.DataDependentGeneralisedPtolemaicFiltering;
import vm.objTransforms.storeLearned.PivotPairsStoreInterface;
import vm.search.algorithm.SearchingAlgorithm;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class LearningPivotPairsForPtolemyInequalityWithLimitedAngles<T> {

    public static final Logger LOG = Logger.getLogger(LearningPivotPairsForPtolemyInequalityWithLimitedAngles.class.getName());
    public static final Boolean BREAK_AFTER_FIRST_LB = true;

    private final AbstractMetricSpace<T> metricSpace;
    private final DistanceFunctionInterface<T> df;
    private final List<Object> pivots;
    private final List<Object> sampleObjects;
    private final List<Object> sampleQueries;
    private final DataDependentGeneralisedPtolemaicFiltering filter;
    private final Integer K = 5;
    private final PivotPairsStoreInterface<T> storage;
    private final String datasetName;

    public LearningPivotPairsForPtolemyInequalityWithLimitedAngles(AbstractMetricSpace<T> metricSpace, DistanceFunctionInterface<T> df, List<Object> pivots, List<Object> sampleObjectsAndQueries, int objectsSampleCount, int queriesSampleCount, int numberOfSmallestDistsUsedForLearning, DataDependentGeneralisedPtolemaicFiltering filter, String datasetName, PivotPairsStoreInterface storage) {
        this.metricSpace = metricSpace;
        this.df = df;
        this.pivots = pivots;
        this.sampleQueries = Tools.getAndRemoveFirst(sampleObjectsAndQueries, queriesSampleCount);
        this.sampleObjects = Tools.getAndRemoveFirst(sampleObjectsAndQueries, objectsSampleCount);
        this.filter = filter;
        this.storage = storage;
        this.datasetName = datasetName;
    }

    public void execute() throws InterruptedException {
        Map<Object, Object> oMap = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(metricSpace, sampleObjects, true);
        Object[] sampleQueryArray = ToolsMetricDomain.getData(sampleQueries.toArray(), metricSpace);
        Map<String, Integer> sumsForQueries = new HashMap<>();
        ExecutorService threadPool = vm.javatools.Tools.initExecutor();
        threadPool = Executors.newFixedThreadPool(1);
        CountDownLatch latch = new CountDownLatch(sampleQueryArray.length);

        for (int i = 0; i < sampleQueryArray.length; i++) {
            int iFinal = i;
            threadPool.execute(() -> {
                long time = -System.currentTimeMillis();
                Object qData = sampleQueryArray[iFinal];
                Map<String, Integer> evaluatedQuery = evaluateQuery((T) qData, oMap);
                for (String key : evaluatedQuery.keySet()) {
                    int value = evaluatedQuery.get(key);
                    if (sumsForQueries.containsKey(key)) {
                        int value2 = sumsForQueries.get(key);
                        sumsForQueries.put(key, value + value2);
                    } else {
                        sumsForQueries.put(key, value);
                    }
                }
                time += System.currentTimeMillis();
                LOG.log(Level.INFO, "Processed query {0} in {1} ms", new Object[]{iFinal, time});
                latch.countDown();
            });
        }
        latch.await();
        TreeSet<Map.Entry<String, Integer>> ret = new TreeSet<>(new Tools.MapByValueIntComparator<>());
        for (Map.Entry<String, Integer> entry : sumsForQueries.entrySet()) {
            ret.add(entry);
        }
        Map<Object, Object> pMap = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(metricSpace, pivots, false);
        List<Object> retList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : ret) {
            String[] split = entry.getKey().split("-");
            retList.add(pMap.get(split[0]));
            retList.add(pMap.get(split[1]));
        }
        String name = datasetName + "_" + pivots.size() + "p_" + sampleQueryArray.length + "q_" + sampleObjects.size() + "o_" + K + "k";
        if (BREAK_AFTER_FIRST_LB) {
            name += "_dependent";
        }
        storage.storePivotPairs(name, metricSpace, retList);
    }

    private Map<String, Integer> evaluateQuery(T qData, Map<Object, Object> oMap) {
        TreeSet<Map.Entry<Object, Float>> queryAnswer = new TreeSet<>(new Tools.MapByFloatValueComparator());
        Map<String, Integer> ret = new HashMap<>();

        Object[] pivotsData = ToolsMetricDomain.getData(pivots.toArray(), metricSpace);

        float[] qpDists = new float[pivots.size()];
        for (int i = 0; i < pivots.size(); i++) {
            T pData = (T) pivotsData[i];
            qpDists[i] = df.getDistance(qData, pData);
        }

        float[][] qp1p2MultipliedByCoef = computeqpDistMultipliedByCoefForPivots(qpDists, pivotsData);
        float range = 0;
        int counter = oMap.size();
        oLoop:
        for (Map.Entry<Object, Object> o : oMap.entrySet()) {
            int discarded = 0;
            T oData = (T) o.getValue();
            if (queryAnswer.size() < K) {
                float distance = df.getDistance((T) qData, oData);
                queryAnswer.add(new AbstractMap.SimpleEntry<>(o.getKey(), distance));
                range = SearchingAlgorithm.adjustAndReturnSearchRadiusAfterAddingOne(queryAnswer, K);
                counter--;
                continue;
            }
            int checkedLB = 0;
            for (int p1Idx = 0; p1Idx < pivots.size() - 1; p1Idx++) {
                Object p1 = pivots.get(p1Idx);
                T p1Data = metricSpace.getDataOfMetricObject(p1);
                Object p1ID = metricSpace.getIDOfMetricObject(p1);
                float p1O = df.getDistance(p1Data, oData);
                for (int p2Idx = p1Idx + 1; p2Idx < pivots.size(); p2Idx++) {
                    Object p2 = pivots.get(p2Idx);
                    T p2Data = metricSpace.getDataOfMetricObject(p2);
                    Object p2ID = metricSpace.getIDOfMetricObject(p2);
                    float p2O = df.getDistance(p2Data, oData);
                    float lb = filter.lowerBound(p2O, qp1p2MultipliedByCoef[p1Idx][p2Idx], p1O, qp1p2MultipliedByCoef[p2Idx][p1Idx]);
                    checkedLB++;
                    if (lb >= range) {
                        discarded++;
                        String key = p1ID.toString() + "-" + p2ID.toString();
                        if (!ret.containsKey(key)) {
                            ret.put(key, 1);
                        } else {
                            Integer value = ret.get(key);
                            value++;
                            ret.put(key, value);
                        }
                        if (BREAK_AFTER_FIRST_LB) {
                            continue oLoop;
                        }
                    }
                }
            }
            if (discarded == 0) {
                float distance = df.getDistance(qData, oData);
                queryAnswer.add(new AbstractMap.SimpleEntry<>(o.getKey(), distance));
                range = SearchingAlgorithm.adjustAndReturnSearchRadiusAfterAddingOne(queryAnswer, K);
            }
            if (!BREAK_AFTER_FIRST_LB) {
                counter--;
                LOG.log(Level.INFO, "Remains {0} objects. Previous discarded by {1} lower bounds out of {2}", new Object[]{counter, discarded, checkedLB});
            }
        }
        return ret;
    }

    private float[][] computeqpDistMultipliedByCoefForPivots(float[] qpDists, Object[] pivotsData) {
        float[][] ret = new float[pivotsData.length][pivotsData.length];
        for (int i = 0; i < pivotsData.length; i++) {
            float qp1Dist = qpDists[i];
            for (int j = 0; j < pivotsData.length; j++) {
                float qp2Dist = qpDists[j];
                ret[i][j] = qp1Dist * filter.getCoefPivotPivotForLB(i, j);
                ret[j][i] = qp2Dist * filter.getCoefPivotPivotForLB(j, i);
            }
        }
        return ret;
    }

}
