package vm.metricSpace.distance.impl;

import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author Vlada
 */
public class L1OnIntsArray extends DistanceFunctionInterface<int[]> {

    @Override
    public float getDistance(int[] obj1, int[] obj2) {
        if (obj2.length != obj1.length) {
            throw new RuntimeException("Cannot compute distance on different vector dimensions (" + obj1.length + ", " + obj2.length + ")");
        }
        float sum = 0;
        for (int i = 0; i < obj1.length; i++) {
            sum += Math.abs(obj1[i] - obj2[i]);
        }
        return sum;
    }

}
