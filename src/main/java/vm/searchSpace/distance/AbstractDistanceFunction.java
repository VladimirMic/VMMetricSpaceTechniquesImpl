package vm.searchSpace.distance;

import java.lang.reflect.ParameterizedType;

/**
 *
 * @author Vlada
 * @param <T>
 */
public abstract class AbstractDistanceFunction<T>  {

    public abstract float getDistance(T obj1, T obj2);

    public float getDistance(T obj1, T obj2, Object... additionalParams) {
        return getDistance(obj1, obj2);
    }

    public abstract String getName();

    public Class getClassOfComparedData() {
        ParameterizedType pt = (ParameterizedType) getClass().getGenericSuperclass();
        return (Class<?>) pt.getActualTypeArguments()[0];
    }

}
