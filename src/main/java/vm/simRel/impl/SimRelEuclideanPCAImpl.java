package vm.simRel.impl;

import java.util.Arrays;
import vm.datatools.Tools;
import vm.simRel.SimRelInterface;

/**
 * Pure implementation of the simRel
 *
 * @author xmic
 */
public class SimRelEuclideanPCAImpl implements SimRelInterface<float[]> {

    protected final float[] diffThresholdsPerCoords;
    protected final long[] earlyStopsOnCoordsCounts;

    public SimRelEuclideanPCAImpl(float[] diffThresholdsPerCoords) {
        this.diffThresholdsPerCoords = diffThresholdsPerCoords;
        earlyStopsOnCoordsCounts = new long[diffThresholdsPerCoords.length + 1];
    }

    @Override
    public short getMoreSimilar(float[] q, float[] o1, float[] o2) {
        float diffQO1 = 0;
        float diffQO2 = 0;
        for (int i = 0; i < q.length; i++) {
            diffQO1 += (q[i] - o1[i]) * (q[i] - o1[i]);
            diffQO2 += (q[i] - o2[i]) * (q[i] - o2[i]);
            if (Math.abs(diffQO1 - diffQO2) > diffThresholdsPerCoords[i]) {
                earlyStopsOnCoordsCounts[i]++;
                return Tools.booleanToShort(diffQO1 < diffQO2, 1, 2);
            }
        }
        earlyStopsOnCoordsCounts[q.length]++;
        return 0;
    }

    public long[] getEarlyStopsOnCoordsCounts() {
        return earlyStopsOnCoordsCounts;
    }

    public final void resetEarlyStopsOnCoordsCounts() {
        Arrays.fill(earlyStopsOnCoordsCounts, 0);
    }

    public final float getTOmega(int omega) {
        return diffThresholdsPerCoords[omega];
    }

}
