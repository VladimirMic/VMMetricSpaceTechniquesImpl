package vm.searchSpace.distance.impl;

import vm.searchSpace.distance.AbstractDistanceFunction;

/**
 *
 * @author Vlada
 */
public class L1OnShortsArray extends AbstractDistanceFunction<short[]> {

    @Override
    public float getDistance(short[] obj1, short[] obj2) {
        if (obj2.length != obj1.length) {
            throw new RuntimeException("Cannot compute distance on different vector dimensions (" + obj1.length + ", " + obj2.length + ")");
        }
        float sum = 0;
        for (int i = 0; i < obj1.length; i++) {
            sum += Math.abs(obj1[i] - obj2[i]);
        }
        return sum;
    }

    @Override
    public String getName() {
        return "Mahnattan distance";
    }

}
