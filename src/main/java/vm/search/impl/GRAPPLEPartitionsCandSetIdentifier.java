/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.search.impl;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import vm.datatools.Tools;
import vm.metricSpace.Dataset;
import vm.metricSpace.datasetPartitioning.StorageDatasetPartitionsInterface;
import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class GRAPPLEPartitionsCandSetIdentifier<T> extends VoronoiPartitionsCandSetIdentifier<T> {

    public GRAPPLEPartitionsCandSetIdentifier(Dataset dataset, StorageDatasetPartitionsInterface voronoiPartitioningStorage, int pivotCountUsedForPartitioningLearning) {
        super(dataset, voronoiPartitioningStorage, pivotCountUsedForPartitioningLearning);
    }

    @Override
    public Object[] evaluateKeyOrdering(DistanceFunctionInterface<T> df, Map<Object, T> pivotsMap, T qData, Object... params) {
        Object[] pivotsArray = pivotsMap.entrySet().toArray();
        SortedSet<AbstractMap.SimpleEntry<Object, Float>> pivotPairs = new TreeSet(new Tools.MapByValueComparator<>());
        Map<String, Float> distsP1P2 = new HashMap<>();
        for (int idx1 = 0; idx1 < pivotsArray.length - 1; idx1++) {
            Map.Entry<Object, T> p1 = (Map.Entry<Object, T>) pivotsArray[idx1];
            Object p1ID = p1.getKey();
            T p1Data = p1.getValue();
            float distQP1 = df.getDistance(qData, p1Data);
            for (int idx2 = idx1 + 1; idx2 < pivotsArray.length; idx2++) {
                Map.Entry<Object, T> p2 = (Map.Entry<Object, T>) pivotsArray[idx2];
                Object p2ID = p2.getKey();
                T p2Data = p2.getValue();
                String key1 = p1ID + "-" + p2ID;
                float distQP2 = df.getDistance(qData, p2Data);
                Float distP1P2 = distsP1P2.get(key1);
                if (distP1P2 == null) {
                    distP1P2 = df.getDistance(p1Data, p2Data);
                    distsP1P2.put(key1, distP1P2);
                }
                String key2 = p2ID + "-" + p1ID;
                float cosPi1;
//                cosPi1 = (-distQP1 * distQP1 + distQP2 * distQP2 + distP1P2 * distP1P2) / (2 * distQP2 * distP1P2);
                cosPi1 = distQP1 * distQP2 / distP1P2;
                pivotPairs.add(new AbstractMap.SimpleEntry<>(key1, cosPi1));
//                cosPi1 = (-distQP2 * distQP2 + distQP1 * distQP1 + distP1P2 * distP1P2) / (2 * distQP1 * distP1P2);
                pivotPairs.add(new AbstractMap.SimpleEntry<>(key2, cosPi1));
            }
        }
        Iterator<AbstractMap.SimpleEntry<Object, Float>> it = pivotPairs.iterator();
        Object[] ret = new Object[pivotPairs.size()];
        for (int i = 0; it.hasNext(); i++) {
            AbstractMap.SimpleEntry<Object, Float> entry = it.next();
            ret[i] = entry.getKey();
        }
        return ret;
    }

}
