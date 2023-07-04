package vm.metricSpace.distance.impl;

import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author Vlada
 */
public class L2OnFloatsArray extends DistanceFunctionInterface<float[]> {

    @Override
    public float getDistance(float[] o1, float[] o2) {
        if (o2.length != o1.length) {
            throw new IllegalArgumentException("Cannot compute distance on different vector dimensions (" + o1.length + ", " + o2.length + ")");
        }
        float powSum = 0;
        for (int i = 0; i < o1.length; i++) {
            float dif = (o1[i] - o2[i]);
            powSum += dif * dif;
        }
        return (float) Math.sqrt(powSum);
    }

}
