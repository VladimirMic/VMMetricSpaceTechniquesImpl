package vm.searchSpace.distance;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;

/**
 *
 * @author Vlada
 * @param <T>
 */
public abstract class AbstractDistanceFunction<T> {

    public abstract float getDistance(T obj1, T obj2);

    public float getDistance(T obj1, T obj2, Object... additionalParams) {
        return getDistance(obj1, obj2);
    }

    public abstract String getName();

    public Class getClassOfComparedData() {
        ParameterizedType pt = (ParameterizedType) getClass().getGenericSuperclass();
        return (Class<?>) pt.getActualTypeArguments()[0];
    }

    public static float getRatioOfTripletsViolatingTriangleIneqnality(float[][] dists) {
        int denom = 0;
        int num = 0;
        for (int i = 0; i < dists.length - 2; i++) {
            for (int j = i + 1; j < dists.length - 1; j++) {
                for (int k = j + 1; k < dists.length; k++) {
                    if (dists[k][j] > 0 && dists[i][k] > 0 && dists[i][j] > 0) {
                        float[] tmp = new float[]{dists[i][j], dists[i][k], dists[k][j]};
                        Arrays.sort(tmp);
                        boolean violates = tmp[0] + tmp[1] < tmp[2];
                        denom++;
                        if (violates) {
                            num++;
                        }
                    }
                }
            }
        }
        return ((float) num) / denom;
    }

}
