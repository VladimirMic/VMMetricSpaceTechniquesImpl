package vm.objTransforms.perform;

import java.util.Iterator;
import java.util.List;
import vm.metricSpace.Dataset;
import vm.metricSpace.MetricSpacesStorageInterface;
import vm.objTransforms.MetricObjectsParallelTransformerImpl;
import vm.objTransforms.objectToSketchTransformators.AbstractObjectToSketchTransformator;
import vm.objTransforms.objectToSketchTransformators.SketchingGHP;
import vm.objTransforms.storeLearned.GHPSketchingPivotPairsStoreInterface;

/**
 *
 * @author Vlada
 */
public class TransformDataToGHPSketches {

    private final Dataset dataset;
    private final GHPSketchingPivotPairsStoreInterface storageOfPivotPairs;
    private final MetricSpacesStorageInterface storageForSketches;
    private final float balance;
    private final int pivotCount;

    public TransformDataToGHPSketches(Dataset dataset, GHPSketchingPivotPairsStoreInterface storageOfPivotPairs, MetricSpacesStorageInterface storageForSketches) {
        this(dataset, storageOfPivotPairs, storageForSketches, 0.5f, 512);
    }

    public TransformDataToGHPSketches(Dataset dataset, GHPSketchingPivotPairsStoreInterface storageOfPivotPairs, MetricSpacesStorageInterface storageForSketches, float balance, int pivotCount) {
        this.dataset = dataset;
        this.storageOfPivotPairs = storageOfPivotPairs;
        this.balance = balance;
        this.pivotCount = pivotCount;
        this.storageForSketches = storageForSketches;
    }

    public void createSketchesForDatasetPivotsAndQueries(int[] sketchesLengths) {
        List pivots = dataset.getPivots(pivotCount);
        for (int sketchLength : sketchesLengths) {
            AbstractObjectToSketchTransformator sketchingTechnique = new SketchingGHP(dataset.getDistanceFunction(), dataset.getMetricSpace(), pivots, false);
            String sketchesName = sketchingTechnique.getNameOfTransformedSetOfObjects(dataset.getDatasetName(), sketchLength, balance);
            sketchingTechnique.setPivotPairsFromStorage(storageOfPivotPairs, sketchesName);

            MetricObjectsParallelTransformerImpl parallelTransformer = new MetricObjectsParallelTransformerImpl(sketchingTechnique, storageForSketches, sketchesName);
            Iterator pivotsIt = dataset.getPivots(-1).iterator();
            Iterator queriesIt = dataset.getMetricQueryObjects().iterator();
            Iterator dataIt = dataset.getMetricObjectsFromDataset();
            parallelTransformer.processIteratorSequentially(pivotsIt, MetricSpacesStorageInterface.OBJECT_TYPE.PIVOT_OBJECT);
            parallelTransformer.processIteratorSequentially(queriesIt, MetricSpacesStorageInterface.OBJECT_TYPE.QUERY_OBJECT);
            parallelTransformer.processIteratorInParallel(dataIt, MetricSpacesStorageInterface.OBJECT_TYPE.DATASET_OBJECT, MetricObjectsParallelTransformerImpl.PARALLELISATION);
        }

    }

}
