package vm.metricSpace.voronoiPartitioning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author Vlada
 */
public class VoronoiPartitioning {

    public static final Logger LOG = Logger.getLogger(VoronoiPartitioning.class.getName());

    private final AbstractMetricSpace metricSpace;
    private final DistanceFunctionInterface df;
    private final Map<Object, Object> pivots;

    public VoronoiPartitioning(AbstractMetricSpace metricSpace, DistanceFunctionInterface df, List<Object> pivots) {
        this.metricSpace = metricSpace;
        this.df = df;
        this.pivots = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(metricSpace, pivots, true);
    }

    public Map<Object, SortedSet<Object>> splitByVoronoi(Iterator<Object> dataObjects, String datasetName, int pivotCountUsedInTheFileName, StorageLearnedVoronoiPartitioningInterface storage) {
        Map<Object, SortedSet<Object>> ret = new HashMap<>();
        for (int i = 0; dataObjects.hasNext(); i++) {
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
                ret.put(pivotID, new TreeSet<>());
            }
            ret.get(pivotID).add(oID);
            if (i % 50000 == 0) {
                LOG.log(Level.INFO, "Voronoi partitioning done for {0} objects", i);
            }
        }
        if (storage != null) {
            storage.store(ret, datasetName, pivotCountUsedInTheFileName);
        }
        return ret;
    }

}
