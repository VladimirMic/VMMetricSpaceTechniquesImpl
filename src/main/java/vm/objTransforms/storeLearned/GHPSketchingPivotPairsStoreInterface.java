package vm.objTransforms.storeLearned;

import java.util.List;
import vm.metricSpace.AbstractMetricSpace;

/**
 *
 * @author xmic
 */
public interface GHPSketchingPivotPairsStoreInterface {

    /**
     *
     * @param resultName see
     * @AbstractObjectToSketchTransformator.getNameOfTransformedSetOfObjects
     * @param metricSpace
     * @param pivots
     * @param additionalInfoToStoreWithLearningSketching
     */
    public void storeSketching(String resultName, AbstractMetricSpace<Object> metricSpace, List<Object> pivots, Object... additionalInfoToStoreWithLearningSketching);

    /**
     *
     * @param sketchesName see
     * @AbstractObjectToSketchTransformator.getNameOfTransformedSetOfObjects()
     * @return list of pivot pairs, the order in the list defines the bits order
     */
    public List<String[]> loadPivotPairsIDs(String sketchesName);

}
