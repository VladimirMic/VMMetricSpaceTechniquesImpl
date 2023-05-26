package vm.objTransforms.objectToSketchTransformators;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.objTransforms.storeLearned.GHPSketchingPivotPairsStoreInterface;

/**
 *
 * @author xmic
 */
public class SketchingGHP extends AbstractObjectToSketchTransformator {

    private static final Logger LOG = Logger.getLogger(SketchingGHP.class.getName());

    public SketchingGHP(DistanceFunctionInterface<Object> distanceFunc, AbstractMetricSpace<Object> metricSpace, List<Object> pivots, boolean makeAllPivotPairs, Object... additionalInfo) {
        this(distanceFunc, metricSpace, pivots.toArray(), makeAllPivotPairs, additionalInfo);
    }

    public SketchingGHP(DistanceFunctionInterface<Object> distanceFunc, AbstractMetricSpace<Object> metricSpace, Object[] pivots, boolean makeAllPivotPairs, Object... additionalInfo) {
        super(distanceFunc, metricSpace, pivots, additionalInfo);
        if (makeAllPivotPairs) {
            makeAllPivotsPairs();
        }
    }

    /**
     * Creates pivot pairs as defined in the csv file. Notice that the list of
     * the pivots must be set in advance in the variable given in the
     * constructor
     *
     * @param storage
     * @param csvFileName
     */
    @Override
    public final void setPivotPairsFromStorage(GHPSketchingPivotPairsStoreInterface storage, String csvFileName) {
        List<String[]> pivotPairsIDs = storage.loadPivotPairsIDs(csvFileName);
        Map<Object, Object> pivotsMap = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(metricSpace, Tools.arrayToList(pivots), false);
        Object[] pivotPairs = new Object[pivotPairsIDs.size() * 2];
        for (int i = 0; i < pivotPairsIDs.size(); i++) {
            String[] pivotPairIDs = pivotPairsIDs.get(i);
            Object p1 = pivotsMap.get(pivotPairIDs[0]);
            Object p2 = pivotsMap.get(pivotPairIDs[1]);
            pivotPairs[2 * i] = p1;
            pivotPairs[2 * i + 1] = p2;
        }
        pivots = pivotPairs;
    }

    private void makeAllPivotsPairs() {
        int length = pivots.length;
        Object[] newPivots = new Object[length * (length - 1)];
        int counter = 0;
        for (int i = 0; i < length - 1; i++) {
            for (int j = i + 1; j < length; j++) {
                newPivots[counter] = pivots[i];
                counter++;
                newPivots[counter] = pivots[j];
                counter++;
            }
        }
        pivots = newPivots;
    }

    @Override
    protected int getSketchLength() {
        return pivots.length / 2;
    }

    @Override
    public void redefineSketchingToSwitchBit(int i) {
        Object pivot = pivots[2 * i];
        pivots[2 * i] = pivots[2 * i + 1];
        pivots[2 * i + 1] = pivot;
    }

    @Override
    public final String getTechniqueAbbreviation() {
        return "GHP";
    }

    @Override
    public void preserveJustGivenBits(int[] bitsToPreserve) {
        Object[] newPivots = new Object[2 * bitsToPreserve.length];
        for (int i = 0; i < bitsToPreserve.length; i++) {
            int sketchIndex = bitsToPreserve[i];
            newPivots[2 * i] = pivots[2 * sketchIndex];
            newPivots[2 * i + 1] = pivots[2 * sketchIndex + 1];
        }
        pivots = newPivots;
    }

