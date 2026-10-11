package vm.searchSpace.distance.impl;

import vm.searchSpace.distance.AbstractDistanceFunction;
import org.apache.commons.math3.linear.RealMatrix;
import vm.datatools.Tools;

/**
 *
 * @author au734419
 */
public class QuadraticFormDistance extends AbstractDistanceFunction<float[]> {

    private final RealMatrix matrix;
    private int dimension;

    public QuadraticFormDistance(String numpyFileWithMatrix) {
        this(Tools.loadNpyMatrix(numpyFileWithMatrix));
    }

    public QuadraticFormDistance(RealMatrix matrix) {
        this.matrix = matrix;
        if (matrix.getColumnDimension() != matrix.getRowDimension()) {
            throw new IllegalArgumentException("Provided matrix must be a square matrix, but it is: " + matrix.getColumnDimension() + " times " + matrix.getRowDimension());
        }
        dimension = matrix.getColumnDimension();
    }

    @Override
    public float getDistance(float[] o1, float[] o2) {
        if (o2.length != o1.length) {
            throw new IllegalArgumentException("Cannot compute distance on different vector dimensions (" + o1.length + ", " + o2.length + ")");
        }
        int n = o1.length;
        if (dimension != n) {
            throw new IllegalArgumentException("Matrix A dimensions (" + dimension + "x" + dimension + ") must match vector dimensions (" + n + ")");
        }
        // 1. Calculate difference vector d = (o1 - o2)
        double[] diff = new double[n];
        for (int i = 0; i < n; i++) {
            diff[i] = o1[i] - o2[i];
        }
        // 2. Multiply matrix with diff vector: A * diff
        double[] aMulDiff = matrix.operate(diff);

        // 3. Compute quadratic form: diff^T * (A * diff) using dot product
        double quadraticFormSum = 0.0;
        for (int i = 0; i < n; i++) {
            quadraticFormSum += diff[i] * aMulDiff[i];
        }

        // Handle potential tiny negative values caused by floating-point inaccuracy
        if (quadraticFormSum < 0) {
            quadraticFormSum = 0.0;
        }

        return (float) Math.sqrt(quadraticFormSum);
    }

    @Override
    public String getName() {
        return "Quadratic form distance";
    }

    public static QuadraticFormDistance getFromNumpyFile(String path) {
        return null;
    }

}
