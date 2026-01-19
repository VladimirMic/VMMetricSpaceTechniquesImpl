package vm.search.algorithm.impl;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import vm.datatools.Tools;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.distance.AbstractDistanceFunction;
import vm.searchSpace.distance.bounding.twopivots.AbstractPtolemaicBasedFiltering;
import vm.search.algorithm.SearchingAlgorithm;
import vm.searchSpace.distance.bounding.twopivots.impl.PtolemaicFiltering;

/**
 *
 * @author Vlada
 * @param <T> type of object data
 */
public class KNNSearchWithPtolemaicFiltering<T> extends SearchingAlgorithm<T> {

    private static boolean query_dynamic_pivots = true;
    private float thresholdOnLBsPerObjForSeqScan;
    protected int objBeforeSeqScan;
    private final GroundTruthEvaluator bruteForceAlg;

    protected final AbstractPtolemaicBasedFiltering filter;
    protected final List<T> pivotsData;
    protected final float[][] poDists;
    protected final Map<Comparable, Integer> rowHeaders;
    protected final AbstractDistanceFunction<T> df;
    protected final ConcurrentHashMap<Object, float[][]> qpMultipliedByCoefCached = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<Object, int[]> qPivotArraysCached;

    private final Set<String> qSkip = new HashSet<>();

    public KNNSearchWithPtolemaicFiltering(AbstractSearchSpace<T> searchSpace, AbstractPtolemaicBasedFiltering ptolemaicFilter, List<Object> pivots, float[][] poDists, Map<Comparable, Integer> rowHeaders, AbstractDistanceFunction<T> df) {
        this.filter = ptolemaicFilter;
        if (ptolemaicFilter instanceof PtolemaicFiltering) {
            PtolemaicFiltering cast = (PtolemaicFiltering) ptolemaicFilter;
            query_dynamic_pivots = cast.isQueryDynamicPivotPairs();
        }
        this.pivotsData = searchSpace.getDataOfObjects(pivots);
        this.poDists = poDists;
        this.df = df;
        this.rowHeaders = rowHeaders;
        this.qPivotArraysCached = new ConcurrentHashMap<>();
        this.bruteForceAlg = new GroundTruthEvaluator(df);
        this.objBeforeSeqScan = -1;
        this.thresholdOnLBsPerObjForSeqScan = 0;
    }

    @Override
    public TreeSet<Map.Entry<Comparable, Float>> completeKnnSearch(AbstractSearchSpace<T> searchSpace, Object q, int k, Iterator<Object> objects, Object... params) {
        long t = -System.currentTimeMillis();
        TreeSet<Map.Entry<Comparable, Float>> ret = params.length == 0 || params[0] == null ? new TreeSet<>(new Tools.MapByFloatValueComparator()) : (TreeSet<Map.Entry<Comparable, Float>>) params[0];
        Comparable qId = searchSpace.getIDOfObject(q);
        if (qSkip.contains(qId)) {
            bruteForceAlg.resetDistComps(qId);
            ret = bruteForceAlg.completeKnnSearch(searchSpace, q, k, objects, ret);
            t += System.currentTimeMillis();
            incTime(qId, t);
            incDistsComps(qId, bruteForceAlg.getDistCompsForQuery(qId));
            return ret;
        }
        long lbChecked = 0;
        T qData = searchSpace.getDataOfObject(q);

        float[][] qpDistMultipliedByCoefForPivots = qpMultipliedByCoefCached.get(qId);
        if (qpDistMultipliedByCoefForPivots == null) {
            qpDistMultipliedByCoefForPivots = computeqpDistMultipliedByCoefForPivots(qData, pivotsData, df, filter);
            qpMultipliedByCoefCached.put(qId, qpDistMultipliedByCoefForPivots);
        }
        int[] pivotArrays = qPivotArraysCached.get(qId);
        if (pivotArrays == null) {
            if (query_dynamic_pivots) {
                pivotArrays = identifyExtremePivotPairs(qpDistMultipliedByCoefForPivots, qpDistMultipliedByCoefForPivots.length);
            } else {
                pivotArrays = identifyRandomPivotPairs(qpDistMultipliedByCoefForPivots.length);
            }
            qPivotArraysCached.put(qId, pivotArrays);
        }
        int distComps = 0;
        float range = adjustAndReturnSearchRadiusAfterAddingOne(ret, k, Float.MAX_VALUE);
        int oIdx, p1Idx, p2Idx, p;
        float distP1O, distP2O, distP2Q, distQP1, lowerBound, distance;
        float[] poDistsArray;
        Object o;
        Comparable oId;
        T oData;
        int oCounter = -k;
        objectsLoop:
        while (objects.hasNext()) {
            oCounter++;
            if (oCounter == objBeforeSeqScan && thresholdOnLBsPerObjForSeqScan > 0) {
                long avg = lbChecked / oCounter;
                if (avg >= thresholdOnLBsPerObjForSeqScan) {
                    ret = bruteForceAlg.completeKnnSearch(searchSpace, q, k, objects, ret);
                    t += System.currentTimeMillis();
                    incTime(qId, t);
                    incDistsComps(qId, bruteForceAlg.getDistCompsForQuery(qId) + distComps);
                    incAdditionalParam(qId, lbChecked, 0);
                    qSkip.add(qId.toString());
                    return ret;
                }
            }
            o = objects.next();
            oId = searchSpace.getIDOfObject(o);
            if (range < Float.MAX_VALUE) {
                oIdx = rowHeaders.get(oId);
                poDistsArray = poDists[oIdx];
                for (p = 0; p < pivotArrays.length; p += 2) {
                    p1Idx = pivotArrays[p];
                    p2Idx = pivotArrays[p + 1];
                    distP1O = poDistsArray[p1Idx];
                    distP2O = poDistsArray[p2Idx];
                    distP2Q = qpDistMultipliedByCoefForPivots[p2Idx][p1Idx];
                    distQP1 = qpDistMultipliedByCoefForPivots[p1Idx][p2Idx];
                    lowerBound = filter.lowerBound(distP2O, distQP1, distP1O, distP2Q);
                    if (lowerBound > range) {
                        lbChecked += p / 2 + 1;
                        continue objectsLoop;
                    }
                }
                lbChecked += p / 2;
            }
            distComps++;
            oData = searchSpace.getDataOfObject(o);
            distance = df.getDistance(qData, oData);
            if (distance < range) {
                ret.add(new AbstractMap.SimpleEntry<>(oId, distance));
                range = adjustAndReturnSearchRadiusAfterAddingOne(ret, k, Float.MAX_VALUE);
            }
        }
        t += System.currentTimeMillis();
        System.err.println(qId + ": " + t + " ms ");
        incTime(qId, t);
        incDistsComps(qId, distComps);
        incAdditionalParam(qId, lbChecked, 0);
        return ret;
    }

