package vm.searchSpace.datasetPartitioning;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.datasetPartitioning.impl.batchProcessor.AbstractPivotBasedPartitioningProcessor;

/**
 *
 * @author xmic
 * @param <T>
 */
public abstract class AbstractDatasetPartitioning<T> {

    public static final Integer BATCH_SIZE = 10000;
    protected final AbstractSearchSpace<T> searchSpace;
    protected long lastTimeOfPartitioning = 0;
    protected long dcOfPartitioning = 0;
    protected String lastAdditionalStats = "";

    public AbstractDatasetPartitioning(AbstractSearchSpace<T> searchSpace) {
        this.searchSpace = searchSpace;
    }

    public abstract Map<Comparable, Collection<Comparable>> partitionObjects(Iterator<Object> dataObjects, String datasetName, StorageDatasetPartitionsInterface storage, Object... params);

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
//        return 1;
        return vm.javatools.Tools.PARALELISATION;
    }

}
