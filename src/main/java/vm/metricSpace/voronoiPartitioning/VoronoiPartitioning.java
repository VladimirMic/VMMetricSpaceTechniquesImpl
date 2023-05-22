package vm.metricSpace.voronoiPartitioning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author Vlada
 */
public class VoronoiPartitioning {

    private final AbstractMetricSpace metricSpace;
    private final DistanceFunctionInterface df;
    private final Map<Object, Object> pivots;

    public VoronoiPartitioning(AbstractMetricSpace metricSpace, DistanceFunctionInterface df, List<Object> pivots) {
        this.metricSpace = metricSpace;
        this.df = df;
        this.pivots = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(metricSpace, pivots, true);
    }

    public Map<Object, List<Object>> splitByVoronoi(Iterator<Object> dataObjects) {
        Map<Object, List<Object>> ret = new HashMap<>();
        while (dataObjects.hasNext()) {
            Object o = dataObjects.next();
            Object oData = metricSpace.getDataOfMetricObject(o);
            Object oID = metricSpace.getIDOfMetricObject(o);
            float minDist = Float.MAX_VALUE;
            Object pivotID = null;
            for (Map.Entry<Object, Object> pivot : pivots.entrySet()) {
                float dist = df.getDistance(oData, pivot.getValue());
                if (dist < minDist) {
                    minDist = dist;
                    pivotID = pivot.getKey();
                }
            }
            if (!ret.containsKey(pivotID)) {
                ret.put(pivotID, new ArrayList<>());
            }
            ret.get(pivotID).add(oID);
        }
        return ret;
    }

}