    @Override
    public List<Comparable> candSetKnnSearch(AbstractSearchSpace<T> searchSpace, Object queryObject, int k, Iterator<Object> objects, Object... additionalParams) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getResultName() {
        String ret = filter.getTechFullName() + "_" + pivotsData.size() + "LB";
        if (thresholdOnLBsPerObjForSeqScan > 0) {
            ret += "_" + thresholdOnLBsPerObjForSeqScan + "perc_" + objBeforeSeqScan + "objMem";
        }
        if (!query_dynamic_pivots) {
            ret += "_random_pivots";
        }
        return ret;
    }

    public static <T> float[][] computeqpDistMultipliedByCoefForPivots(T qData, List<T> pivotsData, AbstractDistanceFunction<T> df, AbstractPtolemaicBasedFiltering filter) {
        float[][] ret = new float[pivotsData.size()][pivotsData.size()];
        for (int i = 0; i < pivotsData.size(); i++) {
            T pData = pivotsData.get(i);
            float qp1Dist = df.getDistance(qData, pData);
            for (int j = 0; j < pivotsData.size(); j++) {
                float coefLBP1P2 = filter.getCoefPivotPivotForLB(i, j);
                ret[i][j] = qp1Dist * coefLBP1P2;
            }
        }
        return ret;
    }

    private int[] identifyFirstPivotPairs(float[][] coefs, int size) {
        int[] ret = new int[size * 2];
        int pivotCount = coefs.length;
        for (int i = 0; i < size; i++) {
            ret[2 * i] = i;
            ret[2 * i + 1] = (i + 1) % pivotCount;
        }
        return ret;
    }

    private static final Random rand = new Random();

    public static int[] identifyRandomPivotPairs(int size) {
        int[] ret = new int[size * 2];
        for (int i = 0; i < size; i++) {
            ret[2 * i] = rand.nextInt(size);
            ret[2 * i + 1] = rand.nextInt(size);
        }
        return ret;
    }

    public static int[] identifyExtremePivotPairs(float[][] qpDistMultipliedByCoefForPivots, int size) {
        TreeSet<Map.Entry<Integer, Float>> sorted = new TreeSet<>(new Tools.MapByFloatValueComparator<>());
        float a, b, value;
        float radius = Float.MAX_VALUE;
        int i, j, idx;
        for (i = 0; i < qpDistMultipliedByCoefForPivots.length - 1; i++) {
            for (j = i + 1; j < qpDistMultipliedByCoefForPivots.length; j++) {
                a = qpDistMultipliedByCoefForPivots[i][j];
                b = qpDistMultipliedByCoefForPivots[j][i];
                if (a > b) {
                    value = b;
                    b = a;
                    a = value;
                }
                value = a / b;
                if (value == 0) {
                    continue;
                }
                if (sorted.size() < size) {
                    sorted.add(new AbstractMap.SimpleEntry<>(i * qpDistMultipliedByCoefForPivots.length + j, value));
                } else {
                    if (value < radius) {
                        sorted.add(new AbstractMap.SimpleEntry<>(i * qpDistMultipliedByCoefForPivots.length + j, value));
                        sorted.remove(sorted.last());
                        radius = sorted.last().getValue();
                    }
                }
            }
        }
        int[] ret = new int[size * 2];
        Iterator<Map.Entry<Integer, Float>> it = sorted.iterator();

        for (idx = 0; idx < ret.length; idx += 2) {
            Map.Entry<Integer, Float> entry = it.next();
            i = entry.getKey();
            j = i % qpDistMultipliedByCoefForPivots.length;
            i -= j;
            i = i / qpDistMultipliedByCoefForPivots.length;
            ret[idx] = i;
            ret[idx + 1] = j;
        }
        return ret;
    }

    public void setThresholdOnLBsPerObjForSeqScan(float thresholdOnLBsPerObjForSeqScan) {
        this.thresholdOnLBsPerObjForSeqScan = thresholdOnLBsPerObjForSeqScan;
    }

    public void setObjBeforeSeqScan(int objBeforeSeqScan) {
        this.objBeforeSeqScan = objBeforeSeqScan;
    }

}
