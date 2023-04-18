package vm.metricSpace.distance.bounding.onepivot.impl;

import vm.metricSpace.distance.bounding.onepivot.OnePivotFiltering;

/**
 *
 * @author Vlada
 */
public class TriangleInequality extends OnePivotFiltering {

    public TriangleInequality(String namePrefix) {
        super(namePrefix);
    }

    @Override
    public float lowerBound(float distQP, float distOP, String pivotID) {
        return Math.abs(distQP - distOP);
    }

    @Override
    public float upperBound(float distQP, float distOP, String pivotID) {
        return distQP + distOP;
    }

    @Override
    public String getTechName() {
        return "triangle_inequality";
    }

}
