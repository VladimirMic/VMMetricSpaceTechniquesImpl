package vm.simRel.impl.learn.storeLearnt;

/**
 *
 * @author Vlada
 */
public interface SimRelEuclidThresholdsTOmegaStorage {

    public void store(float[][] thresholds, String datasetName);

    public float[][] load(String datasetName);
}
