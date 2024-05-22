package vm.metricSpace.distance.bounding.onepivot.learning;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.DatasetOfCandidates;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.metricSpace.distance.bounding.onepivot.storeLearned.TriangleInequalityWithLimitedAnglesCoefsStoreInterface;
import vm.metricSpace.distance.storedPrecomputedDistances.PrecomputedPairsOfDistancesStoreInterface;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class LearningTriangleInequalityWithLimitedAngles<T> {

    public static final Logger LOG = Logger.getLogger(LearningTriangleInequalityWithLimitedAngles.class.getName());
    public static final Float RATIO_OF_SMALLEST_DISTS = 0.4f / 100f;

    private final Dataset dataset;
    private final String resultName;
    private final TriangleInequalityWithLimitedAnglesCoefsStoreInterface storage;
    private final Integer pCount;
    private final Integer oCount;
    private final Integer qCount;
    private final PrecomputedPairsOfDistancesStoreInterface storageOfSmallDists;

    public LearningTriangleInequalityWithLimitedAngles(Dataset dataset, PrecomputedPairsOfDistancesStoreInterface storageOfSmallDists, int pivotCount, Integer oCount, Integer qCount, TriangleInequalityWithLimitedAnglesCoefsStoreInterface storage, String resultName) {
        this.dataset = dataset;
        this.storage = storage;
        this.resultName = resultName;
        this.pCount = pivotCount;
        this.oCount = oCount;
        this.qCount = qCount;
        this.storageOfSmallDists = storageOfSmallDists;
    }

    public Map<Object, Float> execute() {
        TreeSet<Map.Entry<String, Float>> dists = storageOfSmallDists.loadPrecomputedDistances();
        int lastIndex = dists.size();
        Map<Object, Float> ret = new HashMap<>();
        AbstractMetricSpace<T> metricSpace = dataset.getMetricSpace();
        List pivots = dataset.getPivots(pCount);
        Map<Object, T> oMap = null;
        if (!(dataset instanceof DatasetOfCandidates)) {
            oMap = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(metricSpace, dataset.getMetricObjectsFromDataset(), true);
        }
        DistanceFunctionInterface df = dataset.getDistanceFunction();

        for (Object pivot : pivots) {
            T pivotData = metricSpace.getDataOfMetricObject(pivot);
            Iterator<Map.Entry<String, Float>> it = dists.iterator();
            float coefForP = Float.MAX_VALUE;
            Map<Object, T> cache = new HashMap<>();
            for (int i = 0; i < lastIndex && it.hasNext(); i++) {
                Map.Entry<String, Float> dist = it.next();
                String[] oqIDs = dist.getKey().split(";");
                T qData;
                T oData;
                if (oMap == null) {
                    if (cache.containsKey(oqIDs[1])) {
                        qData = cache.get(oqIDs[1]);
                    } else {
                        qData = metricSpace.getDataOfMetricObject(oqIDs[1]);
                        cache.put(oqIDs[1], qData);
                    }
                    if (cache.containsKey(oqIDs[0])) {
                        oData = cache.get(oqIDs[0]);
                    } else {
                        oData = metricSpace.getDataOfMetricObject(oqIDs[0]);
                        cache.put(oqIDs[0], oData);
                    }
                } else {
                    qData = (T) oMap.get(oqIDs[1]);
                    oData = (T) oMap.get(oqIDs[0]);
                }
                float c = Math.max(0, (float) dist.getValue());
                float dPO = df.getDistance(pivotData, oData);
                float dPQ = df.getDistance(pivotData, qData);
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
        String description = storage.getResultDescription(resultName, pivots.size(), oCount, qCount, RATIO_OF_SMALLEST_DISTS);
        storage.storeCoefficients(ret, description);
        return ret;
    }

}
