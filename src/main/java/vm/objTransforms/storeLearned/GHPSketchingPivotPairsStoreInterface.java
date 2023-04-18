package vm.objTransforms.storeLearned;

import java.util.List;
import vm.metricSpace.AbstractMetricSpace;

/**
 *
 * @author xmic
 */
public interface GHPSketchingPivotPairsStoreInterface {

    public void storeSketching(String resultName, AbstractMetricSpace<Object> metricSpace, List<Object> pivots, Object... additionalInfoToStoreWithLearningSketching);

    public List<String[]> loadPivotPairsIDs(String sketchesName);

}
