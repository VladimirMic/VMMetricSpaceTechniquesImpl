package vm.simRel.impl.learn;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.voronoiPartitioning.StorageLearnedVoronoiPartitioningInterface;
import vm.search.SearchingAlgorithm;
import vm.search.impl.SimRelSeqScanKNNCandSet;
import vm.search.impl.VoronoiPartitionsCandSetIdentifier;
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

    public float[] learnTOmegaThresholds(Dataset fullDataset, Dataset<float[]> pcaDataset, SimRelEuclidThresholdsTOmegaStorage simRelStorage, VoronoiPartitionsCandSetIdentifier algVoronoi, int candidatesOfVoronoi) {
        List<Object> querySamples = fullDataset.getPivots(querySampleCount);
        AbstractMetricSpace metricSpaceOfFullDataset = fullDataset.getMetricSpace();
        AbstractMetricSpace<float[]> pcaDatasetMetricSpace = pcaDataset.getMetricSpace();

        SimRelEuclideanPCAForLearning simRelLearn = null;
        int pcaLength = 0;
        Map pcaOMap = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(pcaDatasetMetricSpace, pcaDataset.getMetricObjectsFromDataset(), false);
        //Map pcaOMap = pcaDataset.getKeyValueStorage();
        Map pcaQueriesMap = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(pcaDatasetMetricSpace, pcaDataset.getPivots(-1), false);

        for (int i = 0; i < querySamples.size(); i++) {
            Object fullQueryObj = querySamples.get(i);
            Object queryObjId = metricSpaceOfFullDataset.getIDOfMetricObject(fullQueryObj);
            AbstractMap.SimpleEntry<Object, float[]> pcaQueryObj = (AbstractMap.SimpleEntry<Object, float[]>) pcaQueriesMap.get(queryObjId);
            List<Object> candidatesIDs = algVoronoi.candSetKnnSearch(metricSpaceOfFullDataset, fullQueryObj, candidatesOfVoronoi, null);
            List<Object> pcaOfCandidates = Tools.filterMap(pcaOMap, candidatesIDs, false);

            if (simRelLearn == null) {
                float[] vector = pcaDatasetMetricSpace.getDataOfMetricObject(pcaOfCandidates.get(0));
                pcaLength = vector.length;
                simRelLearn = new SimRelEuclideanPCAForLearning(pcaLength);
            }
            SearchingAlgorithm simRelAlg = new SimRelSeqScanKNNCandSet(simRelLearn, kPCA);

            simRelLearn.resetCounters(pcaLength);
            simRelAlg.candSetKnnSearch(pcaDataset.getMetricSpace(), pcaQueryObj, kPCA, pcaOfCandidates.iterator());
            LOG.log(Level.INFO, "Learning tresholds with the query obj {0}, i.e., qID {0}", new Object[]{i + 1, queryObjId});
        }
        float[][] ret = simRelLearn.getDiffWhenWrong(percentileWrong);
        simRelStorage.store(ret, pcaDataset.getDatasetName() + "_voronoiP" + algVoronoi.getNumberOfPivots() + "_O" + candidatesOfVoronoi);
        return ret[0];
    }

    public float[] learnTOmegaThresholds(Dataset<float[]> pcaDataset, SimRelEuclidThresholdsTOmegaStorage storage) {
        List<Object> querySamples = pcaDataset.getPivots(querySampleCount);
        AbstractMetricSpace<float[]> metricSpace = pcaDataset.getMetricSpace();
        List<Object> sampleOfDataset = pcaDataset.getSampleOfDataset(dataSampleCount);
        float[] vector = metricSpace.getDataOfMetricObject(sampleOfDataset.get(0));
        int pcaLength = vector.length;
        SimRelEuclideanPCAForLearning simRelLearn = new SimRelEuclideanPCAForLearning(pcaLength);
        SearchingAlgorithm alg = new SimRelSeqScanKNNCandSet(simRelLearn, kPCA);

        simRelLearn.resetLearning(pcaLength);
        for (int i = 0; i < querySamples.size(); i++) {
            Object queryObj = querySamples.get(i);
            String qID = metricSpace.getIDOfMetricObject(queryObj).toString();
            simRelLearn.resetCounters(pcaLength);
            alg.candSetKnnSearch(pcaDataset.getMetricSpace(), queryObj, kPCA, sampleOfDataset.iterator());
            LOG.log(Level.INFO, "Learning tresholds with the query obj {0}, i.e., qID {0}", new Object[]{i + 1, qID});
        }
        float[][] ret = simRelLearn.getDiffWhenWrong(percentileWrong);
        storage.store(ret, pcaDataset.getDatasetName());
        return ret[0];
    }

}
