package vm.metricSpace.voronoiPartitioning;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.metricSpace.distance.impl.CosineDistance;

/**
 *
 * @author Vlada
 */
public class VoronoiPartitioning {

    public static final Integer BATCH_SIZE = 100000;
    public static final Logger LOG = Logger.getLogger(VoronoiPartitioning.class.getName());

    private final AbstractMetricSpace metricSpace;
    private final DistanceFunctionInterface df;
    private final Map<Object, Object> pivots;
    private final List<Object> pivotsList;

    public VoronoiPartitioning(AbstractMetricSpace metricSpace, DistanceFunctionInterface df, List<Object> pivots) {
        this.metricSpace = metricSpace;
        this.df = df;
        this.pivots = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(metricSpace, pivots, true);
        this.pivotsList = pivots;
    }

    public Map<Object, SortedSet<Object>> splitByVoronoi(Iterator<Object> dataObjects, String datasetName, int pivotCountUsedInTheFileName, StorageLearnedVoronoiPartitioningInterface storage) {
        Map<Object, SortedSet<Object>> ret = new HashMap<>();
        ExecutorService threadPool = vm.javatools.Tools.initExecutor(vm.javatools.Tools.PARALELISATION);
        int batchCounter = 0;
        long size = 0;
        Map<Object, Float> lengthOfPivotVectors = null;
        if (df instanceof CosineDistance) {
            lengthOfPivotVectors = getVectorsLength(pivotsList, metricSpace);
        }
        while (dataObjects.hasNext()) {
            try {
                CountDownLatch latch = new CountDownLatch(vm.javatools.Tools.PARALELISATION);
                ProcessBatch[] processes = new ProcessBatch[vm.javatools.Tools.PARALELISATION];
                for (int j = 0; j < vm.javatools.Tools.PARALELISATION; j++) {
                    batchCounter++;
                    List batch = Tools.getObjectsFromIterator(dataObjects, BATCH_SIZE);
                    size += batch.size();
                    Map<Object, Float> lengthOfBatchVectors = null;
                    if (df instanceof CosineDistance) {
                        lengthOfBatchVectors = getVectorsLength(batch, metricSpace);
                    }
                    processes[j] = new ProcessBatch(batch, metricSpace, latch, lengthOfPivotVectors, lengthOfBatchVectors);
                    threadPool.execute(processes[j]);
                }
                latch.await();
                for (int j = 0; j < vm.javatools.Tools.PARALELISATION; j++) {
                    Map<Object, SortedSet<Object>> partial = processes[j].getRet();
                    for (Map.Entry<Object, SortedSet<Object>> partialEntry : partial.entrySet()) {
                        Object key = partialEntry.getKey();
                        if (!ret.containsKey(key)) {
                            SortedSet<Object> set = new TreeSet<>();
                            ret.put(key, set);
                        }
                        ret.get(key).addAll(partialEntry.getValue());
                    }
                }
                LOG.log(Level.INFO, "Voronoi partitioning done for {0} objects", size);
            } catch (InterruptedException ex) {
                Logger.getLogger(VoronoiPartitioning.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        threadPool.shutdown();
        if (storage != null) {
            storage.store(ret, datasetName, pivotCountUsedInTheFileName);
        }
        return ret;
    }

    private Map<Object, Float> getVectorsLength(List batch, AbstractMetricSpace metricSpace) {
        Map<Object, Float> ret = new HashMap<>();
        for (Object object : batch) {
            Object id = metricSpace.getIDOfMetricObject(object);
            float length = 0;
            float[] vector = (float[]) metricSpace.getDataOfMetricObject(object); // must be the space of floats
            for (int i = 0; i < vector.length; i++) {
                float f = vector[i];
                length += f * f;
            }
            ret.put(id, length);
        }
        return ret;
    }

    private class ProcessBatch implements Runnable {

        private final List batch;
        private final Map<Object, SortedSet<Object>> ret;
        private final AbstractMetricSpace metricSpace;
        private final Map<Object, Float> pivotLengths;
        private final Map<Object, Float> objectsLengths;

        private final CountDownLatch latch;

        public ProcessBatch(List batch, AbstractMetricSpace metricSpace, CountDownLatch latch, Map<Object, Float> pivotLengths, Map<Object, Float> objectsLengths) {
            this.batch = batch;
            this.ret = new HashMap<>();
            this.metricSpace = metricSpace;
            this.latch = latch;
            this.pivotLengths = pivotLengths == null ? new HashMap<>() : pivotLengths;
            this.objectsLengths = objectsLengths == null ? new HashMap<>() : objectsLengths;
        }

        @Override
        public void run() {
            long t = -System.currentTimeMillis();
            Iterator dataObjects = batch.iterator();
            for (int i = 0; dataObjects.hasNext(); i++) {
                Object o = dataObjects.next();
                Object oData = metricSpace.getDataOfMetricObject(o);
                Object oID = metricSpace.getIDOfMetricObject(o);
                float minDist = Float.MAX_VALUE;
                Object pivotWithMinDist = null;
                for (Map.Entry<Object, Object> pivot : pivots.entrySet()) {
                    Object pivotID = pivot.getKey();
                    float dist;
                    dist = df.getDistance(oData, pivot.getValue(), objectsLengths.get(oID), pivotLengths.get(pivotID));
                    if (dist < minDist) {
                        minDist = dist;
                        pivotWithMinDist = pivotID;
                    }
                }
                if (!ret.containsKey(pivotWithMinDist)) {
                    ret.put(pivotWithMinDist, new TreeSet<>());
                }
                ret.get(pivotWithMinDist).add(oID);
            }
            latch.countDown();
            t += System.currentTimeMillis();
            LOG.log(Level.INFO, "Batch finished in {0} ms", t);
        }

        public Map<Object, SortedSet<Object>> getRet() {
            return Collections.unmodifiableMap(ret);
        }

    }
}
