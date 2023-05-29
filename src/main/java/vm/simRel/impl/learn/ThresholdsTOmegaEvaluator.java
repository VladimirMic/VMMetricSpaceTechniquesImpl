package vm.simRel.impl.learn;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.metricSpace.Dataset;
import vm.search.SearchingAlgorithm;
import vm.search.impl.SimRelSeqScanKNNCandSetThenFullDistEval;
import vm.simRel.impl.learn.storeLearnt.SimRelEuclidThresholdsTOmegaStorage;

/**
 *
 * @author Vlada
 */
public class ThresholdsTOmegaEvaluator {

    private static final Logger LOG = Logger.getLogger(ThresholdsTOmegaEvaluator.class.getName());

    private final int querySampleCount;
    private final int dataSampleCount;
    private final int kPCA;
    private final float percentileWrong;

    public ThresholdsTOmegaEvaluator(int querySampleCount, int dataSampleCount, int kPCA, float percentileWrong) {
        this.querySampleCount = querySampleCount;
        this.dataSampleCount = dataSampleCount;
        this.kPCA = kPCA;
        this.percentileWrong = percentileWrong;
    }

    public float[] learnTOmegaThresholds(Dataset<float[]> pcaDataset, SimRelEuclidThresholdsTOmegaStorage storage) {
        List<Object> querySamples = pcaDataset.getPivots(querySampleCount);
        List<Object> sampleOfDataset = pcaDataset.getSampleOfDataset(dataSampleCount);
        float[] vector = pcaDataset.getMetricSpace().getDataOfMetricObject(sampleOfDataset.get(0));
        int pcaLength = vector.length;
        SimRelEuclideanPCAForLearning simRelLearn = new SimRelEuclideanPCAForLearning(pcaLength);
        SearchingAlgorithm alg = new SimRelSeqScanKNNCandSetThenFullDistEval(simRelLearn, kPCA, pcaDataset.getDistanceFunction());

        simRelLearn.resetLearning(pcaLength);
        for (int i = 0; i < querySamples.size(); i++) {
            Object queryObj = querySamples.get(i);
            simRelLearn.resetCounters(pcaLength);
            alg.candSetKnnSearch(pcaDataset.getMetricSpace(), queryObj, kPCA, sampleOfDataset.iterator());
            LOG.log(Level.INFO, "Learning tresholds with the query obj {0}", new Object[]{i + 1});
        }
        float[] ret = simRelLearn.getDiffWhenWrong(percentileWrong, -1);
        storage.store(ret, pcaDataset.getDatasetName(), querySampleCount, dataSampleCount, pcaLength, kPCA);
        return ret;
    }

}
