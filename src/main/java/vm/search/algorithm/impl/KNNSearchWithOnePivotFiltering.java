package vm.search.algorithm.impl;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import vm.datatools.Tools;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.ToolsSpaceDomain;
import vm.search.algorithm.SearchingAlgorithm;
import vm.searchSpace.distance.AbstractDistanceFunction;
import vm.searchSpace.distance.bounding.onepivot.AbstractOnePivotFilter;

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
    private final AbstractDistanceFunction<T> df;

    public KNNSearchWithOnePivotFiltering(AbstractSearchSpace<T> searchSpace, AbstractOnePivotFilter filter, List<Object> pivots, float[][] poDists, Map<Object, Integer> rowHeaders, Map<Object, Integer> columnHeaders, AbstractDistanceFunction<T> df) {
        this.filter = filter;
        this.pivotsData = searchSpace.getDataOfObjects(pivots);
        this.poDists = poDists;
        this.df = df;
        this.rowHeaders = rowHeaders;
    }

    @Override
    public TreeSet<Map.Entry<Comparable, Float>> completeKnnSearch(AbstractSearchSpace<T> searchSpace, Object q, int k, Iterator<Object> objects, Object... params) {
        long t = -System.currentTimeMillis();
        long lbChecked = 0;
        TreeSet<Map.Entry<Comparable, Float>> ret = params.length == 0 || params[0] == null ? new TreeSet<>(new Tools.MapByFloatValueComparator()) : (TreeSet<Map.Entry<Comparable, Float>>) params[0];
        Comparable qId = searchSpace.getIDOfObject(q);
        T qData = searchSpace.getDataOfObject(q);
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
                pivotPermutation = ToolsSpaceDomain.getPivotPermutationIndexes(searchSpace, df, pivotsData, qData, -1);
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
            Comparable oId = searchSpace.getIDOfObject(o);
            if (range < Float.MAX_VALUE) {
                oIdx = rowHeaders.get(oId.toString());
                for (p = 0; p < pivotsData.size(); p++) {
                    pIdx = SORT_PIVOTS ? pivotPermutation[p] : p;
                    distQP = qpDists[pIdx];
                    distPO = poDists[oIdx][pIdx];
                    lowerBound = filter.lowerBound(distQP, distPO, pIdx);
                    if (lowerBound > range) {
                        lbChecked += p + 1;
                        continue objectsLoop;
                    }
                }
                lbChecked += p;
            }
            distComps++;
            T oData = searchSpace.getDataOfObject(o);
            distance = df.getDistance(qData, oData);
            if (distance < range) {
                ret.add(new AbstractMap.SimpleEntry<>(oId, distance));
                range = adjustAndReturnSearchRadiusAfterAddingOne(ret, k, Float.MAX_VALUE);
            }
        }
        t += System.currentTimeMillis();
        incTime(qId, t);
        incDistsComps(qId, distComps);
        incAdditionalParam(qId, lbChecked, 0);
        System.out.println(qId + ": " + t + " ms;" + distComps + " DC");
        return ret;
    }

    @Override
    public List<Comparable> candSetKnnSearch(AbstractSearchSpace<T> searchSpace, Object queryObject, int k, Iterator<Object> objects, Object... additionalParams) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getResultName() {
        return filter.getTechFullName();
    }

}
