package vm.searchSpace.distance.impl;

import vm.searchSpace.distance.AbstractDistanceFunction;

/**
 *
 * @author au734419
 */
public class BrayCurtisOnFloatsArray extends AbstractDistanceFunction<float[]> {

    @Override
    public float getDistance(float[] obj1, float[] obj2) {
        float num = 0;
        float denom = 0;
        for (int i = 0; i < obj1.length; i++) {
            num += Math.abs(obj1[i] - obj2[i]);
            denom += Math.abs(obj1[i] + obj2[i]);
        }
        return num / denom;
    }

    @Override
    public String getName() {
        return "Bray Curtis distance";
    }

}
