package vm.simRel.impl.learn.storeLearnt;

import java.util.Arrays;

/**
 *
 * @author Vlada
 */
public abstract class SimRelEuclidThresholdsTOmegaStorage {

    public static final float[] PERCENTILES = new float[]{0.5f, 0.55f, 0.6f, 0.65f, 0.7f, 0.75f, 0.8f, 0.85f, 0.9f, 0.95f, 0.97f, 0.98f, 0.99f, 1f};
    public static final Integer IMPLICIT_PERCENTILE_IDX = 7;

    public static int percentileToArrayIdx(float perc) {
        int pos = Arrays.binarySearch(PERCENTILES, perc);
        return pos;
    }

    public abstract void store(float[][] thresholds, String datasetName);

    public abstract float[][] load(String datasetName);
}
