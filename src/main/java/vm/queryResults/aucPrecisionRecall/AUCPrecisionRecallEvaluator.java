package vm.queryResults.aucPrecisionRecall;

import java.io.PrintStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import vm.mathtools.Tools;

/**
 *
 * @author au734419
 */
public class AUCPrecisionRecallEvaluator {

    private final Map<Comparable, Collection<Comparable>> classes;
    private final TreeMap<Float, List<Float>> recallToPrecisionMap = new TreeMap<>();
    private final Comparator comp = new vm.datatools.Tools.MapByFloatValueComparator<>();

    public AUCPrecisionRecallEvaluator(Map<Comparable, Collection<Comparable>> classes) {
        this.classes = classes;
    }

    public void registerNextQueryResult(Comparable qClass, List<AbstractMap.SimpleEntry<Comparable, Float>> labelsAndDistsOfDatasetObjects) {
        labelsAndDistsOfDatasetObjects.sort(comp);
        int classSize = classes.get(qClass).size();
//        add(0, 1);
        int hits = 0;
        for (int i = 0; i < labelsAndDistsOfDatasetObjects.size(); i++) {
            AbstractMap.SimpleEntry<Comparable, Float> nn = labelsAndDistsOfDatasetObjects.get(i);
            Comparable nnClass = nn.getKey();
            if (nnClass.equals(qClass)) {
                hits++;
                Float dist = nn.getValue();
                boolean nextInSameDist = i < labelsAndDistsOfDatasetObjects.size() - 1 && labelsAndDistsOfDatasetObjects.get(i + 1).getValue().equals(dist);
                if (!nextInSameDist) {
                    add(hits / (float) classSize, hits / (i + 1f));
                }
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
        Map.Entry<Float, List<Float>> previousEntry = recallToPrecisionMap.firstEntry();
        List<Float> prevValues = previousEntry.getValue();
        for (Map.Entry<Float, List<Float>> entry : recallToPrecisionMap.entrySet()) {
            float deltaX = entry.getKey() - previousEntry.getKey();
            List<Float> currValues = entry.getValue();
            Double meanFAR = Tools.getMean(prevValues, currValues);
            ret += deltaX * meanFAR;
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
}
