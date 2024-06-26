package vm.metricSpace.distance.bounding.nopivot.learning;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author xmic
 */
public class PxEvaluator {

    public static final Logger LOG = Logger.getLogger(PxEvaluator.class.getName());
    private static final Integer IMPLICIT_MIN_BUCKET_SIZE = 20;
    private final int minNumberOfDistsPerBucket;

    private final List<Object> sampleObjects;
    private final float distInterval;
    private final int sketchLength;
    private final Dataset fullDataset;
    private final DistanceFunctionInterface<long[]> hammingDF;
    private final Map<Comparable, long[]> sketches;

    public PxEvaluator(Dataset fullDataset, Dataset<long[]> sketchesDataset, int objCount, int sketchLength, float distInterval) {
        this(fullDataset, sketchesDataset, objCount, sketchLength, distInterval, IMPLICIT_MIN_BUCKET_SIZE);
    }

    protected PxEvaluator(Dataset fullDataset, Dataset<long[]> sketchesDataset, int objCount, int sketchLength, float distInterval, int minNumberOfExampleForBitToCount) {
        this.fullDataset = fullDataset;
        this.sampleObjects = fullDataset.getSampleOfDataset(objCount);
        this.distInterval = distInterval;
        this.sketchLength = sketchLength;
        this.minNumberOfDistsPerBucket = minNumberOfExampleForBitToCount;
        Iterator<Object> it = sketchesDataset.getMetricObjectsFromDataset();
        sketches = ToolsMetricDomain.getMetricObjectsAsIdDataMap(sketchesDataset.getMetricSpace(), it);
        hammingDF = sketchesDataset.getDistanceFunction();
    }

    /**
     * Returns pairs [x,y] for plot
     *
     * @param maxDist max distance that enables the matrix. Do not put
     * significantly bigger than observed. Can be even less than max observed
     * distance. Just for printing.
     * @param distCount
     * @return x .. orig distance, y .. probability
     */
    public SortedMap<Float, Float> evaluateProbabilities(float maxDist, int distCount) {
        try {
            SortedMap<Float, Float> sumOfProbabilities = initDistMapping(maxDist);
            // number of measurements per each dist interval
            SortedMap<Float, Float> counts = initDistMapping(maxDist);
            ExecutorService threadPool = vm.javatools.Tools.initExecutor(vm.javatools.Tools.PARALELISATION);
            if (sampleObjects.size() < 300000) {
                LOG.log(Level.INFO, "Small dataset. Going to evaluate just {0} dist sample instead of {1}", new Object[]{sampleObjects.size() * 10, distCount});
                distCount = sampleObjects.size() * 10;
            }
            final int distComparisons = distCount;
            CountDownLatch latch = new CountDownLatch(distCount);
            DistanceFunctionInterface fullDistFunc = fullDataset.getDistanceFunction();
            AbstractMetricSpace fullMetricSpace = fullDataset.getMetricSpace();
            for (int i = 0; i < distCount; i++) {
                final int idx = i + 1;
                final Object o1 = Tools.randomObject(sampleObjects);
                final Object o2 = Tools.randomObject(sampleObjects);
                Comparable o1ID = fullMetricSpace.getIDOfMetricObject(o1);
                Comparable o2ID = fullMetricSpace.getIDOfMetricObject(o2);
                Object o1Data = fullMetricSpace.getDataOfMetricObject(o1);
                Object o2Data = fullMetricSpace.getDataOfMetricObject(o2);
                long[] sk1 = (long[]) sketches.get(o1ID);
                long[] sk2 = (long[]) sketches.get(o2ID);
                threadPool.execute(() -> {
                    float origDist = fullDistFunc.getDistance(o1Data, o2Data);
                    origDist = Math.max(origDist, 0);
                    origDist = vm.math.Tools.round(origDist, distInterval, true);
                    float hamDistRelative = hammingDF.getDistance(sk1, sk2) / sketchLength;
                    addValueToMap(sumOfProbabilities, origDist, hamDistRelative);
                    addValueToMap(counts, origDist, 1f);
                    if (idx % 1000000 == 0 || idx > distComparisons - 10) {
                        Logger.getLogger(PxEvaluator.class.getName()).log(Level.INFO, "Evaluated correspondence of {0}  distances out of {1}", new Object[]{idx, distComparisons});
                    }
                    latch.countDown();
                });
            }
            latch.await();
            threadPool.shutdown();
            SortedMap<Float, Float> ret = new TreeMap<>();
            for (Float key : counts.keySet()) {
                int count = counts.get(key).intValue();
                if (count >= minNumberOfDistsPerBucket) {
                    ret.put(key, sumOfProbabilities.get(key) / count);
                }
            }
            return ret;
        } catch (InterruptedException ex) {
            Logger.getLogger(PxEvaluator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private SortedMap<Float, Float> initDistMapping(float maxDist) {
        TreeMap<Float, Float> ret = new TreeMap<>();
        for (int i = 0; i <= Math.ceil(maxDist / distInterval); i++) {
            ret.put(i * distInterval, 0f);
        }
        return ret;
    }

    private void addValueToMap(SortedMap<Float, Float> map, float key, Float value) {
        Float oldValue = map.get(key);
        if (oldValue == null) {
            String s = "";
        }
        map.put(key, oldValue + value);
    }

}
