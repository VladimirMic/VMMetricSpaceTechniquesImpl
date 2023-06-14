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
public class VorSkeSimSorting<T> extends SearchingAlgorithm<T> {

    public static final Boolean STORE_RESULTS = true;

    private static final Logger LOG = Logger.getLogger(VorSkeSim.class.getName());

    private final VoronoiPartitionsCandSetIdentifier voronoiFilter;
    private final int voronoiK;
    private final SecondaryFilteringWithSketches sketchSecondaryFilter;
    private final AbstractObjectToSketchTransformator sketchingTechnique;
    private final AbstractMetricSpace<long[]> hammingSpaceForSketches;

    private final SimRelInterface<float[]> simRelFunc;
    private final int simRelMinK;
    private final Map<Object, float[]> pcaPrefixesMap;

    private final Map<Object, T> fullObjectsStorage;

    private final DistanceFunctionInterface<T> fullDF;

    private long simRelEvalCounter;

    public VorSkeSimSorting(VoronoiPartitionsCandSetIdentifier voronoiFilter, int voronoiK, SecondaryFilteringWithSketches sketchSecondaryFilter, AbstractObjectToSketchTransformator sketchingTechnique, AbstractMetricSpace<long[]> hammingSpaceForSketches, SimRelInterface<float[]> simRelFunc, int simRelMinK, Map<Object, float[]> pcaPrefixesMap, Map<Object, T> fullObjectsStorage, DistanceFunctionInterface<T> fullDF) {
        this.voronoiFilter = voronoiFilter;
        this.voronoiK = voronoiK;
        this.sketchSecondaryFilter = sketchSecondaryFilter;
        this.sketchingTechnique = sketchingTechnique;
        this.hammingSpaceForSketches = hammingSpaceForSketches;
        this.simRelFunc = simRelFunc;
        this.simRelMinK = simRelMinK;
        this.pcaPrefixesMap = pcaPrefixesMap;
        this.fullObjectsStorage = fullObjectsStorage;
        this.fullDF = fullDF;
    }

    @Override
    public TreeSet<Map.Entry<Object, Float>> completeKnnSearch(AbstractMetricSpace<T> fullMetricSpace, Object fullQ, int k, Iterator<Object> ignored, Object... additionalParams) {
        // preparation
        time_addToFull = 0;;
        long t = -System.currentTimeMillis();
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

        // first phase: voronoi
        long t1 = -System.currentTimeMillis();
        List candSetIDs = voronoiFilter.candSetKnnSearch(fullMetricSpace, fullQ, voronoiK, null);
        t1 += System.currentTimeMillis();

        // simRel preparation
        List<Object> simRelAns = new ArrayList<>();
        Set<Object> objIdUnknownRelation = new HashSet<>();
        Map<Object, float[]> simRelCandidatesMap = new HashMap<>();

        // sketch preparation
        long t2 = -System.currentTimeMillis();
        Object qSketch = sketchingTechnique.transformMetricObject(fullQ);
        long[] qSketchData = hammingSpaceForSketches.getDataOfMetricObject(qSketch);
        t2 += System.currentTimeMillis();
        float range = Float.MAX_VALUE;

        long t3 = -System.currentTimeMillis();
        List<AbstractMap.SimpleEntry<Object, Integer>>[] hammingDists = sketchSecondaryFilter.evaluateHammingDistances(qSketchData, candSetIDs);
        t3 += System.currentTimeMillis();

        TreeSet<AbstractMap.SimpleEntry<Integer, Integer>> mapOfCandSetsIdxsToCurHamDist = new TreeSet(new Tools.MapByValueIntComparator<>());
        int[] curIndexes = new int[hammingDists.length];
        for (int i = 0; i < hammingDists.length; i++) {
            AbstractMap.SimpleEntry<Object, Integer> next = hammingDists[i].get(curIndexes[i]);
            mapOfCandSetsIdxsToCurHamDist.add(new AbstractMap.SimpleEntry<>(i, next.getValue()));
        }
        long t4 = 0;
        long t5 = 0;
        int counter = 0;
        while (true) {
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
//            //jinak simRel
            t4 -= System.currentTimeMillis();
            float[] oPCAData = pcaPrefixesMap.get(candID);
            t4 += System.currentTimeMillis();
            t5 -= System.currentTimeMillis();
            boolean knownRelation = addOToSimRelAnswer(simRelMinK, pcaQData, oPCAData, candID, simRelAns, simRelCandidatesMap);
            t5 += System.currentTimeMillis();
            if (!knownRelation) {
                objIdUnknownRelation.add(candID);
            }
            if (objIdUnknownRelation.size() > 10) {
                distComps += addToFullAnswerWithDists(ret, fullQData, objIdUnknownRelation.iterator());
                range = adjustAndReturnSearchRadius(ret, k);
                objIdUnknownRelation.clear();
            }
            if (counter > 200 && (counter < 1000 && counter % 100 == 0)) {
                distComps += addToFullAnswerWithDists(ret, fullQData, simRelAns.iterator());
                range = adjustAndReturnSearchRadius(ret, k);
            }
        }
        simRelAns.addAll(objIdUnknownRelation);

        long t6 = -System.currentTimeMillis();
        // check by sketches again
        for (Object candID : simRelAns) {
            float lowerBound = sketchSecondaryFilter.lowerBound(qSketchData, candID, range);
            if (lowerBound == Float.MAX_VALUE) {
                continue;
            }
            addToRet(ret, candID, fullQData);
            range = adjustAndReturnSearchRadius(ret, k);
        }
        t6 += System.currentTimeMillis();

        t += System.currentTimeMillis();
        incDistsComps(qId, distComps);
        LOG.log(Level.INFO, "distancesCounter;{0}; simRelCounter;{1}", new Object[]{distComps, simRelEvalCounter});
        incTime(qId, t);
        LOG.log(Level.INFO, "Evaluated query {2} using {0} dist comps. Time: {1}", new Object[]{distComps, t, qId.toString()});
        System.out.println("t1: " + t1);
        System.out.println("t2: " + t2);
        System.out.println("t3: " + t3);
        System.out.println("t4: " + t4);
        System.out.println("t5: " + t5);
        System.out.println("t6: " + t6);
        System.out.println("time_addToFull: " + time_addToFull);
        System.out.println("\n");
        return ret;

    }

