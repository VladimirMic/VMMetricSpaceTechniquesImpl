package vm.metricSpace.distance.bounding.onepivot.storeLearned;

import java.util.Map;

/**
 *
 * @author Vlada
 */
public interface TriangleInequalityWithLimitedAnglesCoefsStoreInterface {

    public void storeCoefficients(Map<Object, Float> results, String resultName);

    public String getResultDescription(String datasetName, int pivotSize, int sampleSetSize, int queriesSampleSize, float ratioOfSmallestDists);
}
