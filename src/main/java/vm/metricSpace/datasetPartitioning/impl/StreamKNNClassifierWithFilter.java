package vm.metricSpace.datasetPartitioning.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.datasetPartitioning.AbstractDatasetPartitioning;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.metricSpace.distance.impl.CosineDistance;
import vm.metricSpace.datasetPartitioning.StorageDatasetPartitionsInterface;
import vm.metricSpace.distance.bounding.BoundsOnDistanceEstimation;
import vm.metricSpace.distance.bounding.onepivot.AbstractOnePivotFilter;
import vm.metricSpace.distance.bounding.twopivots.AbstractTwoPivotsFilter;
import vm.metricSpace.distance.bounding.twopivots.impl.DataDependentPtolemaicFilteringForStreamKNNClassifier;
import vm.metricSpace.distance.bounding.twopivots.impl.PtolemaicFilterForVoronoiPartitioning;

/**
 *
 * @author au734419
 * @param <T>
 */
public class StreamKNNClassifierWithFilter<T> extends AbstractDatasetPartitioning<T> {

    public static final Logger LOG = Logger.getLogger(StreamKNNClassifierWithFilter.class.getName());

    protected final DistanceFunctionInterface df;
    protected final List<Object> pivots;
    protected final BoundsOnDistanceEstimation filter;
    protected final float[][] pivotPivotDists;
    protected AtomicLong lbChecked;
    private final long maxCounterOfLB;
    private final int[] pivotPermutation;

    public StreamKNNClassifierWithFilter(AbstractMetricSpace<T> metricSpace, DistanceFunctionInterface<T> df, List<Object> pivots, BoundsOnDistanceEstimation filter) {
        super(metricSpace);
        this.df = df;
        this.pivots = pivots;
        this.filter = filter;
        lbChecked = new AtomicLong();
        if (filter != null) {
            pivotPivotDists = metricSpace.getDistanceMap(df, pivots, pivots);
        } else {
            pivotPivotDists = null;
        }
        maxCounterOfLB = pivots.size() / 2;
        if (filter instanceof PtolemaicFilterForVoronoiPartitioning) {
            pivotPermutation = ((PtolemaicFilterForVoronoiPartitioning) filter).pivotsOrderForLB();
        } else {
            pivotPermutation = DataDependentPtolemaicFilteringForStreamKNNClassifier.getTrivialPivotOrder(pivots.size());
        }
    }

