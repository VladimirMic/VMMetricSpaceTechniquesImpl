package vm.metricSpace.distance.bounding.onepivot.impl;

import java.util.Map;
import vm.metricSpace.distance.bounding.onepivot.OnePivotFilter;

/**
 *
 * @author Vlada
 */
public class DataDependentMetricFiltering extends OnePivotFilter {

    private final Map<String, Float> pivotToCoefMapping;

    public DataDependentMetricFiltering(String resultPreffixName, Map<String, Float> pivotToCoefMapping) {
        super(resultPreffixName);
        this.pivotToCoefMapping = pivotToCoefMapping;
    }

    @Override
    public float lowerBound(float distQP, float distOP, String pivotID) {
        if (!pivotToCoefMapping.containsKey(pivotID)) {
            throw new IllegalArgumentException("No coefficient for pivot " + pivotID + " provided. Loaded info about " + pivotToCoefMapping.size() + " pivots");
        }
        float coef = pivotToCoefMapping.get(pivotID);
        return Math.abs(distQP - distOP) * coef;
    }

    @Override
    public float upperBound(float distQP, float distOP, String pivotID) {
        return Float.MAX_VALUE;
    }

    @Override
    public String getTechName() {
        return "data-dependent_metric_filtering";
    }

}
