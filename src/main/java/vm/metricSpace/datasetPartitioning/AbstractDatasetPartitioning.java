/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package vm.metricSpace.datasetPartitioning;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import vm.metricSpace.AbstractMetricSpace;

/**
 *
 * @author xmic
 */
public abstract class AbstractDatasetPartitioning {

//    public static final Integer BATCH_SIZE = 11112;
    public static final Integer BATCH_SIZE = 10000;
    protected final AbstractMetricSpace metricSpace;

    public AbstractDatasetPartitioning(AbstractMetricSpace metricSpace) {
        this.metricSpace = metricSpace;
    }

    public abstract Map<Comparable, SortedSet<Comparable>> partitionObjects(Iterator<Object> dataObjects, String datasetName, StorageDatasetPartitionsInterface storage, Object... params);

    public abstract class BatchProcessor implements Runnable {

        protected final List batch;
        protected final ConcurrentMap<Comparable, SortedSet<Comparable>> ret;
        protected final AbstractMetricSpace metricSpace;
        protected final Map<Comparable, Float> pivotLengths;
        protected final Map<Comparable, Float> objectsLengths;

        protected final CountDownLatch latch;

        public BatchProcessor(List batch, AbstractMetricSpace metricSpace, CountDownLatch latch, Map<Comparable, Float> pivotLengths, Map<Comparable, Float> objectsLengths) {
            this.batch = batch;
            this.ret = new ConcurrentHashMap<>();
            this.metricSpace = metricSpace;
            this.latch = latch;
            this.pivotLengths = pivotLengths == null ? new HashMap<>() : pivotLengths;
            this.objectsLengths = objectsLengths == null ? new HashMap<>() : objectsLengths;
        }

        public Map<Comparable, SortedSet<Comparable>> getRet() {
            return Collections.unmodifiableMap(ret);
        }

    }
}
