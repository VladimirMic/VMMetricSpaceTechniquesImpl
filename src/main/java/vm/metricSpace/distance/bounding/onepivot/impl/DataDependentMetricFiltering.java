package vm.metricSpace.distance.bounding.onepivot.impl;

import vm.metricSpace.distance.bounding.onepivot.AbstractOnePivotFilter;

/**
 *
 * @author Vlada
 */
public class DataDependentMetricFiltering extends AbstractOnePivotFilter {

    private final float[] coefsForPivot;

    public DataDependentMetricFiltering(String resultPreffixName, float[] coefsForPivot) {
        super(resultPreffixName);
        this.coefsForPivot = coefsForPivot;
    }

    @Override
    public float lowerBound(float distQP, float distOP, int pivotdx) {
        return Math.abs(distOP - distQP) * coefsForPivot[pivotdx];
    }

    @Override
    public float upperBound(float distQP, float distOP, int pivotIdx) {
        return Float.MAX_VALUE;
    }

    @Override
    public String getTechName() {
        return "data-dependent_metric_filtering";
    }

}
