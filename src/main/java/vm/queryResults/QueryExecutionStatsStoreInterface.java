package vm.queryResults;

import java.util.HashSet;
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

    public void storeStatsForQueries(Map<Comparable, AtomicInteger> distComps, Map<Comparable, AtomicLong> times, Map<Comparable, AtomicLong>... additionalParametersToStore) {
        Set<Comparable> ids = new HashSet<>();
        ids.addAll(distComps.keySet());
        ids.addAll(times.keySet());
        for (Comparable id : ids) {
            Integer distComp = distComps.containsKey(id) ? distComps.get(id).get() : null;
            long time = times.containsKey(id) ? times.get(id).get() : -1;
            Object[] stats = new Object[additionalParametersToStore.length];
            if (additionalParametersToStore.length != 0) {
                for (int i = 0; i < additionalParametersToStore.length; i++) {
                    Map<Comparable, AtomicLong> map = additionalParametersToStore[i];
                    AtomicLong get = map.get(id);
                    stats[i] = get.get();
                }
            }
            storeStatsForQuery(id, distComp, time, stats);
        }
    }

    public abstract Map<Object, Integer> getDistComps();

    public abstract Map<Object, Long> getQueryTimes();

    public abstract void save();

}
