package vm.distEstimation.limitedAngles.foursome;

import vm.math.Tools;
import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author Vlada
 */
public class ToolsPtolemaionsLikeCoefs {

    public static final float[] get8Angles(float[] sixDists, boolean inDegress) {
        float[] ret = new float[8]; // beta1, delta2, gamma2, alphao, deltao, betaq, alphaq, gamma1
        float[] angles = Tools.evaluateAnglesOfTriangle(sixDists[0], sixDists[1], sixDists[4], inDegress); //a, b, e
        ret[0] = angles[1]; // beta1
        ret[3] = angles[0]; // delta2
        angles = Tools.evaluateAnglesOfTriangle(sixDists[1], sixDists[2], sixDists[5], inDegress); //b, c, f
        ret[2] = angles[1]; // gamma2
        ret[5] = angles[0]; // betaq
        angles = Tools.evaluateAnglesOfTriangle(sixDists[2], sixDists[3], sixDists[4], inDegress); //c, d, e
        ret[4] = angles[1]; // deltao
        ret[7] = angles[0]; // gamma1
        angles = Tools.evaluateAnglesOfTriangle(sixDists[3], sixDists[0], sixDists[5], inDegress); //d, a, f
        ret[6] = angles[1]; // alphaq
        ret[1] = angles[0]; // delta2
        return ret;
    }

    public static final float[] getPairwiseDistsOfFourObjects(DistanceFunctionInterface df, boolean enforceEFgeqBD, Object... fourObjects) {
        if (fourObjects.length < 4) {
            throw new IllegalArgumentException("At least four objects must be provided");
        }
        float[] ret = new float[6];
        ret[4] = df.getDistance(fourObjects[0], fourObjects[2]);
        ret[5] = df.getDistance(fourObjects[1], fourObjects[3]);
        for (int i = 0; i < 6; i++) {
            if (i < 4) {
                ret[i] = df.getDistance(fourObjects[i], fourObjects[(i + 1) % 4]);
            }
            if (ret[i] == 0) {
                return null;
            }
        }
        if (enforceEFgeqBD && ret[4] * ret[5] < ret[1] * ret[3]) {
            float b = ret[4];
            float d = ret[5];
            float e = ret[1];
            float f = ret[3];
            ret[1] = b;
            ret[3] = d;
            ret[4] = e;
            ret[5] = f;
        }
        return ret;
    }

    public static final double[] evaluateEq(float[] anglesRad, int order) {
        switch (order) {
            case 1: {
                return evaluateFirstEq(anglesRad);
            }
            case 2: {
                return evaluateSecondEq(anglesRad);
            }
            case 3: {
                return evaluateThirdEq(anglesRad);
            }
            case 4: {
                return evaluateFourthEq(anglesRad);
            }
        }
        return null;
    }

    // 0: beta1, 1: delta2, 2: gamma2, 3: alphao, 4: deltao, 5: betaq, 6: alphaq, 7: gamma1
    private static double[] evaluateFirstEq(float[] anglesRad) {
        float beta1 = anglesRad[0];
        float alphao = anglesRad[3];
        float deltao = anglesRad[4];
        float gamma1 = anglesRad[7];
        double A = (1 - Math.cos(alphao)) * (1 - Math.cos(gamma1)) / ((Math.cos(beta1) - Math.cos(alphao + beta1)) * (Math.cos(deltao) - Math.cos(gamma1 + deltao)));
        double B = Math.sin(alphao + beta1) / Math.sin(beta1) * Math.sin(gamma1 + deltao) / Math.sin(deltao) + 1;
        float delta2 = anglesRad[1];
        float gamma2 = anglesRad[2];
        float betaq = anglesRad[5];
        float alphaq = anglesRad[6];
        double C = Math.sin(betaq) / Math.sin(betaq + gamma2) + Math.sin(delta2) / Math.sin(delta2 + alphaq);
        return new double[]{A * B, A * C};
    }

    // 0: beta1, 1: delta2, 2: gamma2, 3: alphao, 4: deltao, 5: betaq, 6: alphaq, 7: gamma1
    private static double[] evaluateSecondEq(float[] anglesRad) {
        float beta1 = anglesRad[0];
        float alphao = anglesRad[3];
        float deltao = anglesRad[4];
        float gamma1 = anglesRad[7];
        double A = (1 - Math.cos(alphao)) * (1 + Math.cos(gamma1)) / ((Math.cos(beta1) - Math.cos(alphao + beta1)) * (Math.cos(deltao) + Math.cos(gamma1 + deltao)));
        double B = Math.sin(alphao + beta1) / Math.sin(beta1) * Math.sin(gamma1 + deltao) / Math.sin(deltao) - 1;
        float delta2 = anglesRad[1];
        float gamma2 = anglesRad[2];
        float betaq = anglesRad[5];
        float alphaq = anglesRad[6];
        double C = Math.sin(betaq) / Math.sin(betaq + gamma2) - Math.sin(delta2) / Math.sin(delta2 + alphaq);
        return new double[]{A * B, A * C};
    }

    private static double[] evaluateThirdEq(float[] anglesRad) {
        float beta1 = anglesRad[0];
        float alphao = anglesRad[3];
        float deltao = anglesRad[4];
        float gamma1 = anglesRad[7];
        double A = (1 + Math.cos(alphao)) * (1 - Math.cos(gamma1)) / ((Math.cos(beta1) + Math.cos(alphao + beta1)) * (Math.cos(deltao) - Math.cos(gamma1 + deltao)));
        double B = Math.sin(alphao + beta1) / Math.sin(beta1) * Math.sin(gamma1 + deltao) / Math.sin(deltao) - 1;
        float delta2 = anglesRad[1];
        float gamma2 = anglesRad[2];
        float betaq = anglesRad[5];
        float alphaq = anglesRad[6];
        double C = -Math.sin(betaq) / Math.sin(betaq + gamma2) + Math.sin(delta2) / Math.sin(delta2 + alphaq);
        return new double[]{A * B, A * C};
    }

    private static double[] evaluateFourthEq(float[] anglesRad) {
        float beta1 = anglesRad[0];
        float alphao = anglesRad[3];
        float deltao = anglesRad[4];
        float gamma1 = anglesRad[7];
        double A = (1 + Math.cos(alphao)) * (1 + Math.cos(gamma1)) / ((Math.cos(beta1) + Math.cos(alphao + beta1)) * (Math.cos(deltao) + Math.cos(gamma1 + deltao)));
        double B = Math.sin(alphao + beta1) / Math.sin(beta1) * Math.sin(gamma1 + deltao) / Math.sin(deltao) + 1;
        float delta2 = anglesRad[1];
        float gamma2 = anglesRad[2];
        float betaq = anglesRad[5];
        float alphaq = anglesRad[6];
        double C = -Math.sin(betaq) / Math.sin(betaq + gamma2) - Math.sin(delta2) / Math.sin(delta2 + alphaq);
        return new double[]{A * B, A * C};
    }
}
