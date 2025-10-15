/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.searchSpace.distance.impl;

import vm.searchSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author au734419
 */
public class BrayCurtisOnFloatsArray extends DistanceFunctionInterface<float[]> {

    @Override
    public float getDistance(float[] obj1, float[] obj2) {
        float num = 0;
        float denom = 0;
        for (int i = 0; i < obj1.length; i++) {
            num += obj1[i] - obj2[i];
            denom += obj1[i] + obj2[i];
        }
        return num / denom;
    }

}
