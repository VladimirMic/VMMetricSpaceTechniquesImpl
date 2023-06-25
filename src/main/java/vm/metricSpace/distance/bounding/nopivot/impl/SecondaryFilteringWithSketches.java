package vm.metricSpace.distance.bounding.nopivot.impl;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.javatools.Tools;
import vm.metricSpace.Dataset;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.metricSpace.distance.impl.HammingDistanceLongs;
import vm.metricSpace.distance.bounding.nopivot.NoPivotFilter;
import vm.metricSpace.distance.bounding.nopivot.storeLearned.SecondaryFilteringWithSketchesStoreInterface;

/**
 *
 * @author Vlada
 */
public class SecondaryFilteringWithSketches extends NoPivotFilter {

    private static final Logger LOG = Logger.getLogger(SecondaryFilteringWithSketches.class.getName());

    private final Map<Object, Object> sketches;
    private final DistanceFunctionInterface hamDistFunc;
    private final double[] primDistsThreshold;
    private final int[] hamDistsThresholds;

    public SecondaryFilteringWithSketches(String namePrefix, String fullDatasetName, Dataset<long[]> sketchingDataset, SecondaryFilteringWithSketchesStoreInterface storage, float thresholdPcum, int iDimSketchesSampleCount, int iDimDistComps, float distIntervalForPX) {
        super(namePrefix);
        this.hamDistFunc = new HammingDistanceLongs();
        SortedMap<Double, Integer> mapping = storage.loadMapping(thresholdPcum, fullDatasetName, sketchingDataset.getDatasetName(), iDimSketchesSampleCount, iDimDistComps, distIntervalForPX);
        primDistsThreshold = new double[mapping.size()];
        hamDistsThresholds = new int[mapping.size()];
        Iterator<Map.Entry<Double, Integer>> it = mapping.entrySet().iterator();
        for (int i = 0; it.hasNext(); i++) {
            Map.Entry<Double, Integer> entry = it.next();
            primDistsThreshold[i] = entry.getKey();
            hamDistsThresholds[i] = entry.getValue();
        }
        Iterator sketchesIt = sketchingDataset.getMetricObjectsFromDataset();
        LOG.log(Level.INFO, "Going to load SKETCHES for the secondary filtering with sketches");
        sketches = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(sketchingDataset.getMetricSpace(), sketchesIt, true);
    }

    public SecondaryFilteringWithSketches(String namePrefix, Dataset<long[]> sketchingDataset, double[] primDistsThreshold, int[] hamDistsThresholds) {
        super(namePrefix);
        this.hamDistFunc = new HammingDistanceLongs();
        this.primDistsThreshold = primDistsThreshold;
        this.hamDistsThresholds = hamDistsThresholds;
        Iterator sketchesIt = sketchingDataset.getMetricObjectsFromDataset();
        sketches = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(sketchingDataset.getMetricSpace(), sketchesIt, true);
    }

    public SecondaryFilteringWithSketches(String namePrefix, Map<Object, Object> sketches, double[] primDistsThreshold, int[] hamDistsThresholds) {
        super(namePrefix);
        this.sketches = sketches;
        this.hamDistFunc = new HammingDistanceLongs();
        this.primDistsThreshold = primDistsThreshold;
        this.hamDistsThresholds = hamDistsThresholds;
    }

    public float lowerBound(int hamDist, float searchRadius) {
        int pos = Arrays.binarySearch(primDistsThreshold, searchRadius);
        if (pos < 0) {
            pos = -pos - 1;
        }
        if (pos < hamDistsThresholds.length && hamDist >= hamDistsThresholds[pos]) {
            return Float.MAX_VALUE;
        }
        return 0;
    }

    public float lowerBound(long[] querySketch, Object oID, float searchRadius) {
        int pos = Arrays.binarySearch(primDistsThreshold, searchRadius);
        if (pos < 0) {
            pos = -pos - 1;
        }
        if (pos < hamDistsThresholds.length) {
            long[] skData = (long[]) sketches.get(oID);
            if (skData != null) {
                float hamDist = hamDistFunc.getDistance(querySketch, skData);
                if (hamDist >= hamDistsThresholds[pos]) {
                    return Float.MAX_VALUE;
                }
            }
        }
        return 0;
    }

