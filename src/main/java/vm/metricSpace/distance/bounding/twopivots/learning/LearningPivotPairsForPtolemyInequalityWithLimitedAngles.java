/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.metricSpace.distance.bounding.twopivots.learning;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.metricSpace.distance.bounding.twopivots.impl.DataDependentGeneralisedPtolemaicFiltering;
import vm.search.algorithm.SearchingAlgorithm;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class LearningPivotPairsForPtolemyInequalityWithLimitedAngles<T> {

    public static final Logger LOG = Logger.getLogger(LearningPivotPairsForPtolemyInequalityWithLimitedAngles.class.getName());

    private final AbstractMetricSpace<T> metricSpace;
    private final DistanceFunctionInterface<T> df;
    private final List<Object> pivots;
    private final List<Object> sampleObjects;
    private final List<Object> sampleQueries;
    private final DataDependentGeneralisedPtolemaicFiltering filter;
    private final Integer K = 30;

    public LearningPivotPairsForPtolemyInequalityWithLimitedAngles(AbstractMetricSpace<T> metricSpace, DistanceFunctionInterface<T> df, List<Object> pivots, List<Object> sampleObjectsAndQueries, int objectsSampleCount, int queriesSampleCount, int numberOfSmallestDistsUsedForLearning, DataDependentGeneralisedPtolemaicFiltering filter, String datasetName) {
        this.metricSpace = metricSpace;
        this.df = df;
        this.pivots = pivots;
        this.sampleQueries = Tools.getAndRemoveFirst(sampleObjectsAndQueries, queriesSampleCount);
        this.sampleObjects = Tools.getAndRemoveFirst(sampleObjectsAndQueries, objectsSampleCount);
        this.filter = filter;
    }

    public TreeSet<Map.Entry<String, Integer>> execute() {
        Map<Object, Object> oMap = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(metricSpace, sampleObjects, true);
        Object[] sampleQueryArray = ToolsMetricDomain.getData(sampleQueries.toArray(), metricSpace);
        Map<String, Integer> sumsForQueries = new HashMap<>();
        for (Object qData : sampleQueryArray) {
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
        }
        TreeSet<Map.Entry<String, Integer>> ret = new TreeSet<>(new Tools.MapByValueComparator());
        for (Map.Entry<String, Integer> entry : sumsForQueries.entrySet()) {
            ret.add(entry);
        }
        return ret;
    }

    private Map<String, Integer> evaluateQuery(T qData, Map<Object, Object> oMap) {
        TreeSet<Map.Entry<Object, Float>> queryAnswer = new TreeSet<>(new Tools.MapByValueComparator());
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
                    }
                }
            }
            if (discarded == 0) {
                float distance = df.getDistance(qData, oData);
                queryAnswer.add(new AbstractMap.SimpleEntry<>(o.getKey(), distance));
                range = SearchingAlgorithm.adjustAndReturnSearchRadiusAfterAddingOne(queryAnswer, K);
            }
            counter--;
            LOG.log(Level.INFO, "Remains {0} objects", counter);
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
