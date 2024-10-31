package vm.metricSpace.datasetPartitioning.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.datasetPartitioning.AbstractDatasetPartitioning;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.metricSpace.distance.impl.CosineDistance;
import vm.metricSpace.datasetPartitioning.StorageDatasetPartitionsInterface;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class VoronoiPartitioningWithoutFilter<T> extends AbstractDatasetPartitioning<T> {

    public static final Logger LOG = Logger.getLogger(VoronoiPartitioningWithoutFilter.class.getName());

    protected final DistanceFunctionInterface df;
    protected final List<Object> pivots;

    public VoronoiPartitioningWithoutFilter(AbstractMetricSpace<T> metricSpace, DistanceFunctionInterface<T> df, List<Object> pivots) {
        super(metricSpace);
        this.df = df;
        this.pivots = pivots;
    }

    @Override
    public Map<Comparable, Collection<Comparable>> partitionObjects(Iterator<Object> dataObjects, String datasetName, StorageDatasetPartitionsInterface storage, Object... params) {
        int pivotCountUsedInTheFileName = (Integer) params[0];
        Map<Comparable, Collection<Comparable>> ret = new HashMap<>();
//        int parallelism = 1;
        int parallelism = vm.javatools.Tools.PARALELISATION;

        ExecutorService threadPool = vm.javatools.Tools.initExecutor(parallelism);
        int batchCounter = 0;
        long size = 0;
        Map<Comparable, Float> lengthOfPivotVectors = null;
        if (df instanceof CosineDistance) {
            lengthOfPivotVectors = ToolsMetricDomain.getVectorsLength(pivots, metricSpace);
        }
        lastTimeOfPartitioning = System.currentTimeMillis();
        while (dataObjects.hasNext()) {
            try {
                CountDownLatch latch = new CountDownLatch(parallelism);
                AbstractDatasetPartitioning.BatchProcessor[] processes = new AbstractDatasetPartitioning.BatchProcessor[parallelism];
                for (int j = 0; j < parallelism; j++) {
                    batchCounter++;
                    List batch = Tools.getObjectsFromIterator(dataObjects, BATCH_SIZE);
                    size += batch.size();
                    Map<Comparable, Float> lengthOfBatchVectors = null;
                    if (df instanceof CosineDistance) {
                        lengthOfBatchVectors = ToolsMetricDomain.getVectorsLength(batch, metricSpace);
                    }
                    processes[j] = getBatchProcesor(batch, metricSpace, latch, lengthOfPivotVectors, lengthOfBatchVectors);
                    threadPool.execute(processes[j]);
                }
                latch.await();
                for (int j = 0; j < parallelism; j++) {
                    Map<Comparable, List<Comparable>> partial = processes[j].getRet();
                    for (Map.Entry<Comparable, List<Comparable>> partialEntry : partial.entrySet()) {
                        Comparable key = partialEntry.getKey();
                        if (!ret.containsKey(key)) {
                            List<Comparable> set = new ArrayList<>();
                            ret.put(key, set);
                        }
                        ret.get(key).addAll(partialEntry.getValue());
                    }
                }
                lastTimeOfPartitioning = System.currentTimeMillis() - lastTimeOfPartitioning;
                LOG.log(Level.INFO, "Voronoi partitioning done for {0} objects in {1} ms. Total dc: {2}", new Object[]{size, lastTimeOfPartitioning, dcOfPartitioning.get()});
            } catch (InterruptedException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
        threadPool.shutdown();
        if (storage != null) {
            storage.store(ret, datasetName, null, pivotCountUsedInTheFileName);
        }
        return ret;
    }

    protected AbstractDatasetPartitioning.BatchProcessor getBatchProcesor(List batch, AbstractMetricSpace metricSpace, CountDownLatch latch, Map<Comparable, Float> pivotLengths, Map<Comparable, Float> objectsLengths) {
        return new ProcessBatch(batch, metricSpace, latch, pivotLengths, objectsLengths);
    }

    @Override
    public String getName() {
        return "Voronoi_partitioning";
    }

    @Override
    public String getAdditionalStats() {
        return null;
    }

    private class ProcessBatch extends AbstractDatasetPartitioning<T>.BatchProcessor {

        public ProcessBatch(List batch, AbstractMetricSpace<T> metricSpace, CountDownLatch latch, Map<Comparable, Float> pivotLengths, Map<Comparable, Float> objectsLengths) {
            super(batch, metricSpace, latch, pivotLengths, objectsLengths);
        }

        @Override
        public void run() {
            long t = -System.currentTimeMillis();
            Iterator dataObjects = batch.iterator();
            float[] opDists = new float[pivots.size()];
            for (int oCounter = 1; dataObjects.hasNext(); oCounter++) {
                Object o = dataObjects.next();
                T oData = metricSpace.getDataOfMetricObject(o);
                Comparable oID = metricSpace.getIDOfMetricObject(o);
                Comparable pivotWithMinDist = null;
                Float oLength = objectsLengths.get(oID);
                float radius = Float.MAX_VALUE;
                for (int pCounterIdx = 0; pCounterIdx < pivots.size(); pCounterIdx++) {
                    Object pivot = pivots.get(pCounterIdx);
                    Comparable pivotID = metricSpace.getIDOfMetricObject(pivot);
                    T pData = metricSpace.getDataOfMetricObject(pivot);
                    Float pLength = pivotLengths.get(pivotID);
                    float dist = df.getDistance(oData, pData, oLength, pLength);
                    opDists[pCounterIdx] = dist;
                    if (dist > 0 && dist < radius) {
                        radius = dist;
                        pivotWithMinDist = pivotID;
                    }
                }
                if (!ret.containsKey(pivotWithMinDist)) {
                    ret.put(pivotWithMinDist, new ArrayList<>());
                }
                List<Comparable> list = (List<Comparable>) ret.get(pivotWithMinDist);
                list.add(oID);
                if (oCounter % 10000 == 0) {
                    LOG.log(Level.INFO, "Processed {0} objects with {1} dist comps", new Object[]{oCounter, dcOfPartitioning.get()});
                }
            }
            latch.countDown();
            t += System.currentTimeMillis();
            LOG.log(Level.INFO, "Batch finished in {0} ms", t);
        }

    }
}
