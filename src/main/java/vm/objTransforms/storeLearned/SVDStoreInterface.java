package vm.objTransforms.storeLearned;

/**
 *
 * @author Vlada
 */
public interface SVDStoreInterface {

    public void storeSVD(float[] meansOverColumns, float[] singularValues, float[][] matrixU, float[][] matrixVT, Object... additionalInfoToStoreWithPCA);

    public float[][] getVTMatrix(Object... params);

    public float[][] getUMatrix(Object... params);

    public float[] getSingularValues(Object... params);

    public float[] getMeansOverColumns(Object... params);

}
