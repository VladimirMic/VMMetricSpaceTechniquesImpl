package vm.searchSpace.distance.impl;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.searchSpace.distance.DistanceFunctionInterface;

/**
 * Based on the file that is a part of Subsequence Mathing Framework (SMF)
 * library of David Novak, Masaryk University, Brno, Czech Republic
 *
 * Petr Volny, Masaryk University, Brno, Czech Republic, volny.petr@gmail.com
 * Jakub Valcik, Masaryk University, Brno, Czech Republic, xvalcik@fi.muni.cz
 *
 * E.g., motion represenation: List<float[][]> where the list is the motion
 * (time series of multiple joints coordinates),
 * float[timestamp-index][coordinates in 3D Euclid space]
 *
 * @author Vladimir Mic
 */
public class DTWOnFloatsArray extends DistanceFunctionInterface<List<float[][]>> {

    private static final Logger LOG = Logger.getLogger(DTWOnFloatsArray.class.getName());
    public static long counter = 0;

    /**
     * Computes DTW distance between two sequences
     *
     * @param s1 first Sequence object
     * @param s2 second Sequence object
     * @return DTW distance between the first and the second sequence object. If
     * at least one of the given objects is not of a type Sequence than -1 is
     * returned
     */
    @Override
    public float getDistance(List<float[][]> s1, List<float[][]> s2) {
        int n = s1.size(), m = s2.size();
        float cm[][] = new float[n][m]; // accum matrix
        // create the accum matrix
        cm[0][0] = getPiecewiseDist(0, s1, s2, 0);
        for (int i = 1; i < n; i++) {
            cm[i][0] = getPiecewiseDist(i, s1, s2, 0) + cm[i - 1][0];
        }
        for (int j = 1; j < m; j++) {
            cm[0][j] = getPiecewiseDist(0, s1, s2, j) + cm[0][j - 1];
        }
        // Compute the matrix values
        for (int i = 1; i < n; i++) {
            for (int j = 1; j < m; j++) {
                // Decide on the path with minimum distance so far
                cm[i][j] = getPiecewiseDist(i, s1, s2, j) + Math.min(cm[i - 1][j], Math.min(cm[i][j - 1], cm[i - 1][j - 1]));
            }
        }
        counter++;
        if (counter % 100000 == 0) {
            LOG.log(Level.INFO, "Computed {0} DTWs", counter);
        }
        return cm[n - 1][m - 1];
    }

    private float getPiecewiseDist(int idx1, List<float[][]> s1, List<float[][]> s2, int idx2) {
        float[][] s1Pose = s1.get(idx1);
        float[][] s2Pose = s2.get(idx2);
        float ret = 0;
        // dist of poses as sum of the Euclidean distances of joints
        for (int i = 0; i < s1Pose.length; i++) {
            float powSum = 0;
            float[] coords1 = s1Pose[i];
            float[] coords2 = s2Pose[i];
            for (int j = 0; j < coords1.length; j++) {
                float dif = (coords1[j] - coords2[j]);
                powSum += dif * dif;
            }
            ret += (float) Math.sqrt(powSum);
        }
        return ret;
    }

}
