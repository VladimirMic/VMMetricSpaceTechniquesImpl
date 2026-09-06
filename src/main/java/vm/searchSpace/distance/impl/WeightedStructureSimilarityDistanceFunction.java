package vm.searchSpace.distance.impl;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import vm.datatools.Tools;
import vm.searchSpace.distance.AbstractDistanceFunction;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class WeightedStructureSimilarityDistanceFunction<T> extends AbstractDistanceFunction<T> {

    private Boolean insistOnMutual;

    public final Map<String, SortedSet<AbstractMap.SimpleEntry<String, Float>>> kNNDistsSet;
    public final Map<String, Map<String, Float>> kNNDistsMap;
    public final Map<String, Float> norms;
    public final int k;
    public final AbstractDistanceFunction<T> primaryDF;

    public WeightedStructureSimilarityDistanceFunction(Map<Comparable, TreeSet<Map.Entry<Comparable, Float>>> groundTruth, Map<Comparable, T> keyValueStorage, AbstractDistanceFunction<T> primaryDF, int k, boolean insistOnMutual) {
        this.kNNDistsSet = new HashMap<>();
        this.kNNDistsMap = new HashMap<>();
        this.norms = new HashMap<>();
        this.primaryDF = primaryDF;
        this.insistOnMutual = insistOnMutual;
        this.k = k;
        for (Map.Entry<Comparable, TreeSet<Map.Entry<Comparable, Float>>> evaluatedQuery : groundTruth.entrySet()) {
            Comparable qID = evaluatedQuery.getKey();
            TreeSet<Map.Entry<Comparable, Float>> nns = evaluatedQuery.getValue();
            Iterator<Map.Entry<Comparable, Float>> nnsIt = nns.iterator();
            SortedSet<AbstractMap.SimpleEntry<String, Float>> setOfDists = new TreeSet<>(new Tools.MapByFloatValueComparator<>());
            Map<String, Float> mapOfDists = new HashMap<>();
            float norm = 0;
            while (setOfDists.size() < k && nnsIt.hasNext()) {
                Map.Entry<Comparable, Float> next = nnsIt.next();
                float dist = next.getValue();
                norm += dist * dist;
                String key = next.getKey().toString();
                mapOfDists.put(key, dist);
                AbstractMap.SimpleEntry<String, Float> entry = new AbstractMap.SimpleEntry<>(key, dist);
                setOfDists.add(entry);
            }
            norm = (float) Math.sqrt(norm);
            T qData = keyValueStorage.get(qID);
            Comparable qDataID = Tools.hashArray(qData);
            String qDataIDString = qDataID.toString();
            kNNDistsSet.put(qDataIDString, setOfDists);
            kNNDistsMap.put(qDataIDString, mapOfDists);
            norms.put(qDataIDString, norm);
        }
    }

    public WeightedStructureSimilarityDistanceFunction(Map<Comparable, TreeSet<Map.Entry<Comparable, Float>>> groundTruth, Map<Comparable, T> keyValueStorage, int k, boolean insistOnMutual) {
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
        if (insistOnMutual) {
            float distance = primaryDF.getDistance(obj1, obj2);
            SortedSet<AbstractMap.SimpleEntry<String, Float>> dists1 = kNNDistsSet.get(o1IDString);
            SortedSet<AbstractMap.SimpleEntry<String, Float>> dists2 = kNNDistsSet.get(o2IDString);
            Float r1 = dists1.getLast().getValue();
            Float r2 = dists2.getLast().getValue();
            if (r1 < distance || r2 < distance) {
                return 1;
            }
        }
        Map<String, Float> map1 = kNNDistsMap.get(o1IDString);
        Map<String, Float> map2 = kNNDistsMap.get(o2IDString);
        float numerator = 0;
        for (Map.Entry<String, Float> nns1 : map1.entrySet()) {
            String key = nns1.getKey();
            Float dist2 = map2.get(key);
            if (dist2 != null) {
                Float dist1 = map1.get(key);
                numerator += dist1 * dist2;
            }
        }
        float denominator = norms.get(o1IDString) * norms.get(o2IDString);
        return 1 - numerator / denominator;
    }

    @Override
    public String getName() {
        String suf = insistOnMutual ? "_mutual" : "";
        return "WSS_" + k + "NN" + suf;
    }

}
