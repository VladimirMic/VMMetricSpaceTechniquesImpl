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

    public abstract void storeStatsForQuery(Object queryObjId, Integer distanceComputationsCount, long time, Object... additionalParametersToStore);

    public void storeStatsForQueries(Map<Object, AtomicInteger> distComps, Map<Object, AtomicLong> times, Object... additionalParametersToStore) {
        Set<Object> ids = new HashSet<>();
        ids.addAll(distComps.keySet());
        ids.addAll(times.keySet());
        for (Object id : ids) {
            Integer distComp = distComps.containsKey(id) ? distComps.get(id).get() : null;
            long time = times.containsKey(id) ? times.get(id).get() : -1;
            storeStatsForQuery(id, distComp, time, additionalParametersToStore);
        }
    }

}
