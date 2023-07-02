package vm.simRel.impl.learn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.metricSpace.distance.impl.L2OnFloatsArray;
import vm.simRel.SimRelInterface;
import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author Vlada
 */
public class SimRelEuclideanPCAForLearning implements SimRelInterface<float[]> {

    public static final Logger LOG = Logger.getLogger(SimRelEuclideanPCAForLearning.class.getName());
    private int simRelCounter;
    private int[] errorsPerCoord;
    private List<Float>[] diffsWhenWrongPerCoords;

    public SimRelEuclideanPCAForLearning(int prefixLength) {
        resetLearning(prefixLength);
    }

    public int getSimRelCounter() {
        return simRelCounter;
    }

    public int[] getErrorsPerCoord() {
        return errorsPerCoord;
    }

    public float[][] getDiffWhenWrong(float... percentiles) {
        return getDiffWhenWrong(-1, percentiles);
    }

    public float[][] getDiffWhenWrong(int numberOfCoordinates, float... percentiles) {
        if (numberOfCoordinates < 0) {
            numberOfCoordinates = diffsWhenWrongPerCoords.length;
        } else {
            numberOfCoordinates = Math.min(diffsWhenWrongPerCoords.length, numberOfCoordinates);
        }
        final float[][] ret = new float[percentiles.length][numberOfCoordinates];
        ExecutorService threadPool = vm.javatools.Tools.initExecutor(vm.javatools.Tools.PARALELISATION);
        CountDownLatch latch = new CountDownLatch(numberOfCoordinates);
        for (int i = 0; i < numberOfCoordinates; i++) {
            if (!diffsWhenWrongPerCoords[i].isEmpty()) {
                List<Float> diffsFinal = diffsWhenWrongPerCoords[i];
                int iFinal = i;
                threadPool.execute(() -> {
                    Collections.sort(diffsFinal);
                    for (int j = 0; j < percentiles.length; j++) {
                        float percentile = percentiles[j];
                        int idx;
                        if (percentile == 1f) {
                            idx = diffsFinal.size() - 1;
                        } else {
                            idx = (int) (Math.floor(diffsFinal.size() * percentile) - 1);
                        }
                        idx = Math.min(idx, diffsFinal.size() - 1);
                        idx = Math.max(0, idx);
                        ret[j][iFinal] = diffsFinal.get(idx);
                        System.out.println("Percentile: " + percentile + ",cord: " + iFinal + ", idx: " + idx + ", threshold: " + ret[j][iFinal] + ", number of errors: " + diffsFinal.size());
                    }
                    latch.countDown();
                });
            } else {
                latch.countDown();
            }

        }
        try {
            latch.await();
            threadPool.shutdown();
        } catch (InterruptedException ex) {
            Logger.getLogger(SimRelEuclideanPCAForLearning.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }

    @Override
    public short getMoreSimilar(float[] q, float[] o1, float[] o2) {
        simRelCounter++;
        float diffQO1 = 0;
        float diffQO2 = 0;

        DistanceFunctionInterface<float[]> df = new L2OnFloatsArray();
        float d1 = df.getDistance(q, o1);
        float d2 = df.getDistance(q, o2);
        short realOrder = Tools.booleanToShort(d1 < d2, 1, 2);

        for (int i = 0; i < Math.min(q.length, diffsWhenWrongPerCoords.length); i++) {
            diffQO1 += (q[i] - o1[i]) * (q[i] - o1[i]);
            diffQO2 += (q[i] - o2[i]) * (q[i] - o2[i]);
            short currOrder = Tools.booleanToShort(diffQO1 < diffQO2, 1, 2);
            if (currOrder != realOrder) {
                errorsPerCoord[i]++;
                synchronized (this) {
                    diffsWhenWrongPerCoords[i].add(Math.abs(diffQO1 - diffQO2));
                }
            }
        }
        if (diffQO1 == diffQO2) {
            return 0;
        }
        return Tools.booleanToShort(diffQO1 < diffQO2, 1, 2);
    }

    public void resetCounters(int pcaLength) {
        simRelCounter = 0;
        errorsPerCoord = new int[pcaLength];
    }

    public final void resetLearning(int pcaLength) {
        diffsWhenWrongPerCoords = new List[pcaLength];
        for (int i = 0; i < pcaLength; i++) {
            diffsWhenWrongPerCoords[i] = new ArrayList();
        }
        resetCounters(pcaLength);
    }

}
