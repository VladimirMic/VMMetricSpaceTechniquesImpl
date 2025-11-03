package vm.searchSpace.distance.impl;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.searchSpace.Dataset;
import vm.searchSpace.distance.storedPrecomputedDistances.AbstractPrecomputedDistancesMatrixSerializator;
import vm.searchSpace.distance.storedPrecomputedDistances.MainMemoryStoredPrecomputedDistances;

/**
 * This method does not work with symmetry of distances, i.e., rows and columns
 * do not have to be the same.
 *
 * @author au734419
 * @param <T>
 */
public class LocalUltraMetricDFWithPrecomputedValues<T> extends DFWithPrecomputedValues<T> {

    public static final String NAME = "LocallyUltraMetric";
    private static final Logger LOG = Logger.getLogger(LocalUltraMetricDFWithPrecomputedValues.class.getName());

    public LocalUltraMetricDFWithPrecomputedValues(AbstractPrecomputedDistancesMatrixSerializator pd, Dataset dataset) {
        super(dataset, pd, dataset.getPrecomputedDatasetSize());
        try {
            makeItLocallyUtrametric();
        } catch (InterruptedException ex) {
            Logger.getLogger(LocalUltraMetricDFWithPrecomputedValues.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void makeItLocallyUtrametric() throws InterruptedException {
        int columnCount = getColumnCount();
        float[][] newDists = new float[columnCount][columnCount];
        ExecutorService threadPool = vm.javatools.Tools.initExecutor();
        CountDownLatch latch = new CountDownLatch(columnCount);
        int counter = 0;
        Map<Comparable, Integer> columnHeaders = getColumnHeaders();
        Map<Comparable, Integer> rowHeaders = getRowHeaders();
        float[][] dists = getDists();
        for (Map.Entry<Comparable, Integer> rowEntry : rowHeaders.entrySet()) {
            counter++;
            final int counterCopy = counter;
            threadPool.execute(() -> {
                int rowIdx = rowEntry.getValue();
                for (Map.Entry<Comparable, Integer> columnEntry : columnHeaders.entrySet()) {
                    int columnIdx = columnEntry.getValue();
                    int rowIdxOfColumn = rowHeaders.get(columnEntry.getKey());
                    if (dists[rowIdx][columnIdx] == 0f) {
                        newDists[rowIdx][columnIdx] = 0f;
                        continue;
                    }
                    float minVal = dists[rowIdx][columnIdx];

                    // iterate over k
                    for (int k = 0; k < dists.length; k++) {
                        if (dists[rowIdx][k] == 0f || dists[rowIdxOfColumn][k] == 0f) {
                            continue; // ignore zeros
                        }
                        float maxVal = Math.max(dists[rowIdx][k], dists[rowIdxOfColumn][k]);
                        if (maxVal < minVal) {
                            minVal = maxVal;
                        }
                    }
                    newDists[rowIdx][columnIdx] = minVal;
                }
                System.out.print(counterCopy + ";");
                if (counterCopy % 50 == 0) {
                    System.out.println("");
                }
                latch.countDown();
            });
        }
        latch.await();
        // Replace old matrix
        MainMemoryStoredPrecomputedDistances newinstance = new MainMemoryStoredPrecomputedDistances(newDists, columnHeaders, rowHeaders);
        setDistsHolder(newinstance);
        threadPool.shutdown();
    }

    public boolean isUltrametric() {
        float[][] dists = getDists();
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
        float[][] dists = getDists();
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
