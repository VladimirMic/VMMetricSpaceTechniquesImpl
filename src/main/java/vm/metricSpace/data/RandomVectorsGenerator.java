/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.metricSpace.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.AbstractMetricSpacesStorage;

/**
 *
 * @author au734419 based on the BP of Matej Hamala (469228), Masaryk University
 * Brno
 */
public class RandomVectorsGenerator {

    private static final Logger LOG = Logger.getLogger(RandomVectorsGenerator.class.getName());

    private static int[] dimensions;
    private static final AbstractMetricSpacesStorage.OBJECT_TYPE[] OBJECT_TYPES = {AbstractMetricSpacesStorage.OBJECT_TYPE.DATASET_OBJECT, AbstractMetricSpacesStorage.OBJECT_TYPE.QUERY_OBJECT, AbstractMetricSpacesStorage.OBJECT_TYPE.PIVOT_OBJECT};
    private static final String[] OBJECT_TYPES_ID_PREFIXES = {"D", "Q", "P"};
    private static int[] sizesOfGeneratedSets;

    private final AbstractMetricSpacesStorage storage;
    private final AbstractMetricSpace metricSpace;

    public RandomVectorsGenerator(AbstractMetricSpace metricSpace, AbstractMetricSpacesStorage storage) {
        dimensions = new int[]{10, 15, 25, 30, 35, 40, 50, 60, 70, 80, 90, 100};
        this.storage = storage;
        this.metricSpace = metricSpace;
        sizesOfGeneratedSets = new int[]{1000000, 1000, 2560};
    }

    public void generate() {
        Random generator = new Random();
        for (int dimension : dimensions) {
            String datasetName = getDatasetName(dimension, true);
            for (int i = 0; i < OBJECT_TYPES.length; i++) {
                List<Object> objects = generateData(sizesOfGeneratedSets[i], dimension, OBJECT_TYPES_ID_PREFIXES[i], generator, random -> {
                    return (float) random.nextFloat() - 0.5f;
                });
//                generateData(objectTypesSampleSizes[i], dimension, objectTypesPrefixes[i], generator, x -> {
//                    return (float) x.nextGaussian();
//                });
                switch (i) {
                    case 0: {
                        storage.storeObjectsToDataset(objects.iterator(), -1, datasetName, true);
                        break;
                    }
                    case 1: {
                        storage.storeQueryObjects(objects, datasetName, true);
                        break;
                    }
                    case 2: {
                        storage.storePivots(objects, datasetName, true);
                        break;
                    }
                }
            }
            LOG.log(Level.INFO, "Data for dimension {0} generated", dimension);
        }
    }

    /**
     * Generates specified random {@link ObjectFloatVector} data objects.
     *
     * @param sampleSize quantity of generated objects
     * @param sampleDimension objects dimension
     * @param idPreffix prefix for ID of generated objects
     * @param random pseudorandom number generator
     * @param generatingFunction generator's method for generating single
     * coordinates
     * @return map of generated objects
     */
    public List<Object> generateData(int sampleSize, int sampleDimension, String idPreffix, Random random, Function<Random, Float> generatingFunction) {
        List<Object> ret = new ArrayList<>();
        for (int i = 0; i < sampleSize; i++) {
            float[] data = generateRandomVector(sampleDimension, random, generatingFunction);
            String id = idPreffix + i;
            Object object = metricSpace.createMetricObject(id, data);
            ret.add(object);
        }
        return ret;
    }

    /**
     * Generates random vector.
     *
     * @param sampleDimension vector dimension
     * @param generator pseudorandom number generator
     * @param generatingFunction generator's method for generating single
     * coordinates
     * @return random generated vector
     */
    private float[] generateRandomVector(int sampleDimension, Random generator, Function<Random, Float> generatingFunction) {
        float[] vector = new float[sampleDimension];
        for (int i = 0; i < sampleDimension; i++) {
            vector[i] = generatingFunction.apply(generator);
        }
        return vector;
    }

    private String getDatasetName(int dimension, boolean uniform) {
        String distribution = uniform ? "uniform" : "gaussian";
        return "random_" + dimension + "dim_" + distribution + "_1M";
    }

}