    @Override
    public List<BitSet> createColumnwiseSketches(AbstractMetricSpace<Object> metricSpace, List<Object> sampleObjects, DistanceFunctionInterface<Object> df) {
        LOG.log(Level.INFO, "Start creating inverted sketches for: {0} sample objects", sampleObjects.size());
        try {
            List<BitSet> ret = new ArrayList<>();
            int invertedSketchesCount = getSketchLength();
            for (int i = 0; i < invertedSketchesCount; i++) {
                ret.add(new BitSet());
            }
            if (threadPool == null) {
                threadPool = vm.javatools.Tools.initExecutor(vm.javatools.Tools.PARALELISATION);
            }
            List<Object> dataOfSampleObjects = metricSpace.getDataOfMetricObjects(sampleObjects);
            List<Object> idsOfSampleObjects = metricSpace.getIDsOfMetricObjects(sampleObjects);
            final float[][] dists = additionalInfo != null && additionalInfo.length >= 3 ? (float[][]) additionalInfo[0] : null;
            final Map<String, Integer> columns = additionalInfo != null && additionalInfo.length >= 3 ? (Map<String, Integer>) additionalInfo[1] : null;
            final Map<String, Integer> rows = additionalInfo != null && additionalInfo.length >= 3 ? (Map<String, Integer>) additionalInfo[2] : null;

            CountDownLatch latch = new CountDownLatch(sampleObjects.size());
            for (int oIndex = 0; oIndex < sampleObjects.size(); oIndex++) {
                final Object oData = dataOfSampleObjects.get(oIndex);
                final Object oID = idsOfSampleObjects.get(oIndex);
                final Map<Object, Float> distsCache = new ConcurrentHashMap<>();
                final int oIndexF = oIndex;
                threadPool.execute(() -> {
                    for (int bitIndex = 0; bitIndex < invertedSketchesCount; bitIndex++) {
                        Object p1Data = metricSpace.getDataOfMetricObject(pivots[2 * bitIndex]);
                        Object p2Data = metricSpace.getDataOfMetricObject(pivots[2 * bitIndex + 1]);
                        Object p1ID = metricSpace.getIDOfMetricObject(pivots[2 * bitIndex]);
                        Object p2ID = metricSpace.getIDOfMetricObject(pivots[2 * bitIndex + 1]);
                        final int p1idx = columns != null && columns.containsKey(p1ID) ? columns.get(p1ID) : -1;
                        final int p2idx = columns != null && columns.containsKey(p2ID) ? columns.get(p2ID) : -1;
                        int oidx = p1idx > -1 && p2idx > -1 && rows.containsKey(oID) ? rows.get(oID) : -1;
                        float d1 = p1idx > -1 && p2idx > -1 && oidx > -1 ? dists[oidx][p1idx] : getDistance(df, distsCache, oData, p1Data);
                        float d2 = p1idx > -1 && p2idx > -1 && oidx > -1 ? dists[oidx][p2idx] : getDistance(df, distsCache, oData, p2Data);
                        if (d1 < d2) {
                            BitSet columnSketch = ret.get(bitIndex);
                            synchronized (columnSketch) {
                                columnSketch.set(oIndexF);
                            }
                        }
                    }
                    latch.countDown();
                    long count = latch.getCount();
                    if (count % 500 == 0) {
                        LOG.log(Level.INFO, "Creating inverted sketches. Remains {0}", count);
                    }
                });
            }
            latch.await();
            threadPool.shutdown();
            LOG.log(Level.INFO, "Inverted sketches created. (Sample objects count {0})", new Object[]{dataOfSampleObjects.size()});
            return ret;
        } catch (InterruptedException ex) {
            Logger.getLogger(SketchingGHP.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private float getDistance(DistanceFunctionInterface df, Map<Object, Float> cacheDistsOfPivotsToO, Object oData, Object pData) {
        if (cacheDistsOfPivotsToO.containsKey(pData)) {
            return cacheDistsOfPivotsToO.get(pData);
        }
        float ret = df.getDistance(pData, oData);
        cacheDistsOfPivotsToO.put(pData, ret);
        return ret;
    }

    @Override
    public Object transformMetricObject(Object obj, Object... params) {
        Object oID = metricSpace.getIDOfMetricObject(obj);
        Object oData = metricSpace.getDataOfMetricObject(obj);
        BitSet sketch = new BitSet(pivots.length / 2);
        for (int i = 0; i < pivots.length; i += 2) {
            Object p1Data = metricSpace.getDataOfMetricObject(pivots[i]);
            Object p2Data = metricSpace.getDataOfMetricObject(pivots[i + 1]);
            float d1 = distanceFunc.getDistance(oData, p1Data);
            float d2 = distanceFunc.getDistance(oData, p2Data);
            if (d1 > d2) {
                sketch.set(i / 2);
            }
        }
        AbstractMap.SimpleEntry ret = new AbstractMap.SimpleEntry(oID, sketch.toLongArray());
        return ret;
    }

}
