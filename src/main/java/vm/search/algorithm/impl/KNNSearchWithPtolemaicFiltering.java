/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.search.algorithm.impl;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import vm.datatools.Tools;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.metricSpace.distance.bounding.twopivots.AbstractPtolemaicBasedFiltering;
import vm.search.algorithm.SearchingAlgorithm;
import static vm.search.algorithm.impl.KNNSearchWithOnePivotFiltering.SORT_PIVOTS;

/**
 *
 * @author Vlada
 * @param <T> type of object data
 */
public class KNNSearchWithPtolemaicFiltering<T> extends SearchingAlgorithm<T> {

    private final AbstractPtolemaicBasedFiltering filter;
    private final List<T> pivotsData;
    private final float[][] poDists;
    private final Map<Object, Integer> rowHeaders;
    private final DistanceFunctionInterface<T> df;

    private final int pivotsEndSmallDists;
    private final int pivotsEndBigDists;

    private final ConcurrentHashMap<Object, AtomicLong> lbCheckedForQ;
    private final ConcurrentHashMap<Object, float[][]> qpMultipliedByCoefCached = new ConcurrentHashMap<>();

    public KNNSearchWithPtolemaicFiltering(AbstractMetricSpace<T> metricSpace, AbstractPtolemaicBasedFiltering ptolemaicFilter, List<Object> pivots, float[][] poDists, Map<Object, Integer> rowHeaders, Map<Object, Integer> columnHeaders, DistanceFunctionInterface<T> df) {
        this.filter = ptolemaicFilter;
        this.pivotsData = metricSpace.getDataOfMetricObjects(pivots);
        this.poDists = poDists;
        this.df = df;
        this.rowHeaders = rowHeaders;
        this.lbCheckedForQ = new ConcurrentHashMap();
        pivotsEndBigDists = (int) (SORT_PIVOTS ? Math.ceil(Math.sqrt(pivotsData.size())) : pivotsData.size() - 1);
        pivotsEndSmallDists = SORT_PIVOTS ? pivotsEndBigDists : pivotsData.size() - 1;
        checkOrdersOfPivots(pivots, columnHeaders, metricSpace);
    }

    @Override
    public TreeSet<Map.Entry<Object, Float>> completeKnnSearch(AbstractMetricSpace<T> metricSpace, Object q, int k, Iterator<Object> objects, Object... params) {
        long t = -System.currentTimeMillis();
        long lbChecked = 0;
        TreeSet<Map.Entry<Object, Float>> ret = params.length == 0 ? new TreeSet<>(new Tools.MapByValueComparator()) : (TreeSet<Map.Entry<Object, Float>>) params[0];
        T qData = metricSpace.getDataOfMetricObject(q);
        Object qId = metricSpace.getIDOfMetricObject(q);

        float[][] qpDistMultipliedByCoefForPivots = getOrComputeqpDistMultipliedByCoefForPivots(qpMultipliedByCoefCached, qId, qData);

        int[] pivotPermutation = null;
        if (SORT_PIVOTS) {
            pivotPermutation = pivotPermutationCached.get(qId);
            if (pivotPermutation == null) {
                pivotPermutation = ToolsMetricDomain.getPivotPermutationIndexes(qpDistsCached.get(qId), -1);
                pivotPermutationCached.put(qId, pivotPermutation);
            }
        }
        int[] p2Idxs = null;
        if (SORT_PIVOTS) {
            p2Idxs = new int[pivotsEndBigDists];
            for (int i = 0; i < p2Idxs.length; i++) {
                p2Idxs[i] = pivotPermutation[pivotPermutation.length - i - 1];
            }
        }
        int distComps = 0;
        float range = adjustAndReturnSearchRadius(ret, k);
        objectsLoop:
        while (objects.hasNext()) {
            Object o = objects.next();
            Object oId = metricSpace.getIDOfMetricObject(o);
            T oData = metricSpace.getDataOfMetricObject(o);
            int oIdx = rowHeaders.get(oId);
            if (range < Float.MAX_VALUE) {
                for (int p = 0; p < pivotsEndSmallDists; p++) {
                    int p1Idx = SORT_PIVOTS ? pivotPermutation[p] : p;
                    if (!SORT_PIVOTS) {
                        p2Idxs = new int[]{p + 1};
                    }
                    for (int p2Idx : p2Idxs) {
                        float distP2O = poDists[oIdx][p2Idx];
                        float distQP1 = qpDistMultipliedByCoefForPivots[p1Idx][p2Idx];
                        float distP1O = poDists[oIdx][p1Idx];
                        float distP2Q = qpDistMultipliedByCoefForPivots[p2Idx][p1Idx];
//                tLB -= System.currentTimeMillis();
                        float lowerBound = filter.lowerBound(distP2O, distQP1, distP1O, distP2Q);
//                tLB += System.currentTimeMillis();
                        lbChecked++;
                        if (lowerBound > range) {
                            continue objectsLoop;
                        }
                    }
                }
            }
            distComps++;
            float distance = df.getDistance(qData, oData);
            if (distance < range) {
                ret.add(new AbstractMap.SimpleEntry<>(oId, distance));
                range = adjustAndReturnSearchRadius(ret, k);
            }
        }
        t += System.currentTimeMillis();
        incTime(qId, t);
        incDistsComps(qId, distComps);
        incLBChecked(qId, lbChecked);
        return ret;
    }

    @Override
    public List<Object> candSetKnnSearch(AbstractMetricSpace<T> metricSpace, Object queryObject, int k, Iterator<Object> objects, Object... additionalParams) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getResultName() {
        return filter.getTechFullName();
    }

    private void incLBChecked(Object qId, long lbChecked) {
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

    private float[][] getOrComputeqpDistMultipliedByCoefForPivots(ConcurrentHashMap<Object, float[][]> cache, Object qId, T qData) {
        float[][] ret = cache.get(qId);
        if (ret == null) {
            ret = new float[pivotsData.size()][pivotsData.size()];
            float[] qpDists = qpDistsCached.get(qId);
            if (qpDists == null) {
                qpDists = new float[pivotsData.size()];
                for (int i = 0; i < pivotsData.size(); i++) {
                    T pData = pivotsData.get(i);
                    qpDists[i] = df.getDistance(qData, pData);
                }
                qpDistsCached.put(qId, qpDists);
            }
            for (int i = 0; i < pivotsData.size(); i++) {
                float qp1Dist = qpDists[i];
                for (int j = 0; j < pivotsData.size(); j++) {
                    float coefLBP1P2 = filter.getCoefPivotPivotForLB(i, j);
                    ret[i][j] = qp1Dist * coefLBP1P2;
                }
            }
            cache.put(qId, ret);
        }
        return ret;
    }

}
