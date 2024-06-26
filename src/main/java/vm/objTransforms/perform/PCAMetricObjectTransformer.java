package vm.objTransforms.perform;

import vm.objTransforms.MetricObjectTransformerInterface;
import vm.metricSpace.AbstractMetricSpace;

/**
 *
 * @author Vlada
 */
public class PCAMetricObjectTransformer implements MetricObjectTransformerInterface {

    protected final float[] meansOverColumns;
    protected final float[][] pcaMatrix;
    protected final AbstractMetricSpace<float[]> origFloatVectorSpace;
    protected final AbstractMetricSpace<float[]> pcaMetricSpace;

    /**
     *
     * @param pcaMatrix matrix of size origVecDim * newDimensionality, e.g.,
     * vtMatrix from the SVD
     * @param meansOverColumns means that are subtracted from the vector before
     * the multiplication with the matrix
     * @param origFloatVectorSpace metric space that extracts the float vector
     * from the metric object, and encapsulates the resulting vector as a metric
     * object with the same ID
     * @param pcaMetricSpace
     */
    public PCAMetricObjectTransformer(float[][] pcaMatrix, float[] meansOverColumns, AbstractMetricSpace<float[]> origFloatVectorSpace, AbstractMetricSpace<float[]> pcaMetricSpace) {
        this.meansOverColumns = meansOverColumns;
        this.pcaMatrix = pcaMatrix;
        this.origFloatVectorSpace = origFloatVectorSpace;
        this.pcaMetricSpace = pcaMetricSpace;
    }

    @Override
    public Object transformMetricObject(Object obj, Object... params) {
        Comparable objID = origFloatVectorSpace.getIDOfMetricObject(obj);
        float[] vector = origFloatVectorSpace.getDataOfMetricObject(obj);
        int length = pcaMatrix.length;
        final float[] ret = new float[length];
        for (int i = 0; i < length; i++) {
            float[] matrixRow = pcaMatrix[i];
            for (int j = 0; j < vector.length; j++) {
                float v = vector[j];
                ret[i] += v * matrixRow[j];
            }
        }
        return pcaMetricSpace.createMetricObject(objID, ret);
    }

    @Override
    public String getNameOfTransformedSetOfObjects(String origDatasetName, Object... otherParams) {
        return origDatasetName + "_" + getTechniqueAbbreviation() + pcaMatrix.length;
    }

    @Override
    public String getTechniqueAbbreviation() {
        return "PCA";
    }

}
