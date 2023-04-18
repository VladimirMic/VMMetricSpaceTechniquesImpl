package vm.simRel.impl;

import vm.datatools.Tools;

/**
 *
 * @author Vlada
 */
public class SimRelEuclideanPCAImplForTesting extends SimRelEuclideanPCAImpl {

    private final int prefixLength;

    public SimRelEuclideanPCAImplForTesting(float[] diffThresholdsPerCoords, int prefixLength) {
        super(diffThresholdsPerCoords);;
        this.prefixLength = prefixLength > 0 ? prefixLength : Integer.MAX_VALUE;
    }

    public SimRelEuclideanPCAImplForTesting(float[] diffThresholdsPerCoords) {
        this(diffThresholdsPerCoords, Integer.MAX_VALUE);
    }

    @Override
    public short getMoreSimilar(float[] q, float[] o1, float[] o2) {
//        L2OnFloatsArray df = new L2OnFloatsArray();
//        float d1 = df.getDistance(q, o1);
//        float d2 = df.getDistance(q, o2);
        float diffQO1 = 0;
        float diffQO2 = 0;
        for (int i = 0; i < Math.min(q.length, prefixLength); i++) {
            diffQO1 += (q[i] - o1[i]) * (q[i] - o1[i]);
            diffQO2 += (q[i] - o2[i]) * (q[i] - o2[i]);
            if (Math.abs(diffQO1 - diffQO2) > diffThresholdsPerCoords[i]) {
                earlyStopsOnCoordsCounts[i]++;
                return Tools.booleanToShort(diffQO1 < diffQO2, 1, 2);
            }
        }
        earlyStopsOnCoordsCounts[Math.min(q.length, prefixLength)]++;
        return 0;
    }

}
