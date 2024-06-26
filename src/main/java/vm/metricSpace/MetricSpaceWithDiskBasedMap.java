/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.metricSpace;

import java.util.Map;
import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class MetricSpaceWithDiskBasedMap<T> extends AbstractMetricSpace<T> {

    private final AbstractMetricSpace<T> origMetricSpace;
    private final Map<Object, T> diskBasedStorage;

    public MetricSpaceWithDiskBasedMap(AbstractMetricSpace<T> origMetricSpace, Map<Object, T> diskBasedStorage) {
        this.origMetricSpace = origMetricSpace;
        this.diskBasedStorage = diskBasedStorage;
    }

    @Override
    public DistanceFunctionInterface<T> getDistanceFunctionForDataset(String datasetName, Object... params) {
        return origMetricSpace.getDistanceFunctionForDataset(datasetName, params);
    }

    @Override
    public Comparable getIDOfMetricObject(Object o) {
        try {
            return origMetricSpace.getIDOfMetricObject(o);
        } catch (Exception e) {
        }
        return (Comparable) o;
    }

    @Override
    public T getDataOfMetricObject(Object o) {
        try {
            return origMetricSpace.getDataOfMetricObject(o);
        } catch (Exception e) {
        }
        return diskBasedStorage.get(o);
    }

    @Override
    public Object createMetricObject(Comparable id, T data) {
        return origMetricSpace.createMetricObject(id, data);
    }

}
