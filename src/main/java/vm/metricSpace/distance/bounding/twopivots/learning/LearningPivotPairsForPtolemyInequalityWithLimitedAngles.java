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
import static vm.metricSpace.distance.bounding.twopivots.impl.DataDependentGeneralisedPtolemaicFiltering.CONSTANT_FOR_PRECISION;
import vm.metricSpace.distance.bounding.twopivots.storeLearned.PtolemyInequalityWithLimitedAnglesCoefsStoreInterface;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class LearningPivotPairsForPtolemyInequalityWithLimitedAngles<T> {

    public static final Logger LOG = Logger.getLogger(LearningPivotPairsForPtolemyInequalityWithLimitedAngles.class.getName());

    private final String coefsFile;
    private final AbstractMetricSpace<T> metricSpace;
    private final DistanceFunctionInterface<T> df;
    private final List<Object> pivots;
    private final List<Object> sampleObjects;
    private final List<Object> sampleQueries;

    public LearningPivotPairsForPtolemyInequalityWithLimitedAngles(AbstractMetricSpace<T> metricSpace, DistanceFunctionInterface<T> df, List<Object> pivots, List<Object> sampleObjectsAndQueries, int objectsSampleCount, int queriesSampleCount, int numberOfSmallestDistsUsedForLearning, PtolemyInequalityWithLimitedAnglesCoefsStoreInterface storage, String datasetName) {
        this.metricSpace = metricSpace;
        this.df = df;
        this.pivots = pivots;
        this.sampleQueries = Tools.getAndRemoveFirst(sampleObjectsAndQueries, queriesSampleCount);
        this.sampleObjects = Tools.getAndRemoveFirst(sampleObjectsAndQueries, objectsSampleCount);
        this.coefsFile = storage.getResultDescription(datasetName, numberOfSmallestDistsUsedForLearning, objectsSampleCount, queriesSampleCount, pivots.size(), true);
    }

    public Map<Object, float[]> execute() {
        TreeSet<AbstractMap.Entry<String[], Integer>> filteredCounts = new TreeSet<>(new Tools.MapByValueComparatorWithOwnKeyComparator(new Tools.ObjectArrayIdentityComparator()));
        Object[] sampleDataArray = ToolsMetricDomain.getData(sampleObjects.toArray(), metricSpace);

        for (Object sampleData : sampleDataArray) {

        }
        for (int p1Idx = 0; p1Idx < pivots.size() - 1; p1Idx++) {
            Object p1 = pivots.get(p1Idx);
            T p1Data = metricSpace.getDataOfMetricObject(p1);

            for (int p2 = p1Idx + 1; p2 < pivots.size(); p2++) {
                fourObjects[1] = pivots.get(p2);
                fourObjectsData[1] = metricSpace.getDataOfMetricObject(fourObjects[1]);
                float[] extremes = learnForPivots(fourObjects, fourObjectsData, metricObjectsAsIdObjectMap);
                synchronized (LearningCoefsForPtolemyInequalityWithLimitedAngles.class) {
                    String pivotPairsID = metricSpace.getIDOfMetricObject(fourObjects[0]) + "-" + metricSpace.getIDOfMetricObject(fourObjects[1]);
                    results.put(pivotPairsID, extremes);
                    LOG.log(Level.INFO, "Evaluated coefs for pivot pairs {0} with the starting pivot {6}. Results: {1}; {2}; {3}; {4}. Notice first two numbers multiplied by {5} for a sake of numerical precision.", new Object[]{pivotPairsID, extremes[0], extremes[1], extremes[2], extremes[3], CONSTANT_FOR_PRECISION, p1Idx});
                }
            }
        }
        LOG.log(Level.INFO, "Remains {0} primary pivots to check. Results size: {1}", new Object[]{latch.getCount(), results.size()});
    }
    //       storage.storeCoefficients(results, resultName);
    return results ;
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
            float c = Math.abs(sixDists[2]);
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

}
