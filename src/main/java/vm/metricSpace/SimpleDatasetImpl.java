/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.metricSpace;

import java.util.Map;

/**
 *
 * @author Vlada
 * @param <T> type of data used to compute the distance
 */
public class SimpleDatasetImpl<T> extends Dataset<T> {

    private int recommendedNumberOfPivots = -1;
    private final String querysetName;
    private final String pivotsetName;

    public SimpleDatasetImpl(String datasetName, String querysetName, String pivotsetName, AbstractMetricSpace<T> metricSpace, AbstractMetricSpacesStorage metricSpacesStorage) {
        super(datasetName, metricSpace, metricSpacesStorage);
        this.querysetName = querysetName;
        this.pivotsetName = pivotsetName;
    }

    @Override
    public Map<Comparable, T> getKeyValueStorage() {
        return null;
    }

    @Override
    public boolean hasKeyValueStorage() {
        return false;
    }

    @Override
    public void deleteKeyValueStorage() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public int getRecommendedNumberOfPivotsForFiltering() {
        return recommendedNumberOfPivots;
    }

    public void setRecommendedNumberOfPivots(int recommendedNumberOfPivots) {
        this.recommendedNumberOfPivots = recommendedNumberOfPivots;
    }

    @Override
    public String getQuerySetName() {
        return querysetName;
    }

    @Override
    public String getPivotSetName() {
        return pivotsetName;
    }

}
