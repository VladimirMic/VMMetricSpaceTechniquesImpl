package vm.metricSpace.distance.bounding.twopivots.storeLearned;

import vm.structures.ConvexHull2DEuclid;

/**
 *
 * @author Vlada
 */
public interface PtolemyInequalityWithLimitedAnglesHullsStoreInterface {

    public void storeHull(String outputPath, String hullID, ConvexHull2DEuclid hullsForPivotPair);

    public String getResultDescription(String datasetName, int numberOfTetrahedrons, int pivotPairs, float ratioOfSmallestDists);

}
