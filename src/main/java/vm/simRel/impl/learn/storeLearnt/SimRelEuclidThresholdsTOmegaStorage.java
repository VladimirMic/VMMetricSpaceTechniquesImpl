package vm.simRel.impl.learn.storeLearnt;

/**
 *
 * @author Vlada
 */
public interface SimRelEuclidThresholdsTOmegaStorage {

    public void store(float[] thresholds, String datasetName, int querySampleCount, int dataSampleCount, int pcaLength, int kPCA);

    public float[] load(String datasetName, int querySampleCount, int dataSampleCount, int pcaLength, int kPCA);
}
