/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package vm.metricSpace.datasetPartitioning;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.datasetPartitioning.impl.batchProcessor.AbstractPivotBasedPartitioningProcessor;

/**
 *
 * @author xmic
 * @param <T>
 */
public abstract class AbstractDatasetPartitioning<T> {

    public static final Integer BATCH_SIZE = 10000;
    protected final AbstractMetricSpace<T> metricSpace;
    protected long lastTimeOfPartitioning = 0;
    protected long dcOfPartitioning = 0;
    protected String lastAdditionalStats = "";

    public AbstractDatasetPartitioning(AbstractMetricSpace<T> metricSpace) {
        this.metricSpace = metricSpace;
    }

    public abstract Map<Comparable, Collection<Comparable>> partitionObjects(Iterator<Object> dataObjects, String datasetName, StorageDatasetPartitionsInterface storage, Object... params);

    public abstract String getName();

    public abstract String getAdditionalStats(AbstractPivotBasedPartitioningProcessor[] processes);

    public long getLastTimeOfPartitioning() {
        return lastTimeOfPartitioning;
    }

    public String getLastAdditionalStats() {
        return lastAdditionalStats;
    }

    public long getDcOfPartitioning() {
        return dcOfPartitioning;
    }

}
