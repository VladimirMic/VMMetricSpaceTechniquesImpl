package vm.queryResults.aucPrecisionRecall;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import vm.mathtools.Tools;

/**
 *
 * @author au734419
 */
public class AUCPrecisionRecallEvaluator {

    private final Map<Comparable, Collection<Comparable>> classes;
    private final TreeMap<Float, List<Float>> recallToPrecisionMap = new TreeMap<>();

    public AUCPrecisionRecallEvaluator(Map<Comparable, Collection<Comparable>> classes) {
        this.classes = classes;
    }

    public void registerNextQueryResult(Comparable qClass, TreeSet<AbstractMap.SimpleEntry<Comparable, Float>> sortedDataset) {
        int classSize = classes.get(qClass).size();
        add(0, 1);
        int counter = 0;
        int hits = 0;
        for (AbstractMap.SimpleEntry<Comparable, Float> nn : sortedDataset) {
            counter++;
            Comparable nnClass = nn.getKey();
            if (nnClass.equals(qClass)) {
                hits++;
                add(hits / (float) classSize, hits / (float) counter);
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
}
