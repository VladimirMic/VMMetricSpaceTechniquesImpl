package vm.searchSpace.distance.impl;

import vm.searchSpace.distance.AbstractDistanceFunction;

/**
 *
 * @author xmic
 */
public class DotProduct extends AbstractDistanceFunction<float[]> {

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

    @Override
    public String getName() {
        return "Dot product distance";
    }

}
