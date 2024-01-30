package vm.search.algorithm.impl;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.ToolsMetricDomain;
import vm.search.algorithm.SearchingAlgorithm;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.metricSpace.distance.bounding.onepivot.OnePivotFilter;

/**
 * takes pivot pairs in a linear way, i.e., [0], [1], then [2], [3], etc.
 *
 * @author Vlada
 * @param <T>
 */
public class KNNSearchWithOnePivotFiltering<T> extends SearchingAlgorithm<T> {

    private static final Logger LOG = Logger.getLogger(KNNSearchWithTwoPivotFiltering.class.getName());
    public static final Boolean CHECK_ALSO_UB = false;
    public static final Boolean SORT_PIVOTS = true;

    private final OnePivotFilter filter;
    private final List<T> pivotsData;
    private final float[][] poDists;
    private final Map<Object, Integer> rowHeaders;
    private final DistanceFunctionInterface<T> df;

    private final ConcurrentHashMap<Object, AtomicLong> lbCheckedForQ;

    public KNNSearchWithOnePivotFiltering(AbstractMetricSpace<T> metricSpace, OnePivotFilter filter, List<Object> pivots, float[][] poDists, Map<Object, Integer> rowHeaders, Map<Object, Integer> columnHeaders, DistanceFunctionInterface<T> df) {
        this.filter = filter;
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
        TreeSet<Map.Entry<Object, Float>> currAnswer = null;
        if (params.length > 0) {
            currAnswer = (TreeSet<Map.Entry<Object, Float>>) params[0];
        }
        T qData = metricSpace.getDataOfMetricObject(q);
        Object qId = metricSpace.getIDOfMetricObject(q);
        float[] qpDists = qpDistsCached.get(qId);
        if (qpDists == null) {
            qpDists = new float[pivotsData.size()];
            for (int i = 0; i < qpDists.length; i++) {
                T pData = pivotsData.get(i);
                qpDists[i] = df.getDistance(qData, pData);
            }
            qpDistsCached.put(qId, qpDists);
        }
        int[] pivotPermutation = null;
        if (SORT_PIVOTS) {
            pivotPermutation = pivotPermutationCached.get(qId);
            if (pivotPermutation == null) {
                pivotPermutation = ToolsMetricDomain.getPivotPermutationIndexes(metricSpace, df, pivotsData, qData, -1);
                pivotPermutationCached.put(qId, pivotPermutation);
            }
        }
        TreeSet<Map.Entry<Object, Float>> ret = currAnswer == null ? new TreeSet<>(new Tools.MapByValueComparator()) : currAnswer;
        objectsLoop:
        while (objects.hasNext()) {
            Object o = objects.next();
            Object oId = metricSpace.getIDOfMetricObject(o);
            int oIdx = rowHeaders.get(oId.toString());
            T oData = metricSpace.getDataOfMetricObject(o);
            float range = adjustAndReturnSearchRadius(ret, k);
//            float maxLB = 0;
//            float minUB = Float.MAX_VALUE;
            if (range < Float.MAX_VALUE && range > 0) {
                for (int p = 0; p < pivotsData.size(); p++) {
                    int pIdx = SORT_PIVOTS ? pivotPermutation[p] : p;
                    float distQP = qpDists[pIdx];
                    float distPO = poDists[oIdx][pIdx];
                    float lowerBound = filter.lowerBound(distQP, distPO, pIdx);
                    lbChecked++;
//                    System.out.print("XXX range;" + range + ";realDist;" + df.getDistance(qData, oData) + ";lower bound;" + lowerBound);
//                    maxLB = Math.max(maxLB, lowerBound);
                    if (lowerBound > range) {
                        continue objectsLoop;
                    }
//                    if (CHECK_ALSO_UB) {
//                        float upperBound = filter.upperBound(distQP, distPO, pIdx);
//                        if (upperBound < range) {
//                            float distance = df.getDistance(qData, oData);
//                            ret.add(new AbstractMap.SimpleEntry<>(oId, distance));
//                            continue objectsLoop;
//                        }
//                    }
                }
            }
            incDistsComps(qId);
            float distance = df.getDistance(qData, oData);
            ret.add(new AbstractMap.SimpleEntry<>(oId, distance));
        }
        adjustAndReturnSearchRadius(ret, k);
        t += System.currentTimeMillis();
        incTime(qId, t);
        incLBChecked(qId, lbChecked);
//        LOG.log(Level.INFO, "Evaluated query {2} using {0} dist comps. Time: {1}", new Object[]{getDistCompsForQuery(qId), getTimeOfQuery(qId), qId.toString()});
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

}
