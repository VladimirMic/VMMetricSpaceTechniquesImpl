/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.searchSpace.distance.impl;

import vm.searchSpace.distance.AbstractDistanceFunction;

/**
 *
 * @author au734419
 */
public class L2OnLongsArray extends AbstractDistanceFunction<long[]> {

    @Override
    public float getDistance(long[] o1, long[] o2) {
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

    @Override
    public String getName() {
        return "Euclidean distance";
    }

}
