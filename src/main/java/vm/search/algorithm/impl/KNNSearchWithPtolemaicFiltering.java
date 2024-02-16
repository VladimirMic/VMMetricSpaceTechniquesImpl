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
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.metricSpace.distance.bounding.twopivots.AbstractPtolemaicBasedFiltering;
import vm.search.algorithm.SearchingAlgorithm;

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

    private final ConcurrentHashMap<Object, AtomicLong> lbCheckedForQ;
    private final ConcurrentHashMap<Object, float[][]> qpMultipliedByCoefCached = new ConcurrentHashMap<>();

    public KNNSearchWithPtolemaicFiltering(AbstractMetricSpace<T> metricSpace, AbstractPtolemaicBasedFiltering ptolemaicFilter, List<Object> pivots, float[][] poDists, Map<Object, Integer> rowHeaders, Map<Object, Integer> columnHeaders, DistanceFunctionInterface<T> df) {
        this.filter = ptolemaicFilter;
        this.pivotsData = metricSpace.getDataOfMetricObjects(pivots);
        this.poDists = poDists;
        this.df = df;
        this.rowHeaders = rowHeaders;
        this.lbCheckedForQ = new ConcurrentHashMap();
        checkOrdersOfPivots(pivots, columnHeaders, metricSpace);
    }

    @Override
    public TreeSet<Map.Entry<Object, Float>> completeKnnSearch(AbstractMetricSpace<T> metricSpace, Object q, int k, Iterator<Object> objects, Object... params) {
        long t = -System.currentTimeMillis();
        long lbChecked = 0;
        TreeSet<Map.Entry<Object, Float>> ret = params.length == 0 ? new TreeSet<>(new Tools.MapByFloatValueComparator()) : (TreeSet<Map.Entry<Object, Float>>) params[0];
        T qData = metricSpace.getDataOfMetricObject(q);
        Object qId = metricSpace.getIDOfMetricObject(q);
        float[][] qpDistMultipliedByCoefForPivots = getOrComputeqpDistMultipliedByCoefForPivots(qpMultipliedByCoefCached, qId, qData);
        int[] pivotPairs = pivotPermutationCached.get(qId);
        if (pivotPairs == null) {
            pivotPairs = identifyExtremePivotPairs(qpDistMultipliedByCoefForPivots, pivotsData.size());
            pivotPermutationCached.put(qId, pivotPairs);
        }
        int distComps = 0;
        float range = adjustAndReturnSearchRadiusAfterAddingOne(ret, k);
        objectsLoop:
        while (objects.hasNext()) {
            Object o = objects.next();
            Object oId = metricSpace.getIDOfMetricObject(o);
            T oData = metricSpace.getDataOfMetricObject(o);
            int oIdx = rowHeaders.get(oId);
            if (range < Float.MAX_VALUE) {
                for (int p = 0; p < pivotPairs.length; p += 2) {
                    int p1Idx = pivotPairs[p];
                    int p2Idx = pivotPairs[p + 1];

                    float distP1O = poDists[oIdx][p1Idx];
                    float distP2O = poDists[oIdx][p2Idx];
                    float distP2Q = qpDistMultipliedByCoefForPivots[p2Idx][p1Idx];
                    float distQP1 = qpDistMultipliedByCoefForPivots[p1Idx][p2Idx];
                    float lowerBound = filter.lowerBound(distP2O, distQP1, distP1O, distP2Q);
                    lbChecked++;
                    if (lowerBound > range) {
                        continue objectsLoop;
                    }
                }
            }
            distComps++;
            float distance = df.getDistance(qData, oData);
            if (distance < range) {
                ret.add(new AbstractMap.SimpleEntry<>(oId, distance));
                range = adjustAndReturnSearchRadiusAfterAddingOne(ret, k);
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

    private int[] identifyExtremePivotPairs(float[][] coefs, int size) {
        TreeSet<Map.Entry<Integer, Float>> sorted = new TreeSet<>(new Tools.MapByFloatValueComparator<>());
        for (int i = 0; i < coefs.length - 1; i++) {
            for (int j = i + 1; j < coefs.length; j++) {
                float a = coefs[i][j];
                float b = coefs[j][i];
                float value = Math.min(a, b) / Math.max(a, b);
                sorted.add(new AbstractMap.SimpleEntry<>(i * coefs.length + j, value));
                if (sorted.size() > size) {
                    sorted.remove(sorted.last());
                }
            }
        }
        Iterator<Map.Entry<Integer, Float>> it = sorted.iterator();
        int[] ret = new int[size * 2];
        for (int i = 0; i < ret.length; i += 2) {
            Map.Entry<Integer, Float> entry = it.next();
            int row = entry.getKey();
            int column = row % coefs.length;
            row -= column;
            row = row / coefs.length;
            ret[i] = row;
            ret[i + 1] = column;
        }
        return ret;
    }

}
