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
import vm.search.algorithm.SearchingAlgorithm;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.metricSpace.distance.bounding.onepivot.AbstractOnePivotFilter;

/**
 * takes pivot pairs in a linear way, i.e., [0], [1], then [2], [3], etc.
 *
 * @author Vlada
 * @param <T>
 */
public class KNNSearchWithOnePivotFiltering<T> extends SearchingAlgorithm<T> {

    public static final Boolean CHECK_ALSO_UB = false;
    public static final Boolean SORT_PIVOTS = false;

    private final AbstractOnePivotFilter filter;
    private final List<T> pivotsData;
    private final float[][] poDists;
    private final Map<Object, Integer> rowHeaders;
    private final DistanceFunctionInterface<T> df;

    private final ConcurrentHashMap<Object, AtomicLong> lbCheckedForQ;

    public KNNSearchWithOnePivotFiltering(AbstractMetricSpace<T> metricSpace, AbstractOnePivotFilter filter, List<Object> pivots, float[][] poDists, Map<Object, Integer> rowHeaders, Map<Object, Integer> columnHeaders, DistanceFunctionInterface<T> df) {
        this.filter = filter;
        this.pivotsData = metricSpace.getDataOfMetricObjects(pivots);
        this.poDists = poDists;
        this.df = df;
        this.rowHeaders = rowHeaders;
        this.lbCheckedForQ = new ConcurrentHashMap();
    }

    @Override
    public TreeSet<Map.Entry<Comparable, Float>> completeKnnSearch(AbstractMetricSpace<T> metricSpace, Object q, int k, Iterator<Object> objects, Object... params) {
        long t = -System.currentTimeMillis();
        long lbChecked = 0;
        TreeSet<Map.Entry<Comparable, Float>> ret = params.length == 0 ? new TreeSet<>(new Tools.MapByFloatValueComparator()) : (TreeSet<Map.Entry<Comparable, Float>>) params[0];
        Comparable qId = metricSpace.getIDOfMetricObject(q);
        T qData = metricSpace.getDataOfMetricObject(q);
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
            pivotPermutation = qPivotPermutationCached.get(qId);
            if (pivotPermutation == null) {
                pivotPermutation = ToolsMetricDomain.getPivotPermutationIndexes(metricSpace, df, pivotsData, qData, -1);
                qPivotPermutationCached.put(qId, pivotPermutation);
            }
        }
        int distComps = 0;
        float range = adjustAndReturnSearchRadiusAfterAddingOne(ret, k, Float.MAX_VALUE);
        int oIdx, pIdx, p;
        float distPO, distQP, lowerBound, distance;
        objectsLoop:
        while (objects.hasNext()) {
            Object o = objects.next();
            Comparable oId = metricSpace.getIDOfMetricObject(o);
            if (range < Float.MAX_VALUE) {
                oIdx = rowHeaders.get(oId.toString());
                for (p = 0; p < pivotsData.size(); p++) {
                    pIdx = SORT_PIVOTS ? pivotPermutation[p] : p;
                    distQP = qpDists[pIdx];
                    distPO = poDists[oIdx][pIdx];
                    lowerBound = filter.lowerBound(distQP, distPO, pIdx);
                    if (lowerBound > range) {
                        lbChecked += p + 1;
                        T oData = metricSpace.getDataOfMetricObject(o);
                        if (df.getDistance(qData, oData) > lowerBound) {
                            String s = "Achtung";
                        }
                        continue objectsLoop;
                    }
                }
                lbChecked += p;
            }
            distComps++;
            T oData = metricSpace.getDataOfMetricObject(o);
            distance = df.getDistance(qData, oData);
            if (distance < range) {
                ret.add(new AbstractMap.SimpleEntry<>(oId, distance));
                range = adjustAndReturnSearchRadiusAfterAddingOne(ret, k, Float.MAX_VALUE);
            }
        }
        t += System.currentTimeMillis();
        incTime(qId, t);
        incDistsComps(qId, distComps);
        incLBChecked(qId, lbChecked);
        System.out.println(qId + ": " + t + " ms");
        return ret;
    }

    @Override
    public List<Comparable> candSetKnnSearch(AbstractMetricSpace<T> metricSpace, Object queryObject, int k, Iterator<Object> objects, Object... additionalParams) {
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
