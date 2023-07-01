/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.search.impl.multiFiltering;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.metricSpace.distance.bounding.nopivot.impl.SecondaryFilteringWithSketches;
import vm.objTransforms.objectToSketchTransformators.AbstractObjectToSketchTransformator;
import vm.search.SearchingAlgorithm;
import vm.search.impl.VoronoiPartitionsCandSetIdentifier;
import vm.simRel.SimRelInterface;
import vm.simRel.impl.SimRelEuclideanPCAImplForTesting;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class CranberryAlgorithm<T> extends SearchingAlgorithm<T> {

    public static final Integer PARALELISM = vm.javatools.Tools.PARALELISATION;
    public static final Integer MAX_DIST_COMPS = 3500;
    public static final Boolean STORE_RESULTS = true;

    private static final Logger LOG = Logger.getLogger(VorSkeSim.class.getName());

    private final VoronoiPartitionsCandSetIdentifier voronoiFilter;
    private final int voronoiK;
    private final SecondaryFilteringWithSketches sketchSecondaryFilter;
    private final AbstractObjectToSketchTransformator sketchingTechnique;
    private final AbstractMetricSpace<long[]> hammingSpaceForSketches;

    private final SimRelInterface<float[]> simRelFunc;
    private final int simRelMinK;
    private final int simRelMaxK;
    private final Map<Object, float[]> pcaPrefixesMap;

    private final Map<Object, T> fullObjectsStorage;

    private final DistanceFunctionInterface<T> fullDF;

    private long simRelEvalCounter;

    public CranberryAlgorithm(VoronoiPartitionsCandSetIdentifier voronoiFilter, int voronoiK, SecondaryFilteringWithSketches sketchSecondaryFilter, AbstractObjectToSketchTransformator sketchingTechnique, AbstractMetricSpace<long[]> hammingSpaceForSketches, SimRelInterface<float[]> simRelFunc, int simRelMinK, int simRelMaxK, Map<Object, float[]> pcaPrefixesMap, Map<Object, T> fullObjectsStorage, DistanceFunctionInterface<T> fullDF) {
        this.voronoiFilter = voronoiFilter;
        this.voronoiK = voronoiK;
        this.sketchSecondaryFilter = sketchSecondaryFilter;
        this.sketchingTechnique = sketchingTechnique;
        this.hammingSpaceForSketches = hammingSpaceForSketches;
        this.simRelFunc = simRelFunc;
        this.simRelMinK = simRelMinK;
        this.simRelMaxK = simRelMaxK;
        this.pcaPrefixesMap = pcaPrefixesMap;
        this.fullObjectsStorage = fullObjectsStorage;
        this.fullDF = fullDF;
    }
    private Set<String> ANSWER = null;

    @Override
    public TreeSet<Map.Entry<Object, Float>> completeKnnSearch(AbstractMetricSpace<T> fullMetricSpace, Object fullQ, int k, Iterator<Object> ignored, Object... additionalParams) {
        // preparation
        time_addToFull = 0;
        long overallTime = -System.currentTimeMillis();
        int distComps = 0;
        simRelEvalCounter = 0;

        TreeSet<Map.Entry<Object, Float>> currAnswer = null;
        int paramIDX = 0;
        if (additionalParams.length > 0 && additionalParams[0] instanceof TreeSet) {
            currAnswer = (TreeSet<Map.Entry<Object, Float>>) additionalParams[0];
            paramIDX++;
        }
        Object qId = fullMetricSpace.getIDOfMetricObject(fullQ);
        T fullQData = fullMetricSpace.getDataOfMetricObject(fullQ);
        TreeSet<Map.Entry<Object, Float>> ret = currAnswer == null ? new TreeSet<>(new Tools.MapByValueComparator()) : currAnswer;

        if (simRelFunc instanceof SimRelEuclideanPCAImplForTesting) {
            SimRelEuclideanPCAImplForTesting euclid = (SimRelEuclideanPCAImplForTesting) simRelFunc;
            euclid.resetEarlyStopsOnCoordsCounts();
        }

        AbstractMetricSpace<float[]> pcaMetricSpace = (AbstractMetricSpace<float[]>) additionalParams[paramIDX++];
        Object pcaQ = additionalParams[paramIDX++];
        float[] pcaQData = pcaMetricSpace.getDataOfMetricObject(pcaQ);

        // actual query evaluation
        // first phase: voronoi
//        long t1 = -System.currentTimeMillis();
        List candSetIDs = voronoiFilter.candSetKnnSearch(fullMetricSpace, fullQ, voronoiK, null);
//        t1 += System.currentTimeMillis();

        // simRel preparation
        List<Object> simRelAns = new ArrayList<>();
        Set<Object> objIdUnknownRelation = new TreeSet<>();
        Map<Object, float[]> simRelCandidatesMap = new HashMap<>();

        // sketch preparation
//        long t2 = -System.currentTimeMillis();
        Object qSketch = sketchingTechnique.transformMetricObject(fullQ);
        long[] qSketchData = hammingSpaceForSketches.getDataOfMetricObject(qSketch);
//        t2 += System.currentTimeMillis();
        float range = Float.MAX_VALUE;

//        long t3 = -System.currentTimeMillis();
        List<AbstractMap.SimpleEntry<Object, Integer>>[] hammingDists = sketchSecondaryFilter.evaluateHammingDistancesInParallel(qSketchData, candSetIDs);
//        t3 += System.currentTimeMillis();

        TreeSet<AbstractMap.SimpleEntry<Integer, Integer>> mapOfCandSetsIdxsToCurHamDist = new TreeSet(new Tools.MapByValueIntComparator<>());
        int[] curIndexes = new int[hammingDists.length];
        for (int i = 0; i < hammingDists.length; i++) {
            if (hammingDists[i].isEmpty()) {
                continue;
            }
            AbstractMap.SimpleEntry<Object, Integer> next = hammingDists[i].get(curIndexes[i]);
            mapOfCandSetsIdxsToCurHamDist.add(new AbstractMap.SimpleEntry<>(i, next.getValue()));
        }
//        long retrieveFromPCAMemoryMap = 0;
//        long simRelTimes = 0;
        int counter = 0;
//        int COUNT_OF_SEEN = 0;

//        if (additionalParams.length > paramIDX && additionalParams[paramIDX] instanceof Set) {
//            ANSWER = (Set<String>) additionalParams[paramIDX];
//            paramIDX++;
//        }

        Set checkedIDs = new HashSet();

        while (!mapOfCandSetsIdxsToCurHamDist.isEmpty() && distComps < MAX_DIST_COMPS) {
            AbstractMap.SimpleEntry<Integer, Integer> candSetRunIndexAndHamDist = mapOfCandSetsIdxsToCurHamDist.first();
            mapOfCandSetsIdxsToCurHamDist.remove(candSetRunIndexAndHamDist);

            int candSetRunIndex = candSetRunIndexAndHamDist.getKey();
            int indexInTheRun = curIndexes[candSetRunIndex];
            AbstractMap.SimpleEntry<Object, Integer> next = hammingDists[candSetRunIndex].get(indexInTheRun);
            curIndexes[candSetRunIndex]++;
            if (curIndexes[candSetRunIndex] < hammingDists[candSetRunIndex].size()) {
                AbstractMap.SimpleEntry<Object, Integer> nextCandWithHamDist = hammingDists[candSetRunIndex].get(curIndexes[candSetRunIndex]);
                mapOfCandSetsIdxsToCurHamDist.add(new AbstractMap.SimpleEntry<>(candSetRunIndex, nextCandWithHamDist.getValue()));
            }

            Object candID = next.getKey();
//            if (ANSWER != null && ANSWER.contains(candID.toString())) {
//                COUNT_OF_SEEN++;
//                ANSWER.remove(candID.toString());
//            }
            int hamDist = next.getValue();
            // zkusit skece pokud je ret plna
            if (ret.size() < k) {
                //add to ret   
                distComps++;
                addToRet(ret, candID, fullQData);
                range = adjustAndReturnSearchRadius(ret, k);
                continue;
            } else {
                float lowerBound = sketchSecondaryFilter.lowerBound(hamDist, range);
                if (lowerBound == Float.MAX_VALUE) {
                    break;
                }
            }
//            //otherwise simRel
//            retrieveFromPCAMemoryMap -= System.currentTimeMillis();
            float[] oPCAData = pcaPrefixesMap.get(candID);
//            retrieveFromPCAMemoryMap += System.currentTimeMillis();
//            simRelTimes -= System.currentTimeMillis();
            boolean knownRelation = addOToSimRelAnswer(simRelMinK, pcaQData, oPCAData, candID, simRelAns, simRelCandidatesMap);
//            simRelTimes += System.currentTimeMillis();
            if (!knownRelation) {
                objIdUnknownRelation.add(candID);
            }
            if (objIdUnknownRelation.size() > 10) {
                distComps += addToFullAnswerWithDists(ret, fullQData, objIdUnknownRelation.iterator(), checkedIDs);
                range = adjustAndReturnSearchRadius(ret, k);
                objIdUnknownRelation.clear();
            }
            if (counter > 200 && (counter < 1000 && counter % 100 == 0)) {
                distComps += addToFullAnswerWithDists(ret, fullQData, simRelAns.iterator(), checkedIDs);
                range = adjustAndReturnSearchRadius(ret, k);
            }
            if (ANSWER != null && ANSWER.isEmpty()) {
                break;
            }

        }
        simRelAns.addAll(objIdUnknownRelation);

//        long t6 = -System.currentTimeMillis();
        // check by sketches again
        for (Object candID : simRelAns) {
            float lowerBound = sketchSecondaryFilter.lowerBound(qSketchData, candID, range);
            if (lowerBound == Float.MAX_VALUE) {
                continue;
            }
            int added = addToFullAnswerWithDists(ret, fullQData, candID, checkedIDs);
            if (added == 1) {
                range = adjustAndReturnSearchRadius(ret, k);
                distComps++;
            }
        }
//        t6 += System.currentTimeMillis();
//        time_addToFull += t6;

        overallTime += System.currentTimeMillis();
        incDistsComps(qId, distComps);
        incTime(qId, overallTime);
//        System.err.println("t1: " + t1);
//        System.err.println("t2: " + t2);
//        System.err.println("t3: " + t3);
//        System.err.println("retrieveFromPCAMemoryMap: " + retrieveFromPCAMemoryMap);
//        System.err.println("simRelTimes: " + simRelTimes);
//        System.err.println("t6: " + t6);
//        System.err.println("time_addToFull: " + time_addToFull);
        LOG.log(Level.INFO, "Evaluated query {2} using {0} dist comps and {3} simRels. Time: {1}\n\n", new Object[]{distComps, overallTime, qId.toString(), simRelEvalCounter});
        return ret;

    }

    private boolean addOToSimRelAnswer(int k, float[] queryObjectData, float[] oData, Object idOfO, List<Object> ansOfSimRel, Map<Object, float[]> mapOfData) {
        if (ansOfSimRel.isEmpty()) {
            ansOfSimRel.add(idOfO);
            mapOfData.put(idOfO, oData);
            return true;
        }
        if (ansOfSimRel.size() > simRelMaxK) {
            deleteIndexes(ansOfSimRel, k, null, mapOfData);
        }
        int idxWhereAdd = Integer.MAX_VALUE;
        List<Integer> indexesToRemove = new ArrayList<>();
        for (int i = ansOfSimRel.size() - 1; i >= 0; i--) {
            float[] oLastData = mapOfData.get(ansOfSimRel.get(i));
            simRelEvalCounter++;
            short sim = simRelFunc.getMoreSimilar(queryObjectData, oLastData, oData);
            if (sim == 1) {
                if (i < k - 1) {
                    deleteIndexes(ansOfSimRel, k, indexesToRemove, mapOfData);
                    ansOfSimRel.add(i + 1, idOfO);
                    mapOfData.put(idOfO, oData);
                }
                return true;
            }
            if (sim == 2) {
                idxWhereAdd = i;
                indexesToRemove.add(i);
            }
        }
        if (idxWhereAdd != Integer.MAX_VALUE) {
//            System.out.print("Pos;" + idxWhereAdd + ";size;" + ansOfSimRel.size() + ";simRelEvalCounter;" + simRelEvalCounter);
            deleteIndexes(ansOfSimRel, k, indexesToRemove, mapOfData);
//            System.out.println(";afterDeleteSize;" + ansOfSimRel.size());
            ansOfSimRel.add(idxWhereAdd, idOfO);
            mapOfData.put(idOfO, oData);
            return true;
        }
        mapOfData.put(idOfO, oData);
        return false;
    }

    private void deleteIndexes(List<Object> ret, int k, List<Integer> indexesToRemove, Map<Object, float[]> retData) {
        if (indexesToRemove != null) {
            while (ret.size() >= k && !indexesToRemove.isEmpty()) {
                Integer idx = indexesToRemove.get(0);
                Object id = ret.get(idx);
                if (ANSWER != null && ANSWER.contains(id)) {
                    String s = "";
                }
                retData.remove(id);
                ret.remove(id);
                indexesToRemove.remove(0);
            }
        }
        while (ret.size() >= simRelMaxK) {
            Object id = ret.get(ret.size() - 1);
            if (ANSWER != null && ANSWER.contains(id)) {
                String s = "";
            }
            retData.remove(id);
            ret.remove(id);
        }
    }

    private long time_addToFull = 0;

    private int addToFullAnswerWithDists(TreeSet<Map.Entry<Object, Float>> queryAnswer, T fullQData, Object id, Set checkedIDs) {
        if (!checkedIDs.contains(id)) {
            addToRet(queryAnswer, id, fullQData);
            checkedIDs.add(id);
            return 1;
        }
        return 0;
    }

    private int addToFullAnswerWithDists(TreeSet<Map.Entry<Object, Float>> queryAnswer, T fullQData, Iterator<Object> iterator, Set checkedIDs) {
        time_addToFull -= System.currentTimeMillis();
        int distComps = 0;
        while (iterator.hasNext()) {
            Object key = iterator.next();
            distComps += addToFullAnswerWithDists(queryAnswer, fullQData, key, checkedIDs);
        }
        time_addToFull += System.currentTimeMillis();
        return distComps;
    }

    @Override
    public List<Object> candSetKnnSearch(AbstractMetricSpace<T> metricSpace, Object queryObject, int k, Iterator<Object> objects) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public TreeSet<Map.Entry<Object, Float>>[] completeKnnSearchOfQuerySet(final AbstractMetricSpace<T> metricSpace, List<Object> queryObjects, int k, Iterator<Object> objects, Object... additionalParams) {
        AbstractMetricSpace pcaDatasetMetricSpace = (AbstractMetricSpace) additionalParams[0];
        Map<Object, Object> pcaQMap = (Map<Object, Object>) additionalParams[1];
//        int queriesCount = 500;
        int queriesCount = queryObjects.size();
        final TreeSet<Map.Entry<Object, Float>>[] ret = new TreeSet[queriesCount];
        ExecutorService threadPool = vm.javatools.Tools.initExecutor(PARALELISM);
        CountDownLatch latch = new CountDownLatch(queriesCount);
        for (int i = 0; i < queriesCount; i++) {
            Object queryObject = queryObjects.get(i);
            Object qID = metricSpace.getIDOfMetricObject(queryObject);
            Object pcaQueryObject = pcaQMap.get(qID);
            Object pcaQData = pcaDatasetMetricSpace.getDataOfMetricObject(pcaQueryObject);
            int iFinal = i;
            threadPool.execute(() -> {
                long tQ = -System.currentTimeMillis();
                AbstractMap.SimpleEntry pcaQ = new AbstractMap.SimpleEntry(qID, pcaQData);
                ret[iFinal] = completeKnnSearch(metricSpace, queryObject, k, null, pcaDatasetMetricSpace, pcaQ);
                tQ += System.currentTimeMillis();
                timesPerQueries.put(qID, new AtomicLong(tQ));
                latch.countDown();
            });
        }
        try {
            latch.await();
        } catch (InterruptedException ex) {
            Logger.getLogger(CranberryAlgorithm.class.getName()).log(Level.SEVERE, null, ex);
        }
        threadPool.shutdown();
        return ret;
    }

    private void addToRet(TreeSet<Map.Entry<Object, Float>> ret, Object candID, T fullQData) {
        T candData = fullObjectsStorage.get(candID);
        float distance = fullDF.getDistance(fullQData, candData);
        ret.add(new AbstractMap.SimpleEntry(candID, distance));
    }

    public long[] getSimRelStatsOfLastExecutedQuery() {
        if (simRelFunc instanceof SimRelEuclideanPCAImplForTesting) {
            SimRelEuclideanPCAImplForTesting euclid = (SimRelEuclideanPCAImplForTesting) simRelFunc;
            return euclid.getEarlyStopsOnCoordsCounts();
        }
        return null;
    }

}