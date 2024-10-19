/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package vm.metricSpace.datasetPartitioning;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import vm.metricSpace.AbstractMetricSpace;

/**
 *
 * @author xmic
 * @param <T>
 */
public abstract class AbstractDatasetPartitioning<T> {

    public static final Integer BATCH_SIZE = 1000000;
    protected final AbstractMetricSpace<T> metricSpace;
    protected long lastTimeOfPartitioning = 0;
    protected long dcOfPartitioning = 0;

    public AbstractDatasetPartitioning(AbstractMetricSpace<T> metricSpace) {
        this.metricSpace = metricSpace;
    }

    public abstract Map<Comparable, Collection<Comparable>> partitionObjects(Iterator<Object> dataObjects, String datasetName, StorageDatasetPartitionsInterface storage, Object... params);

    public abstract String getName();

    public abstract String getAdditionalStats();

    public long getLastTimeOfPartitioning() {
        return lastTimeOfPartitioning;
    }

    public long getDcOfPartitioning() {
        return dcOfPartitioning;
    }

    public abstract class BatchProcessor implements Runnable {

        protected final List batch;
        protected final ConcurrentMap<Comparable, List<Comparable>> ret;
        protected final AbstractMetricSpace<T> metricSpace;
        protected final Map<Comparable, Float> pivotLengths;
        protected final Map<Comparable, Float> objectsLengths;

        protected final CountDownLatch latch;

        public BatchProcessor(List batch, AbstractMetricSpace<T> metricSpace, CountDownLatch latch, Map<Comparable, Float> pivotLengths, Map<Comparable, Float> objectsLengths) {
            this.batch = batch;
            this.ret = new ConcurrentHashMap<>();
            this.metricSpace = metricSpace;
            this.latch = latch;
            this.pivotLengths = pivotLengths == null ? new HashMap<>() : pivotLengths;
            this.objectsLengths = objectsLengths == null ? new HashMap<>() : objectsLengths;
        }

        public Map<Comparable, List<Comparable>> getRet() {
            return Collections.unmodifiableMap(ret);
        }

    }
}
