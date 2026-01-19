package vm.searchSpace.datasetPartitioning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.DataTypeConvertor;
import vm.datatools.Tools;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.datasetPartitioning.impl.batchProcessor.AbstractPivotBasedPartitioningProcessor;
import vm.searchSpace.distance.AbstractDistanceFunction;

/**
 *
 * @author xmic
 * @param <T>
 */
public abstract class AbstractDatasetPartitioning<T> {

    private static final Logger LOG = Logger.getLogger(AbstractDatasetPartitioning.class.getName());

    public static final Integer BATCH_SIZE = 10000;
    protected final AbstractSearchSpace<T> searchSpace;
    protected long lastTimeOfPartitioning = 0;
    protected long dcOfPartitioning = 0;
    protected String lastAdditionalStats = "";

    public AbstractDatasetPartitioning(AbstractSearchSpace<T> searchSpace) {
        this.searchSpace = searchSpace;
    }

    public abstract Map<Comparable, Collection<Comparable>> partitionObjects(Iterator<Object> dataObjects, String datasetName, StorageDatasetPartitionsInterface storage, Object... params);

    public Map<Comparable, Collection<Comparable>> partitionObjects(Iterator<Object> dataObjects, Object... params) {
        return partitionObjects(dataObjects, null, null, params);
    }

    public abstract String getName();

    public abstract void setAdditionalStats(AbstractPivotBasedPartitioningProcessor[] processes);

    public long getLastTimeOfPartitioning() {
        return lastTimeOfPartitioning;
    }

    public String getLastAdditionalStats() {
        return lastAdditionalStats;
    }

    public long getDcOfPartitioning() {
        return dcOfPartitioning;
    }

    protected int getParalelism() {
        return vm.javatools.Tools.PARALELISATION;
    }

    public List<Float> computeInterClusterDistances(Map<Comparable, Collection<Comparable>> partitioning, Map<Comparable, T> keyValueStorage, AbstractDistanceFunction<T> df) {
        LOG.log(Level.INFO, "Computing inter cluster distances");
        List<Float> ret = new ArrayList<>();
        Map<Comparable, Comparable> mappingToCells = createMappingToPartitionsID(partitioning);
        List<Object> list = Tools.getObjectsFromIterator(keyValueStorage.entrySet().iterator());
        int counter = 0;
        for (int i = 0; i < list.size() - 1; i++) {
            Map.Entry<Comparable, T> iObj = (Map.Entry<Comparable, T>) list.get(i);
            T iData = iObj.getValue();
            Comparable iCellID = mappingToCells.get(iObj.getKey());
            for (int j = i + 1; j < list.size(); j++) {
                Map.Entry<Comparable, T> jObj = (Map.Entry<Comparable, T>) list.get(j);
                Comparable jCellID = mappingToCells.get(jObj.getKey());
                if (!iCellID.equals(jCellID)) {
                    T jData = jObj.getValue();
                    float distance = df.getDistance(iData, jData);
                    counter++;
                    if (counter % 1000000 == 0) {
                        LOG.log(Level.INFO, "Computed {0} distances", counter);
                    }
                    ret.add(distance);
                }
            }
        }
        return ret;
    }

    public static Map<Comparable, Comparable> createMappingToPartitionsID(Map<Comparable, Collection<Comparable>> partitioning) {
        Map<Comparable, Comparable> ret = new TreeMap<>();
        for (Map.Entry<Comparable, Collection<Comparable>> entry : partitioning.entrySet()) {
            Comparable cellID = entry.getKey();
            Collection<Comparable> values = entry.getValue();
            for (Comparable value : values) {
                ret.put(value, cellID);
            }
        }
        return ret;
    }

    public Map<Comparable, List<Float>> computeIntraClusterDistances(Map<Comparable, Collection<Comparable>> partitioning, Map<Comparable, T> keyValueStorage, AbstractDistanceFunction<T> df) {
        LOG.log(Level.INFO, "Computing intra cluster distances");
        Map<Comparable, List<Float>> ret = new TreeMap<>();
        Set<Comparable> keySet = partitioning.keySet();
        for (Comparable cellKey : keySet) {
            Collection<Comparable> cellObjs = partitioning.get(cellKey);
            List<T> data = new ArrayList<>();
            List<Float> dists = new ArrayList<>();
            for (Comparable oID : cellObjs) {
                data.add(keyValueStorage.get(oID));
            }
            for (int i = 0; i < data.size() - 1; i++) {
                T iData = data.get(i);
                for (int j = i + 1; j < data.size(); j++) {
                    T jData = data.get(j);
                    dists.add(df.getDistance(iData, jData));
                }
            }
            LOG.log(Level.INFO, "Cluster: {0}, {1} pairwise distances", new Object[]{cellKey.toString(), dists.size()});
            ret.put(cellKey, dists);
        }
        return ret;
    }

}