    @Override
    public Map<Comparable, Collection<Comparable>> partitionObjects(Iterator<Object> dataObjects, String datasetName, StorageDatasetPartitionsInterface storage, Object... params) {
        int pivotCountUsedInTheFileName = (Integer) params[0];
        Map<Comparable, Collection<Comparable>> ret = new HashMap<>();
        int parallelism = 1;
//        int parallelism = vm.javatools.Tools.PARALELISATION;

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
            String suf = filter == null ? "" : filter.getTechFullName();
            storage.store(ret, datasetName, suf, pivotCountUsedInTheFileName);
            storage.store(ret, datasetName, null, pivotCountUsedInTheFileName);
        }
        return ret;
    }

    protected AbstractDatasetPartitioning.BatchProcessor getBatchProcesor(List batch, AbstractMetricSpace metricSpace, CountDownLatch latch, Map<Comparable, Float> pivotLengths, Map<Comparable, Float> objectsLengths) {
        return new ProcessBatch(batch, metricSpace, latch, pivotLengths, objectsLengths);
    }

    @Override
    public String getName() {
        String ret = "Stream_kNN_partitioning_";
        if (filter != null) {
            ret += filter.getTechFullName();
        }
        return ret;
    }

    @Override
    public String getAdditionalStats() {
        return "LB checked;" + lbChecked.get() + ";";
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
                    int pCounter = pivotPermutation[pCounterIdx];
                    Object pivot = pivots.get(pCounterIdx);
                    Comparable pivotID = metricSpace.getIDOfMetricObject(pivot);
                    T pData = metricSpace.getDataOfMetricObject(pivot);
                    Float pLength = pivotLengths.get(pivotID);
                    float dist = getDistIfSmallerThan(radius, oData, pData, oLength, pLength, opDists, pCounter);
                    opDists[pCounter] = dist;
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
                    LOG.log(Level.INFO, "Processed {0} objects with {1} dc and {2} lb", new Object[]{oCounter, dcOfPartitioning.get(), lbChecked.get()});
                    LOG.log(Level.INFO, "Processed {0} objects with {1} dist comps", new Object[]{oCounter, dcOfPartitioning.get()});
                }
            }
            latch.countDown();
            t += System.currentTimeMillis();
            LOG.log(Level.INFO, "Batch finished in {0} ms", t);
        }

        protected float getDistIfSmallerThan(float radius, T oData, T pData, Float oLength, Float pLength, float[] opDists, int pCounter) {
            boolean canBeFilteredOut = false;
            if (filter != null) {
                if (filter instanceof AbstractOnePivotFilter && pCounter > 0) {
                    canBeFilteredOut = evaluateOnePivotFilter(pCounter, opDists, radius);
                } else if (filter instanceof PtolemaicFilterForVoronoiPartitioning && pCounter > 1) {
                    canBeFilteredOut = evaluatePtolemaicFilter(pCounter, opDists, radius);
                } else if (filter instanceof AbstractTwoPivotsFilter && pCounter > 1) {
                    canBeFilteredOut = evaluateTwoPivotFilter(pCounter, opDists, radius);
                }
            }
            if (canBeFilteredOut) {
                return -1000;
            }
            dcOfPartitioning.incrementAndGet();
            return df.getDistance(oData, pData, oLength, pLength);
        }

        // One pivot filter
        // notation with respect to search:
        // here the o is q in the search
        // here the p0 is p in the search
        // here the p[pCount] is o in the search
        // here d(o, p0)        is d(q, p)   
        // here d(p0, p[pCount]) is d(p, o)   
        // here d(o, p[pCount]) is d(q, o)   
        private boolean evaluateOnePivotFilter(int pCounter, float[] opDists, float radius) {
            for (int p0 = 0; p0 < pCounter; p0++) {
                if (opDists[p0] >= 0) {
                    float distQP = opDists[p0];
                    float distPO = pivotPivotDists[p0][pCounter];
                    float lb = filter.lowerBound(distQP, distPO, p0);
                    lbChecked.incrementAndGet();
                    if (lb > radius) {
                        return true;
                    }
                }
            }
            return false;
        }

        // notation with respect to search:
        // here the o is q in the search
        // here the p0 is p1 in the search
        // here the p1 is p2 in the search
        // here the p[pCount] is o in the search
        // here d(o, p0)        is d(q, p1)   
        // here d(o, p1)        is d(q, p2)   
        // here d(p0, p[pCount]) is d(p1, o)   
        // here d(p1, p[pCount]) is d(p2, o)   
        // here d(p0, p1) is d(p1, p2)   
        // here d(o, p[pCount]) is d(q, o)   
        private boolean evaluateTwoPivotFilter(int pCounter, float[] opDists, float radius) {
            int counter = 0;
            for (int p0 = 0; p0 < pCounter - 1; p0++) {
                if (opDists[p0] >= 0) {
                    float distP1Q = opDists[p0];
                    float distP1O = pivotPivotDists[p0][pCounter];
                    counter++;
                    if (counter == maxCounterOfLB) {
                        return false;
                    }
                    for (int p1 = p0 + 1; p1 < pCounter; p1++) {
                        if (opDists[p1] >= 0) {
                            float distP1P2 = pivotPivotDists[p0][p1];
                            float distP2O = pivotPivotDists[p1][pCounter];
                            float distP2Q = opDists[p1];
                            //distP1P2, distP2O, distP1Q, distP1O, distP2Q,
                            //int p1Idx, int p2Idx, Float range
                            float lb = filter.lowerBound(distP1P2, distP2O, distP1Q, distP1O, distP2Q, -1, -1, radius);
                            lbChecked.incrementAndGet();
                            if (lb > radius) {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        }

        // notation with respect to search:
        // here the o is q in the search
        // here the p0 is p1 in the search
        // here the p1 is p2 in the search
        // here the p[pCount] is o in the search
        // here d(o, p0)        is d(q, p1)   
        // here d(o, p1)        is d(q, p2)   
        // here d(p0, p[pCount]) is d(p1, o)   
        // here d(p1, p[pCount]) is d(p2, o)   
        // here d(p0, p1) is d(p1, p2)   
        // here d(o, p[pCount]) is d(q, o)   
        private boolean evaluatePtolemaicFilter(int pCounter, float[] opDists, float radius) {
            PtolemaicFilterForVoronoiPartitioning filterCast = (PtolemaicFilterForVoronoiPartitioning) filter;
            int counter = 0;
            for (int p0idx = 0; p0idx < pCounter - 1; p0idx++) {
                counter++;
                if (counter == maxCounterOfLB) {
                    return false;
                }
                int p0 = pivotPermutation[p0idx];
                if (opDists[p0] >= 0) {
                    float distP1Q = opDists[p0];
                    for (int p1idx = p0idx + 1; p1idx < pCounter; p1idx++) {
                        int p1 = pivotPermutation[p1idx];
                        if (opDists[p1] >= 0) {
                            float distP2Q = opDists[p1];
                            float lb = filterCast.lowerBound(distP1Q, distP2Q, p0, p1, pCounter);
                            lbChecked.incrementAndGet();
                            if (lb > radius) {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        }
    }
}