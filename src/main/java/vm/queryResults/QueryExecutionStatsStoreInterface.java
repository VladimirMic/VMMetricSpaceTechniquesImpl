package vm.queryResults;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author Vlada
 */
public abstract class QueryExecutionStatsStoreInterface {

    public abstract void storeStatsForQuery(Comparable queryObjId, Integer distanceComputationsCount, long time, Object... additionalParametersToStore);

    public void storeStatsForQueries(Map<Comparable, AtomicInteger> distComps, Map<Comparable, AtomicLong> times, Map<Comparable, List<AtomicLong>> additionalParametersToStore) {
        Set<Comparable> ids = new HashSet<>();
        ids.addAll(distComps.keySet());
        ids.addAll(times.keySet());
        if (additionalParametersToStore != null) {
            ids.addAll(additionalParametersToStore.keySet());
        }
        for (Comparable id : ids) {
            Integer distComp = distComps.containsKey(id) ? distComps.get(id).get() : null;
            long time = times.containsKey(id) ? times.get(id).get() : -1;
            Object[] stats = new Object[0];
            if (additionalParametersToStore != null && additionalParametersToStore.containsKey(id)) {
                stats = additionalParametersToStore.get(id).toArray();
            }
            storeStatsForQuery(id, distComp, time, stats);
        }
    }

    public abstract Map<Object, Integer> getDistComps();

    public abstract Map<Object, Long> getQueryTimes();

    public abstract void save();

}
