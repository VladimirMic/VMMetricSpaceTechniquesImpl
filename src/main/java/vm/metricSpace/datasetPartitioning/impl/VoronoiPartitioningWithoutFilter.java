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
import vm.metricSpace.datasetPartitioning.impl.batchProcessor.AbstractPivotBasedPartitioningProcessor;
import vm.metricSpace.datasetPartitioning.impl.batchProcessor.BruteForceVoronoiPartitioningProcessor;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class VoronoiPartitioningWithoutFilter<T> extends AbstractDatasetPartitioning<T> {

    public static final Logger LOG = Logger.getLogger(VoronoiPartitioningWithoutFilter.class.getName());

    protected final DistanceFunctionInterface df;
    protected final List<Object> pivots;
    protected final List<T> pivotsData;
    protected final List<Comparable> pivotsIDs;
    protected final float[] lengthOfPivotVectors;

    public VoronoiPartitioningWithoutFilter(AbstractMetricSpace<T> metricSpace, DistanceFunctionInterface<T> df, List<Object> pivots) {
        super(metricSpace);
        this.df = df;
        this.pivots = pivots;
        pivotsData = ToolsMetricDomain.getDataAsList(pivots.iterator(), metricSpace);
        pivotsIDs = ToolsMetricDomain.getIDsAsList(pivots.iterator(), metricSpace);

        if (df instanceof CosineDistance) {
            lengthOfPivotVectors = ToolsMetricDomain.getVectorsLengthAsArray(pivots, metricSpace);
        } else {
            lengthOfPivotVectors = null;
        }
    }

    @Override
    public Map<Comparable, Collection<Comparable>> partitionObjects(Iterator<Object> dataObjects, String datasetName, StorageDatasetPartitionsInterface storage, Object... params) {
        int pivotCountUsedInTheFileName = (Integer) params[0];
        Map<Comparable, Collection<Comparable>> ret = new HashMap<>();
//        int parallelism = vm.javatools.Tools.PARALELISATION;
        int parallelism = 1;

        ExecutorService threadPool = vm.javatools.Tools.initExecutor(parallelism);
        int batchCounter = 0;
        long size = 0;
        lastTimeOfPartitioning = System.currentTimeMillis();
        while (dataObjects.hasNext()) {
            try {
                CountDownLatch latch = new CountDownLatch(parallelism);
                AbstractPivotBasedPartitioningProcessor[] processes = new AbstractPivotBasedPartitioningProcessor[parallelism];
                for (int j = 0; j < parallelism; j++) {
                    batchCounter++;
                    List batch = Tools.getObjectsFromIterator(dataObjects, BATCH_SIZE);
                    size += batch.size();
                    Map<Comparable, Float> lengthOfBatchVectors = null;
                    if (df instanceof CosineDistance) {
                        lengthOfBatchVectors = ToolsMetricDomain.getVectorsLength(batch, metricSpace);
                    }
                    processes[j] = getBatchProcesor(batch, metricSpace, latch, pivots.size(), lengthOfPivotVectors, lengthOfBatchVectors);
                    threadPool.execute(processes[j]);
                }
                latch.await();
                for (int j = 0; j < parallelism; j++) {
                    List[] partial = processes[j].getRet();
                    dcOfPartitioning += processes[j].getDcOfPartitioningBatch();
                    for (int i = 0; i < partial.length; i++) {
                        List cell = partial[i];
                        Comparable key = pivotsIDs.get(i);
                        if (!ret.containsKey(key)) {
                            List<Comparable> set = new ArrayList<>();
                            ret.put(key, set);
                        }
                        ret.get(key).addAll(cell);
                    }
                }
                lastTimeOfPartitioning = System.currentTimeMillis() - lastTimeOfPartitioning;
                getAdditionalStats(processes);
                LOG.log(Level.INFO, "kNN classification done for {0} objects in {1} ms. Total dc: {2}, total LBs: {3}", new Object[]{size, lastTimeOfPartitioning, dcOfPartitioning});
            } catch (InterruptedException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
        threadPool.shutdown();
        if (storage != null) {
            storage.store(ret, datasetName, getSuffixForOutputFileName(), pivotCountUsedInTheFileName);
        }
        return ret;
    }

    protected AbstractPivotBasedPartitioningProcessor getBatchProcesor(List batch, AbstractMetricSpace metricSpace, CountDownLatch latch, int classesCount, float[] pivotLengths, Map<Comparable, Float> objectsLengths) {
        return new BruteForceVoronoiPartitioningProcessor(batch, metricSpace, df, latch, pivotsData, pivotLengths, objectsLengths);
    }

    @Override
    public String getName() {
        return "Voronoi_partitioning";
    }

    protected String getSuffixForOutputFileName() {
        return "";
    }

    @Override
    public String getAdditionalStats(AbstractPivotBasedPartitioningProcessor[] processes) {
        return "";
    }

}
