package vm.searchSpace.distance.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import vm.datatools.Tools;
import vm.searchSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author au734419
 * @param <T>
 */
public class SharedNNDistanceFunction<T> extends DistanceFunctionInterface<T> {

    public final Map<String, List<String>> mapOfNNs;
    public final int k;

    public SharedNNDistanceFunction(Map<Comparable, TreeSet<Map.Entry<Comparable, Float>>> groundTruth, Map<Comparable, T> keyValueStorage, int k) {
        this.mapOfNNs = new HashMap<>();
        this.k = k;
        for (Map.Entry<Comparable, TreeSet<Map.Entry<Comparable, Float>>> entry : groundTruth.entrySet()) {
            Comparable qID = entry.getKey();
            ArrayList<String> list = new ArrayList<>();
            TreeSet<Map.Entry<Comparable, Float>> nns = entry.getValue();
            Iterator<Map.Entry<Comparable, Float>> nnsIt = nns.iterator();
            while (list.size() < k) {
                Map.Entry<Comparable, Float> next = nnsIt.next();
                if (next.getValue() != 0) {
                    list.add(next.getKey().toString());
                }
            }
            T qData = keyValueStorage.get(qID);
            Comparable qDataID = Tools.hashArray(qData);
            String qDataIDString = qDataID.toString();
            this.mapOfNNs.put(qDataIDString, list);
        }
    }

    @Override
    public float getDistance(T obj1, T obj2) {
        Comparable o1ID = Tools.hashArray(obj1);
        Comparable o2ID = Tools.hashArray(obj2);
        String o1IDString = o1ID.toString();
        String o2IDString = o2ID.toString();
        List<String> l1 = mapOfNNs.get(o1IDString);
        List<String> l2 = mapOfNNs.get(o2IDString);
        float intersection = Tools.getIntersection(l1, l2).size();
        return 1 - intersection / k;
    }

    @Override
    public String getName() {
        return "Shared " + k + "NN Distance";
    }

}
