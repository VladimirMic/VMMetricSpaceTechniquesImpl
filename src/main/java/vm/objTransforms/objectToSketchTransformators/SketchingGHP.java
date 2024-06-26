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
import vm.objTransforms.storeLearned.PivotPairsStoreInterface;

/**
 *
 * @author xmic
 */
public class SketchingGHP extends AbstractObjectToSketchTransformator {

    private static final Logger LOG = Logger.getLogger(SketchingGHP.class.getName());

    public SketchingGHP(DistanceFunctionInterface<Object> distanceFunc, AbstractMetricSpace<Object> metricSpace, List<Object> pivots, String pivotPairsFileName, PivotPairsStoreInterface storageOfPivotPairs, Object... additionalInfo) {
        this(distanceFunc, metricSpace, pivots.toArray(), false, additionalInfo);
        setPivotPairsFromStorage(storageOfPivotPairs, pivotPairsFileName);
    }

    public SketchingGHP(DistanceFunctionInterface<Object> distanceFunc, AbstractMetricSpace<Object> metricSpace, List<Object> pivots, String fullDatasetName, float balance, int sketchLength, PivotPairsStoreInterface storageOfPivotPairs, Object... additionalInfo) {
        this(distanceFunc, metricSpace, pivots.toArray(), false, additionalInfo);
        String pivotPairsFileName = getNameOfTransformedSetOfObjects(fullDatasetName, sketchLength, balance);
        setPivotPairsFromStorage(storageOfPivotPairs, pivotPairsFileName);
    }

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
     * @param pivotPairsFileName
     */
    @Override
    public final void setPivotPairsFromStorage(PivotPairsStoreInterface storage, String pivotPairsFileName) {
        List<String[]> pivotPairsIDs = storage.loadPivotPairsIDs(pivotPairsFileName);
        Map<Comparable, Object> pivotsMap = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(metricSpace, Tools.arrayToList(pivots));
        Object[] pivotPairs = new Object[pivotPairsIDs.size() * 2];
        for (int i = 0; i < pivotPairsIDs.size(); i++) {
            String[] pivotPairIDs = pivotPairsIDs.get(i);
            Object p1 = pivotsMap.get(pivotPairIDs[0]);
            Object p2 = pivotsMap.get(pivotPairIDs[1]);
            if (p1 == null || p2 == null) {
                LOG.log(Level.SEVERE, "CSV file contains pivot ID which is not in the list of pivots!{0}, {1}", new Object[]{pivotPairIDs[0], pivotPairIDs[1]});
                throw new Error();
            }
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
        LOG.log(Level.INFO, "Start creating inverted sketches for {0} sample objects", sampleObjects.size());
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
            List<Comparable> idsOfSampleObjects = metricSpace.getIDsOfMetricObjects(sampleObjects.iterator());
            final float[][] dists = additionalInfo != null && additionalInfo.length >= 3 ? (float[][]) additionalInfo[0] : null;
            final Map<String, Integer> columns = additionalInfo != null && additionalInfo.length >= 3 ? (Map<String, Integer>) additionalInfo[1] : null;
            final Map<String, Integer> rows = additionalInfo != null && additionalInfo.length >= 3 ? (Map<String, Integer>) additionalInfo[2] : null;

            CountDownLatch latch = new CountDownLatch(sampleObjects.size());
            for (int oIndex = 0; oIndex < sampleObjects.size(); oIndex++) {
                final Object oData = dataOfSampleObjects.get(oIndex);
                final Comparable oID = idsOfSampleObjects.get(oIndex);
                final Map<Object, Float> distsCache = new ConcurrentHashMap<>();
                final int oIndexF = oIndex;
                threadPool.execute(() -> {
                    for (int bitIndex = 0; bitIndex < invertedSketchesCount; bitIndex++) {
                        Object p1Data = metricSpace.getDataOfMetricObject(pivots[2 * bitIndex]);
                        Object p2Data = metricSpace.getDataOfMetricObject(pivots[2 * bitIndex + 1]);
                        Comparable p1ID = metricSpace.getIDOfMetricObject(pivots[2 * bitIndex]);
                        Comparable p2ID = metricSpace.getIDOfMetricObject(pivots[2 * bitIndex + 1]);
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
                        System.gc();
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
        Map<Object, Float> precomputedDists = null;
        if (params.length != 0 && params[0] instanceof Map) {
            precomputedDists = (Map<Object, Float>) params[0];
        }
        for (int i = 0; i < pivots.length; i += 2) {
            Object p1Data = metricSpace.getDataOfMetricObject(pivots[i]);
            Object p2Data = metricSpace.getDataOfMetricObject(pivots[i + 1]);
            Object p1Id = metricSpace.getIDOfMetricObject(pivots[i]);
            Object p2Id = metricSpace.getIDOfMetricObject(pivots[i + 1]);
            float d1;
            if (precomputedDists != null && precomputedDists.containsKey(p1Id)) {
                d1 = precomputedDists.get(p1Id);
            } else {
                d1 = distanceFunc.getDistance(oData, p1Data);
            }
            float d2;
            if (precomputedDists != null && precomputedDists.containsKey(p2Id)) {
                d2 = precomputedDists.get(p2Id);
            } else {
                d2 = distanceFunc.getDistance(oData, p2Data);
            }
            if (d1 > d2) {
                sketch.set(i / 2);
            }
        }
        AbstractMap.SimpleEntry ret = new AbstractMap.SimpleEntry(oID, sketch.toLongArray());
        return ret;
    }

}
