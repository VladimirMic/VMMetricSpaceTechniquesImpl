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
import vm.searchSpace.distance.DistanceFunctionInterface;
import vm.searchSpace.distance.bounding.twopivots.AbstractTwoPivotsFilter;
import static vm.search.algorithm.impl.KNNSearchWithOnePivotFiltering.SORT_PIVOTS;

/**
 * If !KNNSearchWithOnePivotFiltering.SORT_PIVOTS, then takes pivot pairs in a
 * linear way, i.e., [0], [1], then [1], [2], etc.
 *
 * @author Vlada
 * @param <T>
 */
public class KNNSearchWithGenericTwoPivotFiltering<T> extends SearchingAlgorithm<T> {

    private final AbstractTwoPivotsFilter filter;
    private final List<T> pivotsData;
    private final float[][] poDists;
    private final Map<Comparable, Integer> rowHeaders;
    private final float[][] pivotPivotDists;
    private final DistanceFunctionInterface<T> df;

    private final int pivotsEndSmallDists;
    private final int pivotsEndBigDists;

    public KNNSearchWithGenericTwoPivotFiltering(AbstractSearchSpace<T> searchSpace, AbstractTwoPivotsFilter filter, List<Object> pivots, float[][] poDists, Map<Comparable, Integer> rowHeaders, float[][] pivotPivotDists, DistanceFunctionInterface<T> df) {
        this.filter = filter;
        this.pivotsData = searchSpace.getDataOfObjects(pivots);
        this.poDists = poDists;
        this.pivotPivotDists = pivotPivotDists;
        this.df = df;
        this.rowHeaders = rowHeaders;
        pivotsEndBigDists = 8;
        pivotsEndSmallDists = SORT_PIVOTS ? (pivotsData.size() / pivotsEndBigDists) : pivotsData.size() - 1;
    }

    @Override
    public TreeSet<Map.Entry<Comparable, Float>> completeKnnSearch(AbstractSearchSpace<T> searchSpace, Object q, int k, Iterator<Object> objects, Object... params) {
        long t = -System.currentTimeMillis();
        long lbChecked = 0;
        TreeSet<Map.Entry<Comparable, Float>> ret = params.length == 0 || params[0] == null ? new TreeSet<>(new Tools.MapByFloatValueComparator()) : (TreeSet<Map.Entry<Comparable, Float>>) params[0];
        T qData = searchSpace.getDataOfObject(q);
        Comparable qId = searchSpace.getIDOfObject(q);
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
        int[] p2Idxs = null;
        if (SORT_PIVOTS) {
            p2Idxs = new int[pivotsEndBigDists];
            for (int i = 0; i < p2Idxs.length; i++) {
                p2Idxs[i] = pivotPermutation[pivotPermutation.length - i - 1];
            }
        }
        int distComps = 0;
        int oIdx, p1Idx, p;
        float distP1O, distP2O, distP2Q, distQP1, distP1P2, lowerBound, distance;
        float range = adjustAndReturnSearchRadiusAfterAddingOne(ret, k, Float.MAX_VALUE);
        objectsLoop:
        while (objects.hasNext()) {
            Object o = objects.next();
            Comparable oId = searchSpace.getIDOfObject(o);
            T oData = searchSpace.getDataOfObject(o);
            oIdx = rowHeaders.get(oId);
            if (range < Float.MAX_VALUE) {
                for (p = 0; p < pivotsEndSmallDists; p++) {
                    p1Idx = SORT_PIVOTS ? pivotPermutation[p] : p;
                    if (!SORT_PIVOTS) {
                        p2Idxs = new int[]{p + 1};
                    }
                    for (int p2Idx : p2Idxs) {
                        distP1P2 = pivotPivotDists[p1Idx][p2Idx];
                        distP2O = poDists[oIdx][p2Idx];
                        distQP1 = qpDists[p1Idx];
                        distP1O = poDists[oIdx][p1Idx];
                        distP2Q = qpDists[p2Idx];
                        lowerBound = filter.lowerBound(distP1P2, distP2O, distQP1, distP1O, distP2Q, p1Idx, p2Idx, range);
                        lbChecked++;
                        if (lowerBound > range) {
                            continue objectsLoop;
                        }
                    }
                }
            }
            distComps++;
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
        System.err.println(qId + ": " + t + " ms ");
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
