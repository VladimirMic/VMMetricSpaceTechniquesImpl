package vm.objTransforms.learning;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.javatools.Tools;
import vm.objTransforms.MetricObjectsParallelTransformerImpl;

/**
 *
 * @author xmic
 */
public class AuxiliaryLearnSketching {

    public static final Logger LOG = Logger.getLogger(AuxiliaryLearnSketching.class.getName());

    public float[][] getSketchesCorrelations(List<BitSet> columnWiseSketches, int sampleSetSize, boolean absoluteValues) {
        List<double[]> sketchesCardSizeAvg = new ArrayList<>();
        float[][] ret = new float[columnWiseSketches.size()][columnWiseSketches.size()];

        for (int i = 0; i < columnWiseSketches.size(); i++) {
            double[] cardSizeCardCard2 = getCardSizeCardCard2(columnWiseSketches.get(i), sampleSetSize);
            sketchesCardSizeAvg.add(cardSizeCardCard2);
            ret[i][i] = 1f;
        }
        int blockSize = columnWiseSketches.size();

        long timeStart = System.currentTimeMillis();
        int start = 0;
        int end = blockSize - 1;
        ExecutorService threadPool = end - start > 5000 ? Tools.initExecutor(Tools.PARALELISATION) : Tools.initExecutor(1);
        CountDownLatch latch = new CountDownLatch(end - start);
        while (end < columnWiseSketches.size()) {
            try {
                for (int i = start; i < end; i++) {                    
                    CorrelationThreadCounter thread = new CorrelationThreadCounter(columnWiseSketches, i + 1, end + 1, sampleSetSize, absoluteValues, sketchesCardSizeAvg, ret, latch); // jak dlouho toto trvá?
                    threadPool.execute(thread);
                    if (i % 100 == 0) {
                        LOG.log(Level.INFO, "Evaluating correlations of sketches. Primary index: {0} out of {1}", new Object[]{i, end});
                    }
                }
                latch.await();
                start += blockSize;
                end += blockSize;
            } catch (InterruptedException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
        threadPool.shutdown();
        LOG.log(Level.INFO, "{0} correlations counted in {1} ms.", new Object[]{columnWiseSketches.size() * (columnWiseSketches.size() - 1) / 2, System.currentTimeMillis() - timeStart});
        return ret;
    }

    public double[] getCardSizeCardCard2(BitSet bitset, int sketchLength) {
        double[] ret = new double[3];
        ret[0] = bitset.cardinality();
        ret[1] = sketchLength * ret[0];
        ret[2] = ret[0] * ret[0];
        return ret;
    }

    public int[] selectLowCorrelatedBits(float[][] sketchesCorrelations, int retSize) {
        return selectLowCorrelatedBits(sketchesCorrelations, retSize, 0);
    }

    public int[] selectLowCorrelatedBits(float[][] sketchesCorrelations, int retSize, float correlationToAchieve) {
        LOG.log(Level.INFO, "Starting selection of low correlated subset.");
        Random rand = new Random(System.currentTimeMillis());
        updateCorrelations(sketchesCorrelations, -correlationToAchieve);
        List<Integer> curIndexes = new ArrayList<>();
        List<Integer> blackList = new ArrayList<>();
        addIndexes(blackList, curIndexes, retSize, rand, sketchesCorrelations, true);
        List<Integer> bestIndexes = null;
        float lowestFoundCorrelation = 1;
        int iterationsCounter = 0;
        int starts = 0;
        while (starts < 5) {//5
            if (iterationsCounter % 4000 == 0) { // 4000
                starts++;
                curIndexes.clear();
                blackList.clear();
                addIndexes(blackList, curIndexes, retSize, rand, sketchesCorrelations, false);
                LOG.log(Level.INFO, "No better solution found for {0} iterations. Lowest max correlation: {1}. Start number {2}", new Object[]{iterationsCounter, lowestFoundCorrelation, starts});
            }
            iterationsCounter++;
            Integer[] curIndexesArray = curIndexes.toArray(new Integer[0]);
            float worstCorrelation = getHighestCorrelation(curIndexesArray, sketchesCorrelations);
            if (worstCorrelation < lowestFoundCorrelation) {
                bestIndexes = new ArrayList<>(curIndexes);
                lowestFoundCorrelation = worstCorrelation;
                LOG.log(Level.INFO, "Found subset with worst correlation {0}. Started in {1}, iteration {2}", new Object[]{lowestFoundCorrelation, starts, iterationsCounter});
            }
            float diff = 0.02f;
            List<Integer> correlatedIndexes = getCorrelatedIndexes(worstCorrelation, curIndexesArray, sketchesCorrelations, diff);
//            blackList.addAll(correlatedIndexes);
            curIndexes.removeAll(correlatedIndexes);
            boolean bySum = rand.nextInt(100) < 60;
            addIndexes(blackList, curIndexes, retSize, rand, sketchesCorrelations, bySum);
            while (blackList.size() > retSize * 0.8) {
                blackList.remove(0);
            }
        }
        int[] ret = new int[retSize];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = bestIndexes.get(i);
        }
        updateCorrelations(sketchesCorrelations, correlationToAchieve);
        return ret;
    }

    private void updateCorrelations(float[][] sketchesCorrelations, float correlationToAdd) {
        for (float[] sketchesCorrelationLine : sketchesCorrelations) {
            for (int j = 0; j < sketchesCorrelationLine.length; j++) {
                float t = sketchesCorrelationLine[j];
                sketchesCorrelationLine[j] = t + correlationToAdd;
            }
        }
    }

    private void addIndexes(Collection<Integer> blackList, Collection<Integer> ret, int retSize, Random rand, float[][] sketchesCorrelations, boolean bySum) {
        int start = rand.nextInt(sketchesCorrelations.length);
        while (ret.size() < retSize) {
            float[] maxCorrelationOfCandWithSet = new float[sketchesCorrelations.length];
            for (int i = 0; i < sketchesCorrelations.length; i++) {
                int idx = (start + i) % sketchesCorrelations.length;
                maxCorrelationOfCandWithSet[idx] = -1;
                if (ret.contains(idx)) {
                    maxCorrelationOfCandWithSet[idx] = 1;
                } else {

                    if (bySum) {
                        float sum = 0;
                        for (int intSet : ret) {
                            sum += sketchesCorrelations[idx][intSet];
                        }
                        maxCorrelationOfCandWithSet[idx] = sum;
                    } else {
                        for (int intSet : ret) {
                            if (sketchesCorrelations[idx][intSet] > maxCorrelationOfCandWithSet[idx]) {
                                maxCorrelationOfCandWithSet[idx] = sketchesCorrelations[idx][intSet];
                            }
                        }
                    }
                }
            }
            float minMaxCorSum = Float.MAX_VALUE;
            int addIdx = 0;
            for (int i = 0; i < maxCorrelationOfCandWithSet.length; i++) {
                boolean isAllowed = !blackList.contains(i) && !ret.contains(i);
                if (isAllowed && maxCorrelationOfCandWithSet[i] < minMaxCorSum) {
                    addIdx = i;
                    minMaxCorSum = maxCorrelationOfCandWithSet[i];
                }
            }
            ret.add(addIdx);
        }
    }

    private List<Integer> getCorrelatedIndexes(float worstCorrelation, Integer[] curIndexes, float[][] sketchesCorrelations, float coef) {
        List<Integer> removed = new ArrayList<>();
        float limit = worstCorrelation - coef;
        for (int i = 0; i < curIndexes.length - 1; i++) {
            int idxI = curIndexes[i];
            for (int j = i + 1; j < curIndexes.length; j++) {
                int idxJ = curIndexes[j];
                if (sketchesCorrelations[idxI][idxJ] > limit && !removed.contains(idxI) && !removed.contains(idxJ)) {
                    float sumI = 0;
                    float sumJ = 0;
                    for (int k = 0; k < curIndexes.length; k++) {
                        sumI += sketchesCorrelations[idxI][k];
                        sumJ += sketchesCorrelations[idxJ][k];
                    }
                    int rem = sumI > sumJ ? idxI : idxJ;
                    removed.add(rem);
                }
            }
        }
        return removed;
    }

    private float getHighestCorrelation(Integer[] curIndexes, float[][] sketchesCorrelations) {
        float ret = 0;
        for (int i = 0; i < curIndexes.length - 1; i++) {
            int idxI = curIndexes[i];
            for (int j = i + 1; j < curIndexes.length; j++) {
                int idxJ = curIndexes[j];
                if (sketchesCorrelations[idxI][idxJ] > ret) {
                    ret = sketchesCorrelations[idxI][idxJ];
                }
            }
        }
        return ret;
    }

}
