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
    public float lowerBound(float distP1P2, float distP2O, float distP1Q, float distP1O, float distP2Q, int p1Idx, int p2Idx, Float range) {
        return Math.abs(distP1O * distP2Q - distP2O * distP1Q) / distP1P2;
    }

    @Override
    public float upperBound(float distP1P2, float distP2O, float distP1Q, float distP1O, float distP2Q, int p1Idx, int p2Idx, Float range) {
        return (distP1O * distP2Q + distP2O * distP1Q) / distP1P2;
    }

    @Override
    public String getTechName() {
        return "ptolemaios";
    }

    @Override
    public boolean isPivotPairValid(int p1Idx, int p2Idx) {
        return true;
    }

}
