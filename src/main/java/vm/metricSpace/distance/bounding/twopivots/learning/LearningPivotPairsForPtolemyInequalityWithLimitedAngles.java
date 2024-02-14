/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.metricSpace.distance.bounding.twopivots.learning;

import java.util.AbstractMap;
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
import static vm.metricSpace.distance.bounding.twopivots.impl.DataDependentGeneralisedPtolemaicFiltering.CONSTANT_FOR_PRECISION;
import vm.metricSpace.distance.bounding.twopivots.storeLearned.PtolemyInequalityWithLimitedAnglesCoefsStoreInterface;
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

    public Map<Object, float[]> execute() {
        TreeSet<AbstractMap.Entry<String[], Integer>> filteredCounts = new TreeSet<>(new Tools.MapByValueComparatorWithOwnKeyComparator(new Tools.ObjectArrayIdentityComparator()));
        Map<Object, Object> oMap = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(metricSpace, sampleObjects, false);
        Object[] sampleQueryArray = ToolsMetricDomain.getData(sampleQueries.toArray(), metricSpace);
        for (Object qData : sampleQueryArray) {
            evaluateQuery((T) qData, oMap, filteredCounts);
        }
    }
    //       ulozit vysledky - nejlepsi dvojice pivotu

    private void evaluateQuery(T qData, Map<Object, Object> oMap, TreeSet<Map.Entry<String[], Integer>> filteredCounts) {
        TreeSet<Map.Entry<Object, Float>> answer = new TreeSet<>(new Tools.MapByValueComparator());
        float range = 0;
        for (Map.Entry<Object, Object> o : oMap.entrySet()) {
            T oData = (T) o.getValue();
            if (answer.size() < K) {
                float distance = df.getDistance((T) qData, oData);
                answer.add(new AbstractMap.SimpleEntry<>(o.getKey(), distance));
                range = SearchingAlgorithm.adjustAndReturnSearchRadiusAfterAddingOne(answer, K);
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
                    float coef = filter.getCoefPivotPivotForLB(p1Idx, p2Idx);
                    float p2O = df.getDistance(p2Data, oData);
                }

            }
        }
    }

}
