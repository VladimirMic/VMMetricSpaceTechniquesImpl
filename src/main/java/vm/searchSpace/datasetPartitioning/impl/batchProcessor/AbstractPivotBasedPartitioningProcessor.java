/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.searchSpace.datasetPartitioning.impl.batchProcessor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.searchSpace.AbstractSearchSpace;
import static vm.searchSpace.datasetPartitioning.impl.batchProcessor.VoronoiPartitioningWithFilterProcessor.tCheck;
import vm.searchSpace.distance.AbstractDistanceFunction;

/**
 *
 * @author Vlada
 * @param <T>
 */
public abstract class AbstractPivotBasedPartitioningProcessor<T> implements Runnable {

    public static final Logger LOG = Logger.getLogger(AbstractPivotBasedPartitioningProcessor.class.getName());

    protected Iterator batch;
    protected final AbstractDistanceFunction df;
    protected final List<Comparable>[] ret;
    protected final AbstractSearchSpace<T> searchSpace;
    protected final List<T> pivotData;

    protected final float[] pivotLengths;
    protected Map<Comparable, Float> objectsLengths;
    protected final int numberOfPivotsUsedInFiltering;

    protected CountDownLatch latch;

    protected int lbCheckedBatch;
    protected int dcOfPartitioningBatch;

    public AbstractPivotBasedPartitioningProcessor(AbstractSearchSpace<T> searchSpace, AbstractDistanceFunction df, List<T> pivotData, int numberOfPivotsUsedInFiltering, float[] pivotLengths) {
        this.df = df;
        this.pivotData = pivotData;
        this.numberOfPivotsUsedInFiltering = numberOfPivotsUsedInFiltering;
        this.ret = new List[pivotData.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = new ArrayList<>();
        }
        this.searchSpace = searchSpace;
        this.pivotLengths = pivotLengths;
        dcOfPartitioningBatch = 0;
        lbCheckedBatch = 0;
    }

    @Override
    public void run() {
        long t = -System.currentTimeMillis();
        for (int oCounter = 1; batch.hasNext(); oCounter++) {
            Object o = batch.next();
            T oData = searchSpace.getDataOfObject(o);
            Comparable oID = searchSpace.getIDOfObject(o);
            Float oLength = objectsLengths == null ? null : objectsLengths.get(oID);
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
            long t1 = -System.currentTimeMillis();
            for (; pIdx < pivotData.size(); pIdx++) {
                T pData = pivotData.get(pIdx);
                Float pLength = pivotLengths == null ? null : pivotLengths[pIdx];
                float dist = getDistIfSmallerThan(radius, oData, pData, oLength, pLength, opDists, pIdx);
                if (dist > 0 && dist < radius) {
                    radius = dist;
                    pivotWithMinDist = pIdx;
                }
            }
            t1 += System.currentTimeMillis();
            tCheck += t1;
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

    public float getLbCheckedBatchAvgC() {
        return lbCheckedBatch / ((float) pivotData.size());
    }

    public int getDcOfPartitioningBatch() {
        return dcOfPartitioningBatch;
    }

    public void setBatch(Iterator batch) {
        this.batch = batch;
    }

    public void setObjectsLengths(Map<Comparable, Float> objectsLengths) {
        this.objectsLengths = objectsLengths;
    }

    public void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }

    public void resetDCBatch() {
        dcOfPartitioningBatch = 0;
    }

    public void resetLBBatch() {
        lbCheckedBatch = 0;
    }

}
