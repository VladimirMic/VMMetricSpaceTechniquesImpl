/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.metricSpace.distance.impl;

import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author Vlada
 */
public class AngularDistance extends DistanceFunctionInterface<float[]> {

    private float[] lastO1 = null;
    private float[] lastO2 = null;
    private float lastO1Norm = 0;
    private float lastO2Norm = 0;

    @Override
    public float getDistance(float[] o1, float[] o2) {
        if (o1 == o2) {
            return 0;
        }
        if (o2.length != o1.length) {
            throw new IllegalArgumentException("Cannot compute distance on different vector dimensions (" + o1.length + ", " + o2.length + ")");
        }
        float numerator = 0;
        boolean cache1 = lastO1 == o1;
        boolean cache2 = lastO2 == o2;
        float o1Norm = cache1 ? lastO1Norm : 0;
        float o2Norm = cache2 ? lastO2Norm : 0;
        for (int i = 0; i < o1.length; i++) {
            numerator += o1[i] * o2[i];
            if (!cache1) {
                o1Norm += o1[i] * o1[i];
            }
            if (!cache2) {
                o2Norm += o2[i] * o2[i];
            }
        }
        float denominator = o1Norm * o2Norm;
        if (!cache1) {
            o1Norm = (float) Math.sqrt(o2Norm);
            lastO1 = o1;
            lastO1Norm = o1Norm;
        }
        if (!cache2) {
            o2Norm = (float) Math.sqrt(o2Norm);
            lastO2 = o2;
            lastO2Norm = o2Norm;
        }
        double cosine = numerator / denominator;
        return (float) Math.acos(cosine);
    }

    @Override
    public float getDistance(float[] o1, float[] o2, Object... additionalParams) {
        if (o1 == o2) {
            return 0;
        }
        if (o2.length != o1.length) {
            throw new IllegalArgumentException("Cannot compute distance on different vector dimensions (" + o1.length + ", " + o2.length + ")");
        }
        float numerator = 0;
        float o1Norm = (float) additionalParams[0];
        float o2Norm = (float) additionalParams[1];
        for (int i = 0; i < o1.length; i++) {
            numerator += o1[i] * o2[i];
        }
        float denominator = o1Norm * o2Norm; // check whether there is a correct square root!
        return 1 - numerator / denominator;
    }

}
