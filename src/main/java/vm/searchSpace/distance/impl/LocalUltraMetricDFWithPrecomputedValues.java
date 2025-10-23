package vm.searchSpace.distance.impl;

import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;
import vm.searchSpace.Dataset;
import vm.searchSpace.distance.storedPrecomputedDistances.AbstractPrecomputedDistancesMatrixLoader;

/**
 * This method does not work with symmetry of distances, i.e., rows and columns
 * do not have to be the same.
 *
 * @author au734419
 * @param <T>
 */
public class LocalUltraMetricDFWithPrecomputedValues<T> extends DFWithPrecomputedValues<Object> {

    private static final Logger LOG = Logger.getLogger(LocalUltraMetricDFWithPrecomputedValues.class.getName());

    public LocalUltraMetricDFWithPrecomputedValues(AbstractPrecomputedDistancesMatrixLoader pd, Dataset dataset, boolean alreadyLocallyUtrametric) {
        super(dataset.getSearchSpace(), pd, dataset, dataset.getDistanceFunction(), dataset.getPrecomputedDatasetSize());
        if (!alreadyLocallyUtrametric) {
            makeItLocallyUtrametric();
        }
    }

    private void makeItLocallyUtrametric() {
        float[][] newDists = new float[dists.length][dists.length];

        // Compute only for upper triangle
        int counter = 0;
        for (Map.Entry<Comparable, Integer> columnEntry : columnHeaders.entrySet()) {
            counter++;
            System.out.print(counter + ";");
            if (counter % 50 == 0) {
                System.out.println("");
            }
            int j = columnEntry.getValue();
            for (Map.Entry<Comparable, Integer> rowEntry : rowHeaders.entrySet()) {
                int i = rowEntry.getValue();
                if (dists[i][j] == 0f) {
                    newDists[i][j] = 0f;
                    continue;
                }
                float minVal = dists[i][j];

                // iterate over k
                for (int k = 0; k < dists.length; k++) {
                    if (dists[i][k] == 0f || dists[j][k] == 0f) {
                        continue; // ignore zeros
                    }
                    float maxVal = dists[i][k] > dists[j][k] ? dists[i][k] : dists[j][k];
                    if (maxVal < minVal) {
                        minVal = maxVal;
                    }
                }
                newDists[i][j] = minVal;
            }
        }

        // Replace old matrix
        for (int i = 0; i < dists.length; i++) {
            System.arraycopy(newDists[i], 0, dists[i], 0, dists.length);
        }
    }

    public boolean isUltrametric() {
        int n = dists.length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < n; k++) {
                    if (dists[i][j] > Math.max(dists[i][k], dists[j][k]) + 1e-6) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public float getRatioOfTripletsViolatingUltraMetricRule() {
        int denom = 0;
        int num = 0;
        for (int i = 0; i < dists.length - 2; i++) {
            System.err.print(i + ";");
            if (i % 50 == 0) {
                System.err.println("");
            }
            for (int j = i + 1; j < dists.length - 1; j++) {
                for (int k = j + 1; k < dists.length; k++) {
                    if (dists[k][j] > 0 && dists[i][k] > 0 && dists[i][j] > 0) {
                        float[] tmp = new float[]{dists[i][j], dists[i][k], dists[k][j]};
                        Arrays.sort(tmp);
                        boolean violates = tmp[2] > tmp[1];
                        denom++;
                        if (violates) {
                            num++;
                        }
                    }
                }
            }
        }
        return ((float) num) / denom;
    }

}
