package vm.objTransforms.learning;

import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author xmic
 */
public class CorrelationThreadCounter implements Runnable {

    private final List<BitSet> invertedSketches;
    private final int start;
    private final int end;
    private final int indexI;
    private final int sampleObjectNumber;
    private final boolean absoluteValues;
    private final List<double[]> sketchesCardSizeAvg;
    private final float[][] resultHolder;
    private final CountDownLatch latch;

    private static final Logger LOG = Logger.getLogger(CorrelationThreadCounter.class.getName());

    public CorrelationThreadCounter(List<BitSet> invertedSketches, int start, int end, int sampleObjectNumber, boolean absoluteValues, List<double[]> sketchesCardSizeAvg, float[][] resultHolder, CountDownLatch latch) {
        this.invertedSketches = invertedSketches;
        this.start = start;
        this.end = end;
        this.indexI = start - 1;
        this.sampleObjectNumber = sampleObjectNumber;
        this.absoluteValues = absoluteValues;
        this.sketchesCardSizeAvg = sketchesCardSizeAvg;
        this.resultHolder = resultHolder;
        this.latch = latch;
    }

    @Override
    public void run() {
        for (int j = start; j < end; j++) {
            float cor = getPearsonCorrelation(invertedSketches.get(indexI), invertedSketches.get(j), sketchesCardSizeAvg.get(indexI), sketchesCardSizeAvg.get(j), sampleObjectNumber);
            if (absoluteValues) {
                cor = Math.abs(cor);
            }
            resultHolder[indexI][j] = cor;
            resultHolder[j][indexI] = cor;
        }
        if (indexI % 100 == 0) {
            LOG.log(Level.INFO, "Thread finished evaluation of correlations for index {0}", indexI);
        }
        latch.countDown();
    }

    /**
     * Evaluate the Pearson correlation coefficient of two sketches.
     * Precomputedd values must be provided (see description of parameters).
     *
     * @param sk1
     * @param sk2
     * @param thisCards result of method getCardSizeCardCard2 called on this
     * sketch
     * @param otherCards result of method getCardSizeCardCard2 called on
     * otherSketch
     * @param sketchLength The length of sketch. Set this value manually, since
     * it does not have to correspond to any method called on Sketch.
     * @return Pearson correlation value on tis Sketch and other sketch
     */
    public float getPearsonCorrelation(BitSet sk1, BitSet sk2, double[] thisCards, double[] otherCards, long sketchLength) {
        double aCard = thisCards[0];
        double aCardSize = thisCards[1];
        double aCard2 = thisCards[2];
        double bCard = otherCards[0];
        double bCardSize = otherCards[1];
        double bCard2 = otherCards[2];

        BitSet sk1Clone = (BitSet) sk1.clone();
        sk1Clone.and(sk2);
        int a = sk1Clone.cardinality();

        double numerator = (sketchLength * a) - (aCard * bCard);
        double denominator = Math.sqrt((aCardSize - aCard2) * (bCardSize - bCard2));
        double ret = numerator / denominator;
        if (ret > 1.0 || ret < -1.0) {
            LOG.log(Level.SEVERE,
                    "Wrong correlation! {0};{1};{2}\n{3};{4};{5}\nnumerator: {6}; denominator: {7}\n\n",
                    new Object[]{aCard, aCardSize, aCard2, bCard, bCardSize, bCard2, numerator, denominator});
        }
        return (float) ret;
    }

}
