package vm.objTransforms.learning;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import vm.datatools.DataTypeConvertor;
import vm.searchSpace.ToolsSpaceDomain;
import vm.searchSpace.AbstractSearchSpace;
import vm.objTransforms.storeLearned.SVDStoreInterface;

/**
 *
 * @author Vlada
 */
public class LearnSVD {

    private static final Logger LOG = Logger.getLogger(LearnSVD.class.getName());

    private final AbstractSearchSpace<float[]> searchSpace;
    private final List<Object> sampleObjects;
    private final Object[] additionalInfoToStoreWithSVD;
    private final SVDStoreInterface storage;

    /**
     *
     * @param vectorSpace implementation of your matric space
     * @param storage Interface used to storeSearchObject the computed PCA
     * transformation
     * @param sampleObjects search objects used to learn PCA transform
     * @param additionalInfoToStoreWithSVD ALl other information that you wish
     * to storeSearchObject with the learned PCA. For instance, dataset
     * identifier, sample set size, etc.
     */
    public LearnSVD(AbstractSearchSpace<float[]> vectorSpace, SVDStoreInterface storage, List<Object> sampleObjects, Object... additionalInfoToStoreWithSVD) {
        this.searchSpace = vectorSpace;
        this.sampleObjects = sampleObjects;
        this.storage = storage;
        if (additionalInfoToStoreWithSVD != null) {
            this.additionalInfoToStoreWithSVD = additionalInfoToStoreWithSVD;
        } else {
            this.additionalInfoToStoreWithSVD = null;
        }
    }

    public void execute() {
        float[][] floatMatrix = ToolsSpaceDomain.transformSearchObjectsToVectorMatrix(searchSpace, sampleObjects);
        float[] meansOverColumns = getMeanValuesOfColumns(floatMatrix);
        double[][] normedMatrix = vm.mathtools.Tools.subtractColumnsMeansFromMatrix(floatMatrix, meansOverColumns);

        RealMatrix realMatrix = new Array2DRowRealMatrix(normedMatrix);
        LOG.log(Level.INFO, "Start evaluation of singular value decomposition. Sample set is of size {0}, vectors are of length: {1}", new Object[]{sampleObjects.size(), meansOverColumns.length});
        SingularValueDecomposition svd = new SingularValueDecomposition(realMatrix);
        LOG.log(Level.INFO, "Finished evaluation of singular value decomposition.");
        double[][] vTransposedMatrix = svd.getVT().getData();
        float[][] vTransposedMatrixFloats = DataTypeConvertor.doubleMatrixToFloatMatrix(vTransposedMatrix);
        float[][] uMatrix = DataTypeConvertor.doubleMatrixToFloatMatrix(svd.getU().getData());
        float[] singularValues = DataTypeConvertor.doublesToFloats(svd.getSingularValues());
        LOG.log(Level.INFO, "Finished evaluation of singular value decomposition. U matrix: {0} * {1}, vT matrix: {2} * {3}", new Object[]{uMatrix.length, uMatrix[0].length, vTransposedMatrixFloats.length, vTransposedMatrixFloats[0].length});

        storage.storeSVD(meansOverColumns, singularValues, uMatrix, vTransposedMatrixFloats, additionalInfoToStoreWithSVD);
    }

    private float[] getMeanValuesOfColumns(float[][] matrix) {
        float[] ret = new float[matrix[0].length];
        for (int i = 0; i < matrix.length; i++) {
            float[] row = matrix[i];
            for (int j = 0; j < row.length; j++) {
                ret[j] += row[j];
            }
        }
        for (int i = 0; i < ret.length; i++) {
            ret[i] = ret[i] / matrix.length;
        }
        return ret;
    }

}
