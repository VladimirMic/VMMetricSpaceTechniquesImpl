package vm.searchSpace.distance.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import vm.datatools.Tools;
import vm.searchSpace.distance.AbstractDistanceFunction;

/**
 *
 * @author au734419
 * @param <T>
 */
public class SharedNNDistanceFunction<T> extends AbstractDistanceFunction<T> {

    private Boolean insistOnMutual;

    public final Map<String, List<String>> mapOfNNs;
    public final Map<String, Float> kNNradii;
    public final int k;
    public final AbstractDistanceFunction<T> primaryDF;

    public SharedNNDistanceFunction(Map<Comparable, TreeSet<Map.Entry<Comparable, Float>>> groundTruth, Map<Comparable, T> keyValueStorage, AbstractDistanceFunction<T> primaryDF, int k, boolean insistOnMutual) {
        this.mapOfNNs = new HashMap<>();
        this.kNNradii = new HashMap<>();
        this.primaryDF = primaryDF;
        this.insistOnMutual = insistOnMutual;
        this.k = k;
        for (Map.Entry<Comparable, TreeSet<Map.Entry<Comparable, Float>>> evaluatedQuery : groundTruth.entrySet()) {
            Comparable qID = evaluatedQuery.getKey();
            ArrayList<String> list = new ArrayList<>();
            TreeSet<Map.Entry<Comparable, Float>> nns = evaluatedQuery.getValue();
            Iterator<Map.Entry<Comparable, Float>> nnsIt = nns.iterator();
            float knnRadius = 0;
            while (list.size() < k) {
                Map.Entry<Comparable, Float> next = nnsIt.next();
                float dist = next.getValue();
                list.add(next.getKey().toString());
                knnRadius = dist;
            }
            T qData = keyValueStorage.get(qID);
            Comparable qDataID = Tools.hashArray(qData);
            String qDataIDString = qDataID.toString();
            mapOfNNs.put(qDataIDString, list);
            kNNradii.put(qDataIDString, knnRadius);
        }
    }

    public SharedNNDistanceFunction(Map<Comparable, TreeSet<Map.Entry<Comparable, Float>>> groundTruth, Map<Comparable, T> keyValueStorage, int k, boolean insistOnMutual) {
        this(groundTruth, keyValueStorage, null, k, insistOnMutual);
    }

    @Override
    public float getDistance(T obj1, T obj2) {
        Comparable o1ID = Tools.hashArray(obj1);
        Comparable o2ID = Tools.hashArray(obj2);
        if (o1ID.equals(o2ID)) {
            return 0;
        }
        String o1IDString = o1ID.toString();
        String o2IDString = o2ID.toString();
        List<String> l1 = mapOfNNs.get(o1IDString);
        List<String> l2 = mapOfNNs.get(o2IDString);
        if (insistOnMutual) {
            float distance = primaryDF.getDistance(obj1, obj2);
            Float r1 = kNNradii.get(o1IDString);
            Float r2 = kNNradii.get(o2IDString);
            if (r1 < distance || r2 < distance) {
                return 1;
            }
        }
        float intersection = Tools.getIntersection(l1, l2).size();
        return 1 - intersection / k;
    }

    @Override
    public String getName() {
        String suf = insistOnMutual ? "_mutual" : "";
        return "Shared " + k + "NN Distance" + suf;
    }

}
