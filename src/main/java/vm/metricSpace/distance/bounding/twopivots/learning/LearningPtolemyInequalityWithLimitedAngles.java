package vm.metricSpace.distance.bounding.twopivots.learning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.distEstimation.limitedAngles.foursome.ToolsPtolemaionsLikeCoefs;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.distance.DistanceFunctionInterface;
import static vm.metricSpace.distance.bounding.twopivots.impl.PtolemaiosFilteringWithLimitedAnglesSimpleCoef.CONSTANT_FOR_PRECISION;
import vm.metricSpace.distance.bounding.twopivots.storeLearned.PtolemyInequalityWithLimitedAnglesCoefsStoreInterface2;

/**
 *
 * @author xmic
 * @param <T>
 */
public class LearningPtolemyInequalityWithLimitedAngles<T> {

    public static final Logger LOG = Logger.getLogger(LearningPtolemyInequalityWithLimitedAngles.class.getName());
    public static final Float RATIO_OF_SMALLEST_DISTS = 0.4f / 100f;

    private final String resultName;
    private final AbstractMetricSpace<T> metricSpace;
    private final DistanceFunctionInterface<T> df;
    private final List<Object> pivots;
    private final List<Object> sampleObjectsAndQueries;
    private final PtolemyInequalityWithLimitedAnglesCoefsStoreInterface2 storage;
    private final TreeSet<Map.Entry<String, Float>> smallDistsOfSampleObjectsAndQueries;

    public LearningPtolemyInequalityWithLimitedAngles(AbstractMetricSpace<T> metricSpace, DistanceFunctionInterface<T> df, List<Object> pivots, List<Object> sampleObjectsAndQueries, TreeSet<Map.Entry<String, Float>> smallDistsOfSampleObjectsAndQueries, PtolemyInequalityWithLimitedAnglesCoefsStoreInterface2 storage, String datasetName) {
        this.metricSpace = metricSpace;
        this.df = df;
        this.pivots = pivots;
        this.sampleObjectsAndQueries = sampleObjectsAndQueries;
        this.storage = storage;
        this.smallDistsOfSampleObjectsAndQueries = smallDistsOfSampleObjectsAndQueries;
        this.resultName = storage.getResultDescription(datasetName, smallDistsOfSampleObjectsAndQueries.size(), pivots.size(), 0);
    }

    public Map<Object, float[]> execute() {
        Map<Object, float[]> results = new HashMap<>();
        Map<Object, Object> metricObjectsAsIdObjectMap = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(metricSpace, sampleObjectsAndQueries, false);
        for (int p = 0; p < pivots.size(); p++) {
            List<Float> fractionSums = new ArrayList<>();
            List<Float> fractionDiffs = new ArrayList<>();
            Object[] fourObjects = new Object[4];
            fourObjects[0] = pivots.get(p);
            fourObjects[1] = pivots.get((p + 1) % pivots.size());
            for (Map.Entry<String, Float> smallDist : smallDistsOfSampleObjectsAndQueries) {
                String[] qoIDs = smallDist.getKey().split(";");
                fourObjects[2] = metricObjectsAsIdObjectMap.get(qoIDs[0]);
                fourObjects[3] = metricObjectsAsIdObjectMap.get(qoIDs[1]);
                Object[] fourObjectsData = ToolsMetricDomain.getData(fourObjects, metricSpace);
                float[] sixDists = ToolsPtolemaionsLikeCoefs.getPairwiseDistsOfFourObjects(df, true, fourObjectsData);
                if (sixDists == null || Tools.isZeroInArray(sixDists)) {
                    continue;
                }
                float ac = Math.abs(sixDists[0] * sixDists[2]);
                float ef = Math.abs(sixDists[4] * sixDists[5]);
                float bd = Math.abs(sixDists[1] * sixDists[3]);
                float fractionSum = CONSTANT_FOR_PRECISION * ac / (bd + ef);
                float fractionDiff = ac / Math.abs(bd - ef);
                fractionSums.add(fractionSum);
                fractionDiffs.add(fractionDiff);
            }
            Collections.sort(fractionSums);
            Collections.sort(fractionDiffs);
            String pivotPairsID = metricSpace.getIDOfMetricObject(fourObjects[0]) + "-" + metricSpace.getIDOfMetricObject(fourObjects[1]);
            results.put(pivotPairsID, new float[]{fractionSums.get(0), fractionSums.get(fractionSums.size() - 1), fractionDiffs.get(0), fractionDiffs.get(fractionDiffs.size() - 1)});
            LOG.log(Level.INFO, "Evaluated coefs for pivot pair {0} ({1}, {2}, {3}, {4})", new Object[]{p, fractionSums.get(0), fractionSums.get(fractionSums.size() - 1), fractionDiffs.get(0), fractionDiffs.get(fractionDiffs.size() - 1)});
        }
        storage.storeCoefficients(results, resultName);
        return results;
    }

}
