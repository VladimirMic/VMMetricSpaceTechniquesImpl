package vm.search.algorithm.impl;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.ToolsMetricDomain;
import vm.search.algorithm.SearchingAlgorithm;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.metricSpace.distance.bounding.twopivots.TwoPivotsFilter;
import static vm.search.algorithm.impl.KNNSearchWithOnePivotFiltering.CHECK_ALSO_UB;
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
    private final Map<String, Integer> rowHeaders;
    private final float[][] pivotPivotDists;
    private final DistanceFunctionInterface<T> df;
    private final boolean pivotPairsFromFilter;

    private final ConcurrentHashMap<Object, AtomicLong> lbCheckedForQ;

    public KNNSearchWithTwoPivotFiltering(AbstractMetricSpace<T> metricSpace, TwoPivotsFilter filter, List<Object> pivots, float[][] poDists, Map<String, Integer> rowHeaders, Map<String, Integer> columnHeaders, float[][] pivotPivotDists, DistanceFunctionInterface<T> df) {
        this(metricSpace, filter, pivots, poDists, rowHeaders, columnHeaders, pivotPivotDists, df, false);
    }

    public KNNSearchWithTwoPivotFiltering(AbstractMetricSpace<T> metricSpace, TwoPivotsFilter filter, List<Object> pivots, float[][] poDists, Map<String, Integer> rowHeaders, Map<String, Integer> columnHeaders, float[][] pivotPivotDists, DistanceFunctionInterface<T> df, boolean createAllPivotPairs) {
        this.filter = filter;
        this.pivotsData = metricSpace.getDataOfMetricObjects(pivots);// correct!
        this.pivotPairsFromFilter = createAllPivotPairs;
        if (createAllPivotPairs) {
            pivots = Tools.createAllPairs(pivots);
        }
        this.poDists = poDists;
        this.pivotPivotDists = pivotPivotDists;
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
        float[] qpDists = new float[pivotsData.size()];
        for (int i = 0; i < qpDists.length; i++) {
            T pData = pivotsData.get(i);
            qpDists[i] = df.getDistance(qData, pData);
        }
        int[] pivotPermutation = null;
        if (SORT_PIVOTS) {
            pivotPermutation = ToolsMetricDomain.getPivotPermutationIndexes(metricSpace, df, pivotsData, qData, -1);
        }
        TreeSet<Map.Entry<Object, Float>> ret = currAnswer == null ? new TreeSet<>(new Tools.MapByValueComparator()) : currAnswer;
        int step = pivotPairsFromFilter ? 2 : 1;
        while (objects.hasNext()) {
            boolean skip = false;
            Object o = objects.next();
            Object oId = metricSpace.getIDOfMetricObject(o);
            if (!rowHeaders.containsKey(oId.toString())) {
                throw new RuntimeException("Precomputed distances dost not contain object " + oId.toString());
            }
            int oIdx = rowHeaders.get(oId.toString());
            T oData = metricSpace.getDataOfMetricObject(o);
            float range = adjustAndReturnSearchRadius(ret, k);
//            float distanceCheck = df.getDistance(qData, oData);;
            int pivotPairsChecked = 0;
            if (range < Float.MAX_VALUE) {
                pivotsCheck:
                for (int p = 0; p < pivotsData.size(); p += step) {
                    int p1Idx = pivotPermutation == null ? p : pivotPermutation[p];
                    int[] p2Idxs = pivotPermutation == null ? new int[]{p1Idx + 1} : getP2ToCheck(p, pivotPermutation);
                    if (p2Idxs.length == 0) {
                        continue;
                    }
                    if (pivotPermutation == null && p2Idxs[0] == pivotsData.size()) {
                        p2Idxs[0] = 0;
                    }
                    for (int p2Idx : p2Idxs) {
                        if (pivotPairsChecked == pivotsData.size()) {
                            break pivotsCheck;
                        }
                        pivotPairsChecked++;
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
//                        PRINT_DETAILS = true;
//                        if (PRINT_DETAILS) {
//                            System.out.println("Skipped. Radius: " + range + ", lb: " + lowerBound);
//                            filter.lowerBound(distP1P2, distP2O, distQP1, distP1O, distP2Q, p1ID, p2ID);
//                        }
//                        PRINT_DETAILS = false;
                            skip = true;
                            break pivotsCheck;
                        }
                        float upperBound = CHECK_ALSO_UB ? filter.upperBound(distP1P2, distP2O, distQP1, distP1O, distP2Q, p1Idx, p2Idx, range) : Float.MAX_VALUE;
//                    if (distanceCheck > upperBound) {
//                        PRINT_DETAILS = false;
////                        System.out.print("XXX range;" + range + ";realDist;" + distanceCheck + ";");
//                        upperBound = filter.upperBound(distP1P2, distP2O, distQP1, distP1O, distP2Q, p1ID, p2ID);
//                    }
                        if (upperBound < range) {
                            skip = true;
                            float distance = df.getDistance(qData, oData);
                            ret.add(new AbstractMap.SimpleEntry<>(oId, distance));
                            break pivotsCheck;
                        }
                    }
                }
            }
            if (skip) {
                continue;
            }
            incDistsComps(qId);
            float distance = df.getDistance(qData, oData);
            ret.add(new AbstractMap.SimpleEntry<>(oId, distance));
        }
        adjustAndReturnSearchRadius(ret, k);
        t += System.currentTimeMillis();
        incTime(qId, t);
        incLBChecked(qId, lbChecked);
        LOG.log(Level.INFO, "Evaluated query {2} using {0} dist comps. Time: {1}", new Object[]{getDistCompsForQuery(qId), getTimeOfQuery(qId), qId.toString()});
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

    private int[] getP2ToCheck(int pIndex, int[] pivotPermutation) {
        int[] ret = new int[pIndex];
        System.arraycopy(pivotPermutation, 0, ret, 0, pIndex);
        return ret;
    }

}