    @Override
    public float lowerBound(Object... args) {
        long[] querySketch = (long[]) args[0];
        Object oID = args[1];
        Float rad = (Float) args[2];
        return lowerBound(querySketch, oID, rad);
    }

    @Override
    public float upperBound(Object... args) {
        return Float.MAX_VALUE;
    }

    @Override
    protected String getTechName() {
        return "Secondary_filtering_with_sketches";
    }

    public List<AbstractMap.SimpleEntry<Object, Integer>> evaluateHammingDistancesSequentially(long[] qSketch, List candSetIDs) {
        long x3 = -System.currentTimeMillis();
        DistEvaluationThread distEvaluationThread = new DistEvaluationThread(candSetIDs, qSketch, null);
        distEvaluationThread.run();
        x3 += System.currentTimeMillis();
        SortedSet<AbstractMap.SimpleEntry<Object, Integer>> ret = distEvaluationThread.getThreadRet();
        System.out.println("x1 " + x3);
        return new ArrayList<>(ret);
    }

    public List<AbstractMap.SimpleEntry<Object, Integer>>[] evaluateHammingDistancesInParallel(long[] qSketch, List candSetIDs) {
        try {
            long x0 = -System.currentTimeMillis();
            int batchCount = Tools.PARALELISATION;
            float batchSize = candSetIDs.size() / (float) batchCount + 0.5f;
            batchSize = vm.math.Tools.round(batchSize, 1f, false);

            CountDownLatch latch = new CountDownLatch(batchCount);
            Iterator it = candSetIDs.iterator();
            DistEvaluationThread[] threads = new DistEvaluationThread[batchCount];
            ExecutorService threadPool = Tools.initExecutor(batchCount);
            long x1 = -System.currentTimeMillis();
            for (int i = 0; i < batchCount; i++) {
                final Set batch = new HashSet();
                while (batch.size() < batchSize && it.hasNext()) {
                    batch.add(it.next());
                }
                threads[i] = new DistEvaluationThread(batch, qSketch, latch);
                threadPool.execute(threads[i]);
            }
            latch.await();
            x1 += System.currentTimeMillis();
            long x2 = -System.currentTimeMillis();
            List<AbstractMap.SimpleEntry<Object, Integer>>[] ret = new List[batchCount];
            for (int i = 0; i < batchCount; i++) {
                SortedSet<AbstractMap.SimpleEntry<Object, Integer>> threadRet = threads[i].getThreadRet();
                ret[i] = new ArrayList<>(threadRet);
            }
            x2 += System.currentTimeMillis();
            x0 += System.currentTimeMillis();
            System.out.println("x0 " + x0);
            System.out.println("x1 " + x1);
            System.out.println("x2 " + x2);
            System.out.println("batchCount " + batchCount);
            threadPool.shutdown();
            return ret;
        } catch (InterruptedException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private class DistEvaluationThread implements Runnable {

        private final SortedSet<AbstractMap.SimpleEntry<Object, Integer>> threadRet = new TreeSet<>(new vm.datatools.Tools.MapByValueIntComparator());
        private final Collection batch;
        private final long[] qSketch;
        private final CountDownLatch latch;

        public DistEvaluationThread(Collection batchOfIDs, long[] qSketch, CountDownLatch latch) {
            this.batch = batchOfIDs;
            this.qSketch = qSketch;
            this.latch = latch;
        }

        @Override
        public void run() {
            for (Object id : batch) {
                long[] oSketch = (long[]) sketches.get(id);
                int distance = (int) hamDistFunc.getDistance(qSketch, oSketch);
                threadRet.add(new AbstractMap.SimpleEntry<>(id, distance));
            }
            latch.countDown();
        }

        public SortedSet<AbstractMap.SimpleEntry<Object, Integer>> getThreadRet() {
            return Collections.unmodifiableSortedSet(threadRet);
        }

    }

}
