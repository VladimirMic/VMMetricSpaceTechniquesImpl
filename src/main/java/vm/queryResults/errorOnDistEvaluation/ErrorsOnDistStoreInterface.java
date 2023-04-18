package vm.queryResults.errorOnDistEvaluation;

/**
 *
 * @author Vlada
 */
public interface ErrorsOnDistStoreInterface {

    /**
     *
     * @param queryObjId id of the query object
     * @param errorOnDist its error on distance
     * @param additionalParametersToStore store also the names of the query set,
     * dataset, info about the ground truth and its parameters like the number
     * of nearest neighbours etc.
     */
    public void storeErrorOnDistForQuery(Object queryObjId, float errorOnDist, Object... additionalParametersToStore);

}
