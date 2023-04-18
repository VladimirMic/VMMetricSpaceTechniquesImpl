package vm.objTransforms.perform;

import vm.objTransforms.MetricObjectTransformerInterface;
import vm.metricSpace.AbstractMetricSpace;

/**
 *
 * @author Vlada
 */
public class PCAMetricObjectTransformer implements MetricObjectTransformerInterface {

    private final float[] meansOverColumns;
    private final float[][] pcaMatrix;
    private final AbstractMetricSpace<float[]> floatVectorSpace;

    /**
     *
     * @param pcaMatrix matrix of size origVecDim * newDimensionality, e.g.,
     * vtMatrix from the SVD
     * @param meansOverColumns means that are subtracted from the vector before
     * the multiplication with the matrix
     * @param floatVectorSpace metric space that extracts the float vector from
     * the metric object, and encapsulates the resulting vector as a metric
     * object with the same ID
     */
    public PCAMetricObjectTransformer(float[][] pcaMatrix, float[] meansOverColumns, AbstractMetricSpace<float[]> floatVectorSpace) {
        this.meansOverColumns = meansOverColumns;
        this.pcaMatrix = pcaMatrix;
        this.floatVectorSpace = floatVectorSpace;
    }

    @Override
    public Object transformMetricObject(Object obj, Object... params) {
        Object objID = floatVectorSpace.getIDOfMetricObject(obj);
        float[] vector = floatVectorSpace.getDataOfMetricObject(obj);
        int length = pcaMatrix.length;
        if (params.length > 0) {
            length = Math.min(length, (int) params[0]);
        }
        final float[] ret = new float[length];
        for (int i = 0; i < length; i++) {
            float[] matrixRow = pcaMatrix[i];
            for (int j = 0; j < vector.length; j++) {
                float v = vector[j];
                ret[i] += v * matrixRow[j];
            }
        }
        return floatVectorSpace.createMetricObject(objID, ret);
    }

    @Override
    public String getNameOfTransformedSetOfObjects(String origDatasetName, Object... otherParams) {
        return origDatasetName + "_" + getTechniqueAbbreviation() + pcaMatrix.length;
    }

    @Override
    public final String getTechniqueAbbreviation() {
        return "PCA";
    }

}
