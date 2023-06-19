package vm.objTransforms.perform;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    private static final Logger LOG = Logger.getLogger(TransformDataToGHPSketches.class.getName());
    private static final Integer IMPLICIT_PIVOT_COUNT = 512;

    private final Dataset dataset;
    private final GHPSketchingPivotPairsStoreInterface storageOfPivotPairs;
    private final MetricSpacesStorageInterface storageForSketches;
    private final float balance;
    private final int pivotCount;

    public TransformDataToGHPSketches(Dataset dataset, GHPSketchingPivotPairsStoreInterface storageOfPivotPairs, MetricSpacesStorageInterface storageForSketches) {
        this(dataset, storageOfPivotPairs, storageForSketches, 0.5f, IMPLICIT_PIVOT_COUNT);
        LOG.log(Level.WARNING, "Using implicit pivot count {0}", IMPLICIT_PIVOT_COUNT);
    }

    public TransformDataToGHPSketches(Dataset dataset, GHPSketchingPivotPairsStoreInterface storageOfPivotPairs, MetricSpacesStorageInterface storageForSketches, float balance, int pivotCount) {
        this.dataset = dataset;
        this.storageOfPivotPairs = storageOfPivotPairs;
        this.balance = balance;
        this.pivotCount = pivotCount;
        this.storageForSketches = storageForSketches;
    }

    public AbstractObjectToSketchTransformator createSketchesForDatasetPivotsAndQueries(int[] sketchesLengths, String[] sketchesPivotPairsNames) {
        AbstractObjectToSketchTransformator sketchingTechnique = null;
        for (int i = 0; i < sketchesLengths.length; i++) {
            int sketchesLength = sketchesLengths[i];
            List pivots = dataset.getPivots(pivotCount);
            sketchingTechnique = new SketchingGHP(dataset.getDistanceFunction(), dataset.getMetricSpace(), pivots, false, false);
            String producedSketchesName = sketchingTechnique.getNameOfTransformedSetOfObjects(dataset.getDatasetName(), sketchesLength, balance);

            if (sketchesPivotPairsNames[i] == null) {
                sketchesPivotPairsNames[i] = producedSketchesName;
            }
            sketchingTechnique.setPivotPairsFromStorage(storageOfPivotPairs, sketchesPivotPairsNames[i]);

            MetricObjectsParallelTransformerImpl parallelTransformer = new MetricObjectsParallelTransformerImpl(sketchingTechnique, storageForSketches, producedSketchesName);
            Iterator pivotsIt = dataset.getPivots(-1).iterator();
            Iterator queriesIt = dataset.getMetricQueryObjects().iterator();
            Iterator dataIt = dataset.getMetricObjectsFromDataset();
            parallelTransformer.processIteratorSequentially(pivotsIt, MetricSpacesStorageInterface.OBJECT_TYPE.PIVOT_OBJECT);
            parallelTransformer.processIteratorSequentially(queriesIt, MetricSpacesStorageInterface.OBJECT_TYPE.QUERY_OBJECT);
            parallelTransformer.processIteratorInParallel(dataIt, MetricSpacesStorageInterface.OBJECT_TYPE.DATASET_OBJECT, vm.javatools.Tools.PARALELISATION);
        }
        return sketchingTechnique;
    }

}
