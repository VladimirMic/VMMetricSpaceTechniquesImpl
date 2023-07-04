package vm.metricSpace.distance.impl;

import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author xmic
 */
public class DotProduct extends DistanceFunctionInterface<float[]> {

    @Override
    public float getDistance(float[] o1, float[] o2) {
        if (o1 == o2) {
            return 0;
        }
        if (o2.length != o1.length) {
            throw new IllegalArgumentException("Cannot compute distance on different vector dimensions (" + o1.length + ", " + o2.length + ")");
        }
        float ret = 0;
        for (int i = 0; i < o1.length; i++) {
            ret += o1[i] * o2[i];
        }
        return 1 - ret;
    }

}
