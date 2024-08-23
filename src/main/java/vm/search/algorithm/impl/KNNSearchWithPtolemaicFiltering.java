/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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
import java.util.concurrent.atomic.AtomicLong;
import vm.datatools.Tools;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.metricSpace.distance.bounding.twopivots.AbstractPtolemaicBasedFiltering;
import vm.metricSpace.distance.bounding.twopivots.impl.PtolemaicFiltering;
import vm.search.algorithm.SearchingAlgorithm;

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
    private final List<T> pivotsData;
    protected final float[][] poDists;
    protected final Map<Comparable, Integer> rowHeaders;
    protected final DistanceFunctionInterface<T> df;
    private final ConcurrentHashMap<Object, AtomicLong> lbCheckedForQ;
    protected final ConcurrentHashMap<Object, float[][]> qpMultipliedByCoefCached = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<Object, int[]> qPivotArraysCached;

    private final Set<String> qSkip = new HashSet<>();

    public KNNSearchWithPtolemaicFiltering(AbstractMetricSpace<T> metricSpace, AbstractPtolemaicBasedFiltering ptolemaicFilter, List<Object> pivots, float[][] poDists, Map<Comparable, Integer> rowHeaders, DistanceFunctionInterface<T> df) {
        this.filter = ptolemaicFilter;
        if (ptolemaicFilter instanceof PtolemaicFiltering) {
            PtolemaicFiltering cast = (PtolemaicFiltering) ptolemaicFilter;
            query_dynamic_pivots = cast.getQueryDynamicPivotPairs();
        }
        this.pivotsData = metricSpace.getDataOfMetricObjects(pivots);
        this.poDists = poDists;
        this.df = df;
        this.rowHeaders = rowHeaders;
        this.lbCheckedForQ = new ConcurrentHashMap();
        this.qPivotArraysCached = new ConcurrentHashMap<>();
        this.bruteForceAlg = new GroundTruthEvaluator(df);
        this.objBeforeSeqScan = -1;
        this.thresholdOnLBsPerObjForSeqScan = 0;
    }

    @Override
    public TreeSet<Map.Entry<Comparable, Float>> completeKnnSearch(AbstractMetricSpace<T> metricSpace, Object q, int k, Iterator<Object> objects, Object... params) {
        long t = -System.currentTimeMillis();
        TreeSet<Map.Entry<Comparable, Float>> ret = params.length == 0 ? new TreeSet<>(new Tools.MapByFloatValueComparator()) : (TreeSet<Map.Entry<Comparable, Float>>) params[0];
        Comparable qId = metricSpace.getIDOfMetricObject(q);
        if (qSkip.contains(qId)) {
            bruteForceAlg.resetDistComps(qId);
            ret = bruteForceAlg.completeKnnSearch(metricSpace, q, k, objects, ret);
            t += System.currentTimeMillis();
            incTime(qId, t);
            incDistsComps(qId, bruteForceAlg.getDistCompsForQuery(qId));
            return ret;
        }
        long lbChecked = 0;
        T qData = metricSpace.getDataOfMetricObject(q);

        float[][] qpDistMultipliedByCoefForPivots = qpMultipliedByCoefCached.get(qId);
        if (qpDistMultipliedByCoefForPivots == null) {
            qpDistMultipliedByCoefForPivots = computeqpDistMultipliedByCoefForPivots(qData);
            qpMultipliedByCoefCached.put(qId, qpDistMultipliedByCoefForPivots);
        }
        int[] pivotArrays = qPivotArraysCached.get(qId);
        if (pivotArrays == null) {
            if (query_dynamic_pivots) {
                pivotArrays = identifyExtremePivotPairs(qpDistMultipliedByCoefForPivots, qpDistMultipliedByCoefForPivots.length);
            } else {
                pivotArrays = identifyRandomPivotPairs(qpDistMultipliedByCoefForPivots, qpDistMultipliedByCoefForPivots.length);
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
                    ret = bruteForceAlg.completeKnnSearch(metricSpace, q, k, objects, ret);
                    t += System.currentTimeMillis();
                    incTime(qId, t);
                    incDistsComps(qId, bruteForceAlg.getDistCompsForQuery(qId) + distComps);
                    incLBChecked(qId, lbChecked);
                    qSkip.add(qId.toString());
                    return ret;
                }
            }
            o = objects.next();
            oId = metricSpace.getIDOfMetricObject(o);
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
            oData = metricSpace.getDataOfMetricObject(o);
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
        incLBChecked(qId, lbChecked);
        return ret;
    }

    @Override
    public List<Comparable> candSetKnnSearch(AbstractMetricSpace<T> metricSpace, Object queryObject, int k, Iterator<Object> objects, Object... additionalParams) {
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

    protected void incLBChecked(Object qId, long lbChecked) {
        AtomicLong ai = lbCheckedForQ.get(qId);
        if (ai != null) {
            ai.addAndGet(lbChecked);
        } else {
            lbCheckedForQ.put(qId, new AtomicLong(lbChecked));
        }
    }

    @Override
    public Map<Object, AtomicLong>[] getAddditionalStats() {
        return new Map[]{lbCheckedForQ};
    }

    protected float[][] computeqpDistMultipliedByCoefForPivots(T qData) {
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

    private int[] identifyRandomPivotPairs(float[][] coefs, int size) {
        int[] ret = new int[size * 2];
        int pivotCount = coefs.length;
        for (int i = 0; i < size; i++) {
            ret[2 * i] = rand.nextInt(pivotCount);
            ret[2 * i + 1] = rand.nextInt(pivotCount);
        }
        return ret;
    }

    protected int[] identifyExtremePivotPairs(float[][] coefs, int size) {
        TreeSet<Map.Entry<Integer, Float>> sorted = new TreeSet<>(new Tools.MapByFloatValueComparator<>());
        float a, b, value;
        float radius = Float.MAX_VALUE;
        int i, j, idx;
        for (i = 0; i < coefs.length - 1; i++) {
            for (j = i + 1; j < coefs.length; j++) {
                a = coefs[i][j];
                b = coefs[j][i];
                if (a > b) {
                    value = b;
                    b = a;
                    a = value;
                }
                value = a / b;
                if (sorted.size() < size) {
                    sorted.add(new AbstractMap.SimpleEntry<>(i * coefs.length + j, value));
                } else {
                    if (value < radius) {
                        sorted.add(new AbstractMap.SimpleEntry<>(i * coefs.length + j, value));
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
            j = i % coefs.length;
            i -= j;
            i = i / coefs.length;
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
