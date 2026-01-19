package vm.searchSpace.distance.bounding.onepivot.learning;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.Dataset;
import vm.searchSpace.ToolsSpaceDomain;
import vm.searchSpace.distance.AbstractDistanceFunction;
import vm.searchSpace.distance.bounding.onepivot.storeLearned.TriangleInequalityWithLimitedAnglesCoefsStoreInterface;
import vm.searchSpace.distance.storedPrecomputedDistances.AbstractPrecomputedPairsOfDistancesStorage;

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
    private final AbstractPrecomputedPairsOfDistancesStorage storageOfSmallDists;

    public LearningTriangleInequalityWithLimitedAngles(Dataset dataset, AbstractPrecomputedPairsOfDistancesStorage storageOfSmallDists, int pivotCount, Integer oCount, Integer qCount, TriangleInequalityWithLimitedAnglesCoefsStoreInterface storage, String resultName) {
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
        if (dists == null) {
            return null;
        }
        int lastIndex = dists.size();
        Map<Object, Float> ret = new HashMap<>();
        AbstractSearchSpace<T> searchSpace = dataset.getSearchSpace();
        List pivots = dataset.getPivots(pCount);
        TreeSet<Map.Entry<String, Float>> smallDistsOfSampleObjectsAndQueries = storageOfSmallDists.loadPrecomputedDistances();
        Set<Comparable> setOfIDs = AbstractPrecomputedPairsOfDistancesStorage.getIDsOfObjects(smallDistsOfSampleObjectsAndQueries);
        List objectsWithSmallestDists = ToolsSpaceDomain.getObjectsForIDs(setOfIDs, dataset);
        Map<Object, T> oMap = ToolsSpaceDomain.getSearchObjectsAsIdDataMap(searchSpace, objectsWithSmallestDists);
        AbstractDistanceFunction df = dataset.getDistanceFunction();

        for (Object pivot : pivots) {
            T pivotData = searchSpace.getDataOfObject(pivot);
            Iterator<Map.Entry<String, Float>> it = dists.iterator();
            float coefForP = Float.MAX_VALUE;
            for (int i = 0; i < lastIndex && it.hasNext(); i++) {
                Map.Entry<String, Float> dist = it.next();
                String[] oqIDs = dist.getKey().split(";");
                T qData = (T) oMap.get(oqIDs[1]);
                T oData = (T) oMap.get(oqIDs[0]);
                float c = Math.max(0, (float) dist.getValue());
                float dPO = df.getDistance(pivotData, oData);
                float dPQ = df.getDistance(pivotData, qData);
                if (dPO == 0 || dPQ == 0 || c == 0) {
                    continue;
                }
                float coef = c / Math.abs(dPO - dPQ);
                coefForP = Math.min(coefForP, coef);
            }
            Object pivotID = searchSpace.getIDOfObject(pivot);
            ret.put(pivotID, coefForP);
            LOG.log(Level.INFO, "Evaluated pivot coef {0} for pivot {1}", new Object[]{coefForP, pivotID.toString()});
        }
        String description = storage.getResultDescription(resultName, pivots.size(), oCount, qCount, RATIO_OF_SMALLEST_DISTS);
        storage.storeCoefficients(ret, description);
        return ret;
    }

}
