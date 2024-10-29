/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.metricSpace;

import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author xmic
 * @param <T>
 */
public class MetricSpaceWithIDsAsObjects<T> extends AbstractMetricSpace<T> {

    private final DistanceFunctionInterface<T> df;

    public MetricSpaceWithIDsAsObjects(DistanceFunctionInterface<T> df) {
        this.df = df;
    }

    @Override
    public DistanceFunctionInterface<T> getDistanceFunctionForDataset(String datasetName, Object... params) {
        return df;
    }

    @Override
    public Comparable getIDOfMetricObject(Object o) {
        return (Comparable) o;
    }

    @Override
    public T getDataOfMetricObject(Object o) {
        return (T) o;
    }

    @Override
    public Object createMetricObject(Comparable id, T data) {
        return id;
    }

}
