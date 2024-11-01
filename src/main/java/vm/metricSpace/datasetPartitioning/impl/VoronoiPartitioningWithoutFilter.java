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
import vm.metricSpace.datasetPartitioning.impl.batchProcessor.VoronoiPartitioningWithFilterProcessor;

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
        int parallelism = getParalelism();

        ExecutorService threadPool = vm.javatools.Tools.initExecutor(parallelism);
        int batchCounter = 0;
        long size = 0;
        lastTimeOfPartitioning = -System.currentTimeMillis();
        AbstractPivotBasedPartitioningProcessor[] processes = new AbstractPivotBasedPartitioningProcessor[parallelism];

        for (int j = 0; j < parallelism; j++) {
            processes[j] = getBatchProcesor(metricSpace, pivots.size(), lengthOfPivotVectors);
        }
        while (dataObjects.hasNext()) {
            try {
                CountDownLatch latch = new CountDownLatch(parallelism);
                for (int j = 0; j < parallelism; j++) {
                    batchCounter++;
                    lastTimeOfPartitioning += System.currentTimeMillis();
                    List batch = Tools.getObjectsFromIterator(dataObjects, BATCH_SIZE);
                    lastTimeOfPartitioning -= System.currentTimeMillis();
                    size += batch.size();
                    if (df instanceof CosineDistance) {
                        Map<Comparable, Float> lengthOfBatchVectors = ToolsMetricDomain.getVectorsLength(batch, metricSpace);
                        processes[j].setObjectsLengths(lengthOfBatchVectors);
                    }
                    processes[j].setBatch(batch.iterator());
                    processes[j].setLatch(latch);
                    processes[j].resetDCBatch();
                    processes[j].resetLBBatch();
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
                LOG.log(Level.INFO, "kNN classification done for {0} objects. Total dc: {1}", new Object[]{size, dcOfPartitioning});
            } catch (InterruptedException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
        setAdditionalStats(processes);
        lastTimeOfPartitioning += System.currentTimeMillis();
        System.out.println("XXXXX " + VoronoiPartitioningWithFilterProcessor.tCheck + ", " + lastTimeOfPartitioning + ", " + ((float) VoronoiPartitioningWithFilterProcessor.tCheck / lastTimeOfPartitioning));
        threadPool.shutdown();
        if (storage != null) {
            storage.store(ret, datasetName, getSuffixForOutputFileName(), pivotCountUsedInTheFileName);
        }
        return ret;
    }

    protected AbstractPivotBasedPartitioningProcessor getBatchProcesor(AbstractMetricSpace metricSpace, int classesCount, float[] pivotLengths) {
        return new BruteForceVoronoiPartitioningProcessor(metricSpace, df, pivotsData, pivotLengths);
    }

    @Override
    public String getName() {
        return "Voronoi_partitioning_" + getParalelism() + "par";
    }

    protected String getSuffixForOutputFileName() {
        return "";
    }

    @Override
    public void setAdditionalStats(AbstractPivotBasedPartitioningProcessor[] processes) {
        lastAdditionalStats = "";
    }

}
