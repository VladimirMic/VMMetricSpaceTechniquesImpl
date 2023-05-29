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

    public float[][] getDiffWhenWrong(float percentile) {
        return getDiffWhenWrong(percentile, -1);
    }

    public float[][] getDiffWhenWrong(float percentile, int numberOfCoordinates) {
        if (numberOfCoordinates < 0) {
            numberOfCoordinates = diffsWhenWrongPerCoords.length;
        }
        float[] retThresholds = new float[numberOfCoordinates];
        float[] retMax = new float[numberOfCoordinates];
        for (int i = 0; i < retThresholds.length; i++) {
            if (!diffsWhenWrongPerCoords[i].isEmpty()) {
                Collections.sort(diffsWhenWrongPerCoords[i]);
                int idx = Math.round(diffsWhenWrongPerCoords[i].size() * percentile) - 1;
                retThresholds[i] = diffsWhenWrongPerCoords[i].get(idx);
                retMax[i] = diffsWhenWrongPerCoords[i].get(diffsWhenWrongPerCoords[i].size() - 1);
            }
            System.out.println("idx: " + i + ", threshold: " + retThresholds[i] + ", max error: " + retMax[i] + ", number of errors: " + diffsWhenWrongPerCoords[i].size());
        }
        return new float[][]{retThresholds, retMax};
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
    }

}
