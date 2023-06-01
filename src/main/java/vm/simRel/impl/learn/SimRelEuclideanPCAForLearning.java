package vm.simRel.impl.learn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import vm.datatools.Tools;
import vm.metricSpace.distance.impl.L2OnFloatsArray;
import vm.simRel.SimRelInterface;
import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author Vlada
 */
public class SimRelEuclideanPCAForLearning implements SimRelInterface<float[]> {

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
        float[][] ret = new float[percentiles.length][numberOfCoordinates];
        for (int i = 0; i < numberOfCoordinates; i++) {
            if (!diffsWhenWrongPerCoords[i].isEmpty()) {
                Collections.sort(diffsWhenWrongPerCoords[i]);
                for (int j = 0; j < percentiles.length; j++) {
                    float percentile = percentiles[j];
                    int idx;
                    if (percentile == 1f) {
                        idx = diffsWhenWrongPerCoords[i].size() - 1;
                    } else {
                        idx = (int) (Math.floor(diffsWhenWrongPerCoords[i].size() * percentile) - 1);
                    }
                    idx = Math.min(idx, diffsWhenWrongPerCoords[i].size() - 1);
                    idx = Math.max(0, idx);
                    ret[j][i] = diffsWhenWrongPerCoords[i].get(idx);
                    System.out.println("Percentile: " + percentile + ",cord: " + i + ", idx: " + idx + ", threshold: " + ret[j][i] + ", number of errors: " + diffsWhenWrongPerCoords[i].size());
                }
            }
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

        for (int i = 0; i < diffsWhenWrongPerCoords.length; i++) {
            diffQO1 += (q[i] - o1[i]) * (q[i] - o1[i]);
            diffQO2 += (q[i] - o2[i]) * (q[i] - o2[i]);
            short currOrder = Tools.booleanToShort(diffQO1 < diffQO2, 1, 2);
            if (currOrder != realOrder) {
                errorsPerCoord[i]++;
                diffsWhenWrongPerCoords[i].add(Math.abs(diffQO1 - diffQO2));
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
