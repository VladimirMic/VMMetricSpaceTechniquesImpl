package vm.metricSpace.distance.bounding.onepivot.impl;

import vm.metricSpace.distance.bounding.onepivot.AbstractOnePivotFilter;

/**
 *
 * @author Vlada
 */
public class TriangleInequality extends AbstractOnePivotFilter {

    public TriangleInequality(String namePrefix) {
        super(namePrefix);
    }

    @Override
    public float lowerBound(float distQP, float distOP, int pivotIdx) {
        return Math.abs(distOP - distQP);
    }

    @Override
    public float upperBound(float distQP, float distOP, int pivotIdx) {
        return distQP + distOP;
    }

    @Override
    public String getTechName() {
        return "triangle_inequality";
    }

}
