package vm.objTransforms.perform;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.searchSpace.Dataset;
import vm.searchSpace.AbstractSearchSpacesStorage;
import vm.objTransforms.SearchObjectsParallelTransformerImpl;
import vm.objTransforms.objectToSketchTransformators.AbstractObjectToSketchTransformator;
import vm.objTransforms.objectToSketchTransformators.SketchingGHP;
import vm.objTransforms.storeLearned.PivotPairsStoreInterface;

/**
 *
 * @author Vlada
 */
public class TransformDataToGHPSketches {

    private static final Logger LOG = Logger.getLogger(TransformDataToGHPSketches.class.getName());
    private static final Integer IMPLICIT_PIVOT_COUNT = 512;

    private final Dataset dataset;
    private final PivotPairsStoreInterface storageOfPivotPairs;
    private final AbstractSearchSpacesStorage storageForSketches;
    private final float balance;
    private final int pivotCount;

    public TransformDataToGHPSketches(Dataset dataset, PivotPairsStoreInterface storageOfPivotPairs, AbstractSearchSpacesStorage storageForSketches) {
        this(dataset, storageOfPivotPairs, storageForSketches, 0.5f, IMPLICIT_PIVOT_COUNT);
        LOG.log(Level.WARNING, "Using implicit pivot count {0}", IMPLICIT_PIVOT_COUNT);
    }

    public TransformDataToGHPSketches(Dataset dataset, PivotPairsStoreInterface storageOfPivotPairs, AbstractSearchSpacesStorage storageForSketches, float balance, int pivotCount) {
        this.dataset = dataset;
        this.storageOfPivotPairs = storageOfPivotPairs;
        this.balance = balance;
        this.pivotCount = pivotCount;
        this.storageForSketches = storageForSketches;
    }

    public AbstractObjectToSketchTransformator createSketchesForDatasetPivotsAndQueries(int[] sketchesLengths, Object ... params) {
        return createSketchesForDatasetPivotsAndQueries(sketchesLengths, null, params);
    }

    public AbstractObjectToSketchTransformator createSketchesForDatasetPivotsAndQueries(int[] sketchesLengths, String[] sketchesPivotPairsNames, Object ... params) {
        AbstractObjectToSketchTransformator sketchingTechnique = null;
        if (sketchesPivotPairsNames == null) {
            sketchesPivotPairsNames = new String[sketchesLengths.length];
        }
        for (int i = 0; i < sketchesLengths.length; i++) {
            int sketchesLength = sketchesLengths[i];
            List pivots = dataset.getPivots(pivotCount);
            sketchingTechnique = new SketchingGHP(dataset.getDistanceFunction(), dataset.getSearchSpace(), pivots, false);
            String producedDatasetName = sketchingTechnique.getNameOfTransformedSetOfObjects(dataset.getDatasetName(), sketchesLength, balance);
            String producedQuerySetName = sketchingTechnique.getNameOfTransformedSetOfObjects(dataset.getQuerySetName(), sketchesLength, balance);
            String producedPivotsName = sketchingTechnique.getNameOfTransformedSetOfObjects(dataset.getPivotSetName(), sketchesLength, balance);

            if (sketchesPivotPairsNames[i] == null) {
                sketchesPivotPairsNames[i] = producedDatasetName;
            }
            sketchingTechnique.setPivotPairsFromStorage(storageOfPivotPairs, sketchesPivotPairsNames[i]);

            SearchObjectsParallelTransformerImpl parallelTransformer = new SearchObjectsParallelTransformerImpl(sketchingTechnique, storageForSketches, producedDatasetName, producedQuerySetName, producedPivotsName);
            Iterator pivotsIt = dataset.getPivots(-1).iterator();
            Iterator queriesIt = dataset.getQueryObjects().iterator();
            Iterator dataIt = dataset.getSearchObjectsFromDataset();
            parallelTransformer.processIteratorSequentially(pivotsIt, AbstractSearchSpacesStorage.OBJECT_TYPE.PIVOT_OBJECT, params);
            parallelTransformer.processIteratorSequentially(queriesIt, AbstractSearchSpacesStorage.OBJECT_TYPE.QUERY_OBJECT, params);
            parallelTransformer.processIteratorInParallel(dataIt, AbstractSearchSpacesStorage.OBJECT_TYPE.DATASET_OBJECT, vm.javatools.Tools.PARALELISATION, params);
        }
        return sketchingTechnique;
    }

}
