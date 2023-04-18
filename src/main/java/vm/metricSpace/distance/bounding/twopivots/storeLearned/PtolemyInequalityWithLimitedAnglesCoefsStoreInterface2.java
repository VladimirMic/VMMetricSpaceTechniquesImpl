package vm.metricSpace.distance.bounding.twopivots.storeLearned;

import java.util.Map;

/**
 *
 * @author xmic
 */
public interface PtolemyInequalityWithLimitedAnglesCoefsStoreInterface2 {
    
    public void storeCoefficients(Map<Object, float[]> results, String resultName);

    public String getResultDescription(String datasetName, int numberOfTetrahedrons, int pivotPairs, float ratioOfSmallestDists);

}
