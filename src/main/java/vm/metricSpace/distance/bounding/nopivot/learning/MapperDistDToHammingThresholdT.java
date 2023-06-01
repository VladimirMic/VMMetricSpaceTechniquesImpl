package vm.metricSpace.distance.bounding.nopivot.learning;

import java.awt.geom.Point2D;
import java.util.List;
import vm.math.Tools;

/**
 *
 * @author xmic
 */
public class MapperDistDToHammingThresholdT {

    private final double maxDistance;
    private final Double sketchesIDim;
    private final List<Point2D.Float> pxFunctionPoints;
    private final int skLength;
    private final double thresholdPcum;

    public static final Float ERROR_THRESHOLD_P_CUM = 0.0000001f;

    public MapperDistDToHammingThresholdT(double maxDistance, double sketchesIDim, int skLength, List<Point2D.Float> pxFunctionPoints, double thresholdPcum) {
        this.maxDistance = maxDistance;
        this.sketchesIDim = sketchesIDim;
        this.pxFunctionPoints = pxFunctionPoints;
        this.skLength = skLength;
        this.thresholdPcum = thresholdPcum;
    }

    public float findXMappedToT(int t) {
        if (t == 0) {
            return 0;
        }
        double curX = 0f;
        double curErr = Float.MAX_VALUE;
        double step = maxDistance / 2;
        while (curErr > ERROR_THRESHOLD_P_CUM) {
            double curProb = estimateProbabilityXUpToB(curX, t);
            curErr = curProb - thresholdPcum;
            if (curErr > 0) {
                curX += step;
            } else {
                curX -= step;
            }
            step = step / 2;
            if (step == 0) {
                break;
            }
            curErr = Math.abs(curErr);
        }
//        return Math.max(0f, (float) curX);
        return (float) curX;
    }

    private double estimateProbabilityXUpToB(double x, int t) {
        double[] px0b = new double[skLength + 1];
        double[] px0bCumulative = new double[skLength + 1];

        float pxProbSkBitsDifX = Tools.interpolatePoints(pxFunctionPoints, (float) x);
        PxbSmallBinAnalogue pxb = new PxbSmallBinAnalogue(skLength, sketchesIDim, pxProbSkBitsDifX);

        double sum = 0;
        for (int i = 0; i <= skLength; i++) {
            px0b[i] = pxb.value(i);
            sum += px0b[i];
            px0bCumulative[i] = sum;
        }
        return px0bCumulative[t];
    }

}