    private boolean addOToSimRelAnswer(int k, float[] queryObjectData, float[] oData, Object idOfO, List<Object> ansOfSimRel, Map<Object, float[]> mapOfData) {
        if (ansOfSimRel.isEmpty()) {
            ansOfSimRel.add(idOfO);
            mapOfData.put(idOfO, oData);
            return true;
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
            deleteIndexes(ansOfSimRel, k, indexesToRemove, mapOfData);
            ansOfSimRel.add(idxWhereAdd, idOfO);
            mapOfData.put(idOfO, oData);
            return true;
        }
        mapOfData.put(idOfO, oData);
        return false;
    }

    private void deleteIndexes(List<Object> ret, int k, List<Integer> indexesToRemove, Map<Object, float[]> retData) {
        while (ret.size() >= k && !indexesToRemove.isEmpty()) {
            Integer idx = indexesToRemove.get(0);
            Object id = ret.get(idx);
            retData.remove(id);
            ret.remove(id);
            indexesToRemove.remove(0);
        }
    }

    private final Set checked = new HashSet();

    long time_addToFull = 0;

    private int addToFullAnswerWithDists(TreeSet<Map.Entry<Object, Float>> queryAnswer, T fullQData, Iterator<Object> iterator) {
        time_addToFull -= System.currentTimeMillis();
        int distComps = 0;
        Set<Object> currKeys = new HashSet();
        for (Map.Entry<Object, Float> entry : queryAnswer) {
            currKeys.add(entry.getKey());
        }
        // sort them?
        while (iterator.hasNext()) {
            Object key = iterator.next();
            if (!checked.contains(key) && !currKeys.contains(key)) {
                addToRet(queryAnswer, key, fullQData);
                distComps++;
                checked.add(key);
            }
        }
        time_addToFull += System.currentTimeMillis();
        return distComps;
    }

    @Override
    public List<Object> candSetKnnSearch(AbstractMetricSpace<T> metricSpace, Object queryObject, int k, Iterator<Object> objects) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private void addToRet(TreeSet<Map.Entry<Object, Float>> ret, Object candID, T fullQData) {
        T candData = fullObjectsStorage.get(candID);
        float distance = fullDF.getDistance(fullQData, candData);
        ret.add(new AbstractMap.SimpleEntry(candID, distance));
    }

    private int getMinIdx(List<AbstractMap.SimpleEntry<Object, Integer>>[] sortedHammingDists, int[] idxsToCandLists) {
        int ret = 0;
        int minDist = Integer.MAX_VALUE;
        for (int i = 0; i < idxsToCandLists.length; i++) {
            int idx = idxsToCandLists[i];
            Integer hamDist = sortedHammingDists[i].get(idx).getValue();
            if (hamDist < minDist) {
                ret = i;
                minDist = hamDist;
            }
        }
        return ret;
    }

}
