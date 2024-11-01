/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.metricSpace.datasetPartitioning.impl.batchProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author Vlada
 * @param <T>
 */
public abstract class AbstractPivotBasedPartitioningProcessor<T> implements Runnable {

    public static final Logger LOG = Logger.getLogger(AbstractPivotBasedPartitioningProcessor.class.getName());

    protected final List batch;
    protected final DistanceFunctionInterface df;
    protected final List<Comparable>[] ret;
    protected final AbstractMetricSpace<T> metricSpace;
    protected final List<T> pivotData;

    protected final float[] pivotLengths;
    protected final Map<Comparable, Float> objectsLengths;
    protected final int numberOfPivotsUsedInFiltering;

    protected final CountDownLatch latch;

    protected int lbCheckedBatch;
    protected int dcOfPartitioningBatch;

    public AbstractPivotBasedPartitioningProcessor(List batch, AbstractMetricSpace<T> metricSpace, DistanceFunctionInterface df, CountDownLatch latch, List<T> pivotData, int numberOfPivotsUsedInFiltering, float[] pivotLengths, Map<Comparable, Float> objectsLengths) {
        this.batch = batch;
        this.df = df;
        this.pivotData = pivotData;
        this.numberOfPivotsUsedInFiltering = numberOfPivotsUsedInFiltering;
        this.ret = new List[pivotData.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = new ArrayList<>();
        }
        this.metricSpace = metricSpace;
        this.latch = latch;
        this.pivotLengths = pivotLengths;
        this.objectsLengths = objectsLengths == null ? new HashMap<>() : objectsLengths;
        dcOfPartitioningBatch = 0;
        lbCheckedBatch = 0;
    }

    @Override
    public void run() {
        long t = -System.currentTimeMillis();
        Iterator dataObjects = batch.iterator();
        for (int oCounter = 1; dataObjects.hasNext(); oCounter++) {
            Object o = dataObjects.next();
            T oData = metricSpace.getDataOfMetricObject(o);
            Comparable oID = metricSpace.getIDOfMetricObject(o);
            Float oLength = objectsLengths.get(oID);
            if (oID.equals("0000042502")) {
                String s = "";
            }
            int pivotWithMinDist = 0;
            // compute dists to pivots, get closest pivot and its distance
            float[] opDists = new float[numberOfPivotsUsedInFiltering];
            float radius = Float.MAX_VALUE;
            int pIdx;
            for (pIdx = 0; pIdx < numberOfPivotsUsedInFiltering; pIdx++) {
                opDists[pIdx] = df.getDistance(oData, pivotData.get(pIdx), oLength, pivotLengths == null ? null : pivotLengths[pIdx]);
                if (opDists[pIdx] < radius) {
                    radius = opDists[pIdx];
                    pivotWithMinDist = pIdx;
                }
            }
            dcOfPartitioningBatch += numberOfPivotsUsedInFiltering;

            for (; pIdx < pivotData.size(); pIdx++) {
                T pData = pivotData.get(pIdx);
                Float pLength = pivotLengths == null ? null : pivotLengths[pIdx];
                float dist = getDistIfSmallerThan(radius, oData, pData, oLength, pLength, opDists, pIdx);
                if (dist > 0 && dist < radius) {
                    radius = dist;
                    pivotWithMinDist = pIdx;
                }
            }
            List<Comparable> list = ret[pivotWithMinDist];
            list.add(oID);
            if (oCounter % 10000 == 0) {
                LOG.log(Level.INFO, "Processed {0} objects with {1} dc and {2} lb", new Object[]{oCounter, dcOfPartitioningBatch, lbCheckedBatch});
            }
        }
        latch.countDown();
        t += System.currentTimeMillis();

        LOG.log(Level.INFO, "Batch finished in {0} ms after {1} dc and {2} LBs", new Object[]{t, dcOfPartitioningBatch, lbCheckedBatch});
    }

    protected abstract float getDistIfSmallerThan(float radius, T oData, T pData, Float oLength, Float pLength, float[] opDists, int pCounter);

    public List<Comparable>[] getRet() {
        return ret;
    }

    public int getLbCheckedBatch() {
        return lbCheckedBatch;
    }

    public int getDcOfPartitioningBatch() {
        return dcOfPartitioningBatch;
    }

}
