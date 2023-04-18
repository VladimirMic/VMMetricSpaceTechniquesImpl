package vm.simRel;

/**
 *
 * @author Vlada
 * @param <T>
 */
public interface SimRelInterface<T> {

    /**
     * Return -1 iff d(q, o1) \leq d(q, o2) with a high probability, and +1 iff
     * d(q, o1) \geq d(q, o2) with a high probability. Otherwise 0. Semantics of
     * the distance function d must be specified in a specific implementation.
     *
     * @param q
     * @param o1
     * @param o2
     * @return
     */
    public short getMoreSimilar(T q, T o1, T o2);
}
