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
import vm.metricSpace.distance.bounding.twopivots.TwoPivotsFilter;
import static vm.search.algorithm.impl.KNNSearchWithOnePivotFiltering.SORT_PIVOTS;

/**
 * takes pivot pairs in a linear way, i.e., [0], [1], then [2], [3], etc.
 *
 * @author Vlada
 * @param <T>
 */
public class KNNSearchWithTwoPivotFiltering<T> extends SearchingAlgorithm<T> {

    private static final Logger LOG = Logger.getLogger(KNNSearchWithTwoPivotFiltering.class.getName());
    public static boolean PRINT_DETAILS = true;

    private final TwoPivotsFilter filter;
    private final List<T> pivotsData;
    private final float[][] poDists;
    private final Map<Object, Integer> rowHeaders;
    private final float[][] pivotPivotDists;
    private final DistanceFunctionInterface<T> df;

    private final int pivotsEnd;

    private final ConcurrentHashMap<Object, AtomicLong> lbCheckedForQ;

    public KNNSearchWithTwoPivotFiltering(AbstractMetricSpace<T> metricSpace, TwoPivotsFilter filter, List<Object> pivots, float[][] poDists, Map<Object, Integer> rowHeaders, Map<Object, Integer> columnHeaders, float[][] pivotPivotDists, DistanceFunctionInterface<T> df) {
        this.filter = filter;
        this.pivotsData = metricSpace.getDataOfMetricObjects(pivots);// correct!
        this.poDists = poDists;
        this.pivotPivotDists = pivotPivotDists;
        this.df = df;
        this.rowHeaders = rowHeaders;
        this.lbCheckedForQ = new ConcurrentHashMap();
        pivotsEnd = SORT_PIVOTS ? (int) Math.ceil(Math.sqrt(pivotsData.size())) : pivots.size() - 1;
        checkOrdersOfPivots(pivots, columnHeaders, metricSpace);
    }

//    public static long tLB = 0;
//    public static long oSearch = 0;
//    public static long tDistComps = 0;

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
        int[] p2Idxs = SORT_PIVOTS ? getP2ToCheck(pivotsEnd, pivotPermutation) : null;
        objectsLoop:
        while (objects.hasNext()) {
//            oSearch -= System.currentTimeMillis();
            Object o = objects.next();
            Object oId = metricSpace.getIDOfMetricObject(o);
            int oIdx = rowHeaders.get(oId);
            T oData = metricSpace.getDataOfMetricObject(o);
//            oSearch += System.currentTimeMillis();
            float range = adjustAndReturnSearchRadius(ret, k);
//            float distanceCheck = df.getDistance(qData, oData);;
            if (range < Float.MAX_VALUE) {
//                tLB -= System.currentTimeMillis();
                for (int p = 0; p < pivotsEnd; p++) {
                    int p1Idx = SORT_PIVOTS ? pivotPermutation[p] : p;
                    if (!SORT_PIVOTS) {
                        p2Idxs = new int[]{p + 1};
                    }
                    for (int p2Idx : p2Idxs) {
                        float distP1P2 = pivotPivotDists[p1Idx][p2Idx];
                        float distP2O = poDists[oIdx][p2Idx];
                        float distQP1 = qpDists[p1Idx];
                        float distP1O = poDists[oIdx][p1Idx];
                        float distP2Q = qpDists[p2Idx];
                        float lowerBound = filter.lowerBound(distP1P2, distP2O, distQP1, distP1O, distP2Q, p1Idx, p2Idx, range);
                        lbChecked++;
//                    if (distanceCheck < lowerBound) {
//                        PRINT_DETAILS = false;
////                        System.out.print("XXX range;" + range + ";realDist;" + distanceCheck + ";");
//                        lowerBound = filter.lowerBound(distP1P2, distP2O, distQP1, distP1O, distP2Q, p1ID, p2ID);
//                    }
                        if (lowerBound > range) {
//                            tLB += System.currentTimeMillis();
                            continue objectsLoop;
                        }
//                        if (CHECK_ALSO_UB) {
//                            float upperBound = filter.upperBound(distP1P2, distP2O, distQP1, distP1O, distP2Q, p1Idx, p2Idx, range);
//                            if (upperBound < range) {
//                                float distance = df.getDistance(qData, oData);
//                                ret.add(new AbstractMap.SimpleEntry<>(oId, distance));
//                                continue objectsLoop;
//                            }
//                        }
                    }
                }
//                tLB += System.currentTimeMillis();
            }
            incDistsComps(qId);
//            tDistComps -= System.currentTimeMillis();
            float distance = df.getDistance(qData, oData);
//            tDistComps += System.currentTimeMillis();
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

    private int[] getP2ToCheck(int pivotsEnd, int[] pivotPermutation) {
        int[] ret = new int[pivotsEnd];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = pivotPermutation[pivotPermutation.length - i - 1];
        }
        return ret;
    }

}
