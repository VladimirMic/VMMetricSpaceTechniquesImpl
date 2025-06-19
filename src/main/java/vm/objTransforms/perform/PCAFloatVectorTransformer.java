package vm.objTransforms.perform;

import vm.searchSpace.AbstractSearchSpace;
import vm.objTransforms.SearchObjectTransformerInterface;

/**
 *
 * @author Vlada
 */
public class PCAFloatVectorTransformer implements SearchObjectTransformerInterface {

    protected final float[] meansOverColumns;
    protected final float[][] pcaMatrix;
    protected final AbstractSearchSpace<float[]> origFloatVectorSpace;
    protected final AbstractSearchSpace<float[]> pcaSearchSpace;

    /**
     *
     * @param pcaMatrix matrix of size origVecDim * newDimensionality, e.g.,
     * vtMatrix from the SVD
     * @param meansOverColumns means that are subtracted from the vector before
     * the multiplication with the matrix
     * @param origFloatVectorSpace search space that extracts the float vector
     * from the search object, and encapsulates the resulting vector as a search
     * object with the same ID
     * @param pcaSearchSpace
     */
    public PCAFloatVectorTransformer(float[][] pcaMatrix, float[] meansOverColumns, AbstractSearchSpace<float[]> origFloatVectorSpace, AbstractSearchSpace<float[]> pcaSearchSpace) {
        this.meansOverColumns = meansOverColumns;
        this.pcaMatrix = pcaMatrix;
        this.origFloatVectorSpace = origFloatVectorSpace;
        this.pcaSearchSpace = pcaSearchSpace;
    }

    @Override
    public Object transformSearchObject(Object obj, Object... params) {
        Comparable objID = origFloatVectorSpace.getIDOfObject(obj);
        float[] vector = origFloatVectorSpace.getDataOfObject(obj);
        int length = pcaMatrix.length;
        final float[] ret = new float[length];
        for (int i = 0; i < length; i++) {
            float[] matrixRow = pcaMatrix[i];
            for (int j = 0; j < vector.length; j++) {
                float v = vector[j];
                ret[i] += v * matrixRow[j];
            }
        }
        return pcaSearchSpace.createSearchObject(objID, ret);
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
