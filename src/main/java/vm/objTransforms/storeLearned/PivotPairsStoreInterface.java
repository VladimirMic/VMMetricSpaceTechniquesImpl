package vm.objTransforms.storeLearned;

import java.util.List;
import vm.metricSpace.AbstractMetricSpace;

/**
 *
 * @author xmic
 * @param <T>
 */
public interface PivotPairsStoreInterface<T> {

    /**
     *
     * @param resultName see
     * @AbstractObjectToSketchTransformator.getNameOfTransformedSetOfObjects
     * @param metricSpace
     * @param pivots
     * @param additionalInfoToStoreWithLearningSketching
     */
    public void storePivotPairs(String resultName, AbstractMetricSpace<T> metricSpace, List<Object> pivots, Object... additionalInfoToStoreWithLearningSketching);

    /**
     *
     * @param pivotPairsSetName see e.g.
     * @AbstractObjectToSketchTransformator.getNameOfTransformedSetOfObjects()
     * @return list of pivot pairs, the order in the list defines the bits order
     */
    public List<String[]> loadPivotPairsIDs(String pivotPairsSetName);

}
