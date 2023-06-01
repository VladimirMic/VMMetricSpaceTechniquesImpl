package vm.metricSpace.distance.bounding.nopivot.storeLearned;

import java.util.Map;
import java.util.SortedMap;

/**
 *
 * @author Vlada
 */
public interface SecondaryFilteringWithSketchesStoreInterface {

    public void store(
            Map<Double, Integer> learntMapping,
            float thresholdPcum,
            String fullDatasetName,
            String sketchesDatasetName,
            int iDimSketchesSampleCount,
            int iDimDistComps,
            float distIntervalForPX
    );

    public SortedMap<Double, Integer> loadMapping(
            float thresholdPcum,
            String fullDatasetName,
            String sketchesDatasetName,
            int iDimSketchesSampleCount,
            int iDimDistComps,
            float distIntervalForPX
    );
}
