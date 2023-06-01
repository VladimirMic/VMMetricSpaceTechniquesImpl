package vm.metricSpace.distance.bounding.nopivot.learning;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.distribution.BinomialDistribution;

/**
 *
 * @author xmic
 */
public class PxbSmallBinAnalogue implements UnivariateFunction {

    private final BinomialDistribution binAnalogue;
    private final Double bitIndexCoef;
    private double probCoef;
    private final Integer knownResult;

    public PxbSmallBinAnalogue(int bSkLength, Double degreesOfFreedom, double pxProbSkBitsDifX) {
        if (degreesOfFreedom == null) {
            degreesOfFreedom = bSkLength / 2d;
        }
        if (pxProbSkBitsDifX == 0 || pxProbSkBitsDifX == 1) {
            binAnalogue = null;
            bitIndexCoef = 1d;
            probCoef = 1d;
            if (pxProbSkBitsDifX == 0) {
                knownResult = 0;
            } else {
                knownResult = bSkLength;
            }
        } else {
            knownResult = null;
//            double aBinomLength = evalAnalogueBinomLength(bSkLength, degreesOfFreedom, pxProbSkBitsDifX);
            double aBinomLength = degreesOfFreedom;
            int aBinomLengthInt = (int) Math.round(aBinomLength);
            bitIndexCoef = aBinomLengthInt / (double) bSkLength;
            binAnalogue = new BinomialDistribution(aBinomLengthInt, pxProbSkBitsDifX);
            probCoef = evalProbCoef(aBinomLengthInt, bSkLength);
        }
    }

    public PxbSmallBinAnalogue(int bSkLength, Double iDim, UnivariateFunction pxProbSkBitsDif, double x) {
        this(bSkLength, iDim, pxProbSkBitsDif.value(x));
    }

    @Override
    public double value(double bBitIndexDouble) {
        bBitIndexDouble = bBitIndexDouble * bitIndexCoef;
        if (knownResult != null) {
            if (bBitIndexDouble == knownResult) {
                return 1;
            }
            return 0;
        }
        int numberOfTrials = binAnalogue.getNumberOfTrials();
        if (bBitIndexDouble > numberOfTrials) {
            return 0;
        }
        int idx = (int) Math.round(bBitIndexDouble);
        if (numberOfTrials <= 2) {
            double ret = binAnalogue.probability(idx);
            if (ret >= 0.5) {
                return 1;
            }
            return 0;
        }
        int low = (int) bBitIndexDouble;
        int up = low + 1;
        double ret;
        if (up <= numberOfTrials) {
            double r1 = binAnalogue.probability(low);
            double r2 = binAnalogue.probability(up);
            double dy = r2 - r1;
            double dx = bBitIndexDouble - low;
            ret = r1 + dx * dy;
        } else {
            ret = binAnalogue.probability(idx);
        }
        return ret / probCoef;
    }

//    private double evalAnalogueBinomLength(int bSkLength, Double iDim, double pxProbSkBitsDifX) {
////      Varianta 1 s prepoctem pomoci pravdepodobnosti - spatna
////        double lengthOfIdealSketches = bSkLength * bSkLength / (pxProbSkBitsDifX * (1 - pxProbSkBitsDifX) * 8 * iDim);
////        double ret = bSkLength / lengthOfIdealSketches * bSkLength;
////        return ret;
//
////      Varianta 2 globalni prepocet
//        return 2 * iDim;
//    }
    private double evalProbCoef(int aBinomLength, int bSkLength) {
        if (aBinomLength <= 2) {
            return 1;
        }
        probCoef = 1;
        double sum = 0;
        for (int i = 0; i <= bSkLength; i++) {
            sum += value(i);
        }
        return sum;
//        BaseAbstractUnivariateIntegrator integrator = new OnePeakPositiveFunctionIntegrator(0, bSkLength);
//        double sum2 = integrator.integrate(Integer.MAX_VALUE, this, 0, bSkLength);
//        return sum2;
    }

    private double estimateProbPeakOfCorrSketches(double aBinomMean, int bSkLength) {
        int peakIndexReal = (int) Math.round(0.5d * bSkLength);
        double binomPeakOfSkLength = new BinomialDistribution(bSkLength, 0.5d).probability(peakIndexReal);
        int iDimOfIdealSketchesWithOrigLength = bSkLength / 2;
        double iDimRatio = (aBinomMean - 1) / (iDimOfIdealSketchesWithOrigLength - 1);
        double ret = Math.sqrt(iDimRatio) * binomPeakOfSkLength;
        return ret;
    }

}
