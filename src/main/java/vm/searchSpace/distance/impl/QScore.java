package vm.searchSpace.distance.impl;

import java.util.logging.Level;
import java.util.logging.Logger;
import vm.searchSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author xmic
 */
public class QScore extends DistanceFunctionInterface<String> {

    public static final Float IMPLICIT_INNER_PARAMETER_ON_SIZE_CHECK = 0.6f;

    private final float innerParameterOnSizeDiff;
    private final ProteinNativeQScoreDistance nativeDist;

    public QScore(float innerParameterOnSizeDiff) {
        this.innerParameterOnSizeDiff = innerParameterOnSizeDiff;
        nativeDist = new ProteinNativeQScoreDistance();
    }

    public static void setRootFolder(String root) {
        ProteinNativeQScoreDistance.initDistance(root);
    }

    @Override
    public float getDistance(String obj1, String obj2) {
        float[] stats = nativeDist.getStatsFloats(obj1, obj2, innerParameterOnSizeDiff);
        return 1 - stats[0];
    }

    private static class ProteinNativeQScoreDistance {

        private static final Logger LOG = Logger.getLogger(ProteinNativeQScoreDistance.class.getName());

        public static native void init(String archiveDirectory);

        private static native float[] getStats(String id1, String id2, float inherentApprox);

        /**
         * Must be called before first evaluation. Objects from specified file
         * are read into the main memorys for more efficient distance
         * evaluations
         *
         * @param gesamtLibraryPath according to gesamt
         */
        public static void initDistance(String archiveDirectory) {
            try {
                System.loadLibrary("ProteinDistance");
                // parameter 0.6 is inherent parametr in C library that was examined to speed-up distance evaluation
                // while well approximating the geometric similarity of protein structures
                init(archiveDirectory);
            } catch (java.lang.UnsatisfiedLinkError | Exception ex) {
                LOG.log(Level.WARNING, "Initialization of the distance function not successfull.", ex);
            }

        }

        /**
         *
         * @param o1
         * @param o2
         * @param innerParameterOnSizeDiff 0 for no check, 0.6 according tu us,
         * 0.7
         * @return
         */
        public float[] getStatsFloats(String o1, String o2, float innerParameterOnSizeDiff) {
            if (o1 == null || o2 == null) {
                LOG.log(Level.SEVERE, "Attempt to evaluate distance between null proteins {0}, {1}.", new Object[]{o1, o2});
            }
            try {
                float[] ret = getStats(o1, o2, innerParameterOnSizeDiff);
                if (o1.equals(o2)) {
                    ret[0] = 1;
                }
                return ret;
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Unsuccessful attempt to evaluate distance between protein structures with IDs {0}, {1}. Original exception: {2}", new Object[]{o1, o2, e.getMessage()});
                throw new IllegalArgumentException("Unsuccessful attempt to evaluate distance between protein structures with IDs " + o1 + ", " + o2);
            }
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }

        public float getMaxDistance() {
            return 1f;
        }

    }

}
