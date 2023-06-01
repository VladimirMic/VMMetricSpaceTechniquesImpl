package vm.metricSpace.distance.bounding.twopivots.impl;

import vm.metricSpace.distance.bounding.twopivots.TwoPivotsFilter;

/**
 *
 * @author Vlada
 */
public class PtolemaiosFiltering extends TwoPivotsFilter {

    public PtolemaiosFiltering(String resultNamePrefix) {
        super(resultNamePrefix);
    }

    @Override
    public float lowerBound(float distP1P2, float distP2O, float distQP1, float distP1O, float distP2Q, String p1ID, String p2ID) {
        return Math.abs(distP1O * distP2Q - distP2O * distQP1) / distP1P2;
    }

    @Override
    public float upperBound(float distP1P2, float distP2O, float distQP1, float distP1O, float distP2Q, String p1ID, String p2ID) {
        return (distP1O * distP2Q + distP2O * distQP1) / distP1P2;
    }

    @Override
    public String getTechName() {
        return "ptolemaios";
    }

}
