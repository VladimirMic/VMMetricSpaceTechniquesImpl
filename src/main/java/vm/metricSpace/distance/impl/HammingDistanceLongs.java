package vm.metricSpace.distance.impl;

import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 * Class evaluates the Hamming distance on arrays of longs. They do not have to
 * have the same size.
 *
 * @author Vladimir Mic, Masaryk University, Brno, Czech Republic,
 * xmic@fi.muni.cz
 */
public class HammingDistanceLongs extends DistanceFunctionInterface<long[]> {

    @Override
    public float getDistance(long[] o1, long[] o2) {
        int minLength = Math.min(o1.length, o2.length);
        int ret = 0;
        int i;
        for (i = 0; i < minLength; i++) {
            long xor = o1[i] ^ o2[i];
            ret += Long.bitCount(xor);
        }
        if (o1.length < o2.length) {
            for (; i < o2.length; i++) {
                ret += Long.bitCount(o2[i]);
            }
        } else if (o1.length > o2.length) {
            for (; i < o1.length; i++) {
                ret += Long.bitCount(o1[i]);
            }
        }
        return ret;
    }

}
