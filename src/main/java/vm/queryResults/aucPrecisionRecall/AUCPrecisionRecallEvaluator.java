package vm.queryResults.aucPrecisionRecall;

import java.io.PrintStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.mathtools.Tools;

/**
 *
 * @author au734419
 */
public class AUCPrecisionRecallEvaluator {

    private final Map<Comparable, Collection<Comparable>> classes;
    private final TreeMap<Float, List<Float>> recallToPrecisionMap = new TreeMap<>();
    private final Comparator comp = new vm.datatools.Tools.MapByFloatValueComparator<>();
    private final Float RECALL_GRANULARITY = 1 / 1000f * 2;
    private final Integer MIN_OBSERVATIONS_PER_BUCKET_OTHERWISE_LOGGED = 30;

    public AUCPrecisionRecallEvaluator(Map<Comparable, Collection<Comparable>> classes) {
        this.classes = classes;
    }

    public Map<Float, List<Float>> getRecallToPrecisionMap() {
        return Collections.unmodifiableMap(recallToPrecisionMap);
    }

    public void registerNextQueryResult(Comparable qClass, List<AbstractMap.SimpleEntry<Integer, Float>> labelsAndDistsOfDatasetObjects) {
        labelsAndDistsOfDatasetObjects.sort(comp);
        int classSize = classes.get(qClass).size();
//        add(0, 1);
        int hits = 0;
        for (int i = 0; i < labelsAndDistsOfDatasetObjects.size(); i++) {
            AbstractMap.SimpleEntry<Integer, Float> nn = labelsAndDistsOfDatasetObjects.get(i);
            Integer nnClass = nn.getKey();
            Float dist = nn.getValue();
            boolean nextInSameDist = i < labelsAndDistsOfDatasetObjects.size() - 1 && labelsAndDistsOfDatasetObjects.get(i + 1).getValue().equals(dist);
            if (nnClass.equals(qClass)) {
                hits++;
            }
            if (!nextInSameDist) {
                add(hits / (float) classSize, hits / (i + 1f));
            }
        }
    }

    private void add(float recall, float precision) {
        if (!recallToPrecisionMap.containsKey(recall)) {
            recallToPrecisionMap.put(recall, new ArrayList<>());
        }
        recallToPrecisionMap.get(recall).add(precision);
    }

    public float computeAUC() {
        float ret = 0;
        mergeByRecallStep();
        Map.Entry<Float, List<Float>> previousEntry = recallToPrecisionMap.firstEntry();
        List<Float> prevValues = previousEntry.getValue();
        for (Map.Entry<Float, List<Float>> entry : recallToPrecisionMap.entrySet()) {
            float deltaRecall = entry.getKey() - previousEntry.getKey();
            List<Float> currValues = entry.getValue();
            Double meanPrecision = Tools.getMean(prevValues, currValues);
            ret += deltaRecall * meanPrecision;
            previousEntry = entry;
            prevValues = currValues;

        }
        return ret;
    }

    public void printMap(PrintStream p) {
        for (Map.Entry<Float, List<Float>> entry : recallToPrecisionMap.entrySet()) {
            Float recall = entry.getKey();
            float precision = Tools.getMean(entry.getValue()).floatValue();
            p.println(recall + ";" + precision);
        }
    }

    private void mergeByRecallStep() {
        TreeMap<Float, List<Float>> merged = new TreeMap<>();
        for (Map.Entry<Float, List<Float>> entry : recallToPrecisionMap.entrySet()) {
            float x = entry.getKey();
            x = Tools.round(x, RECALL_GRANULARITY, false);
            if (!merged.containsValue(x)) {
                merged.put(x, new ArrayList<>());
            }
            merged.get(x).addAll(entry.getValue());
        }
        for (Map.Entry<Float, List<Float>> entry : merged.entrySet()) {
            if (entry.getValue().size() < MIN_OBSERVATIONS_PER_BUCKET_OTHERWISE_LOGGED) {
                Logger.getLogger(AUCPrecisionRecallEvaluator.class.getName()).log(Level.WARNING, "Small bucket for recall: " + entry.getKey() + ", size " + entry.getValue().size());
            }
        }
        recallToPrecisionMap.clear();
        recallToPrecisionMap.putAll(merged);
    }
}
