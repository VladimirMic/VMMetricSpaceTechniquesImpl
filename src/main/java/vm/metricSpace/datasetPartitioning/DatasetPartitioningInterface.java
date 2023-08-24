/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package vm.metricSpace.datasetPartitioning;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;

/**
 *
 * @author xmic
 */
public interface DatasetPartitioningInterface {

    public Map<Object, SortedSet<Object>> splitByVoronoi(Iterator<Object> dataObjects, String datasetName, StorageDatasetPartitionsInterface storage, Object... params);

}
