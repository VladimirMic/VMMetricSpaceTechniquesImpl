package vm.objTransforms.objectToSketchTransformators;

import java.io.Serializable;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.objTransforms.MetricObjectTransformerInterface;
import vm.objTransforms.storeLearned.PivotPairsStoreInterface;

/**
 *
 * @author xmic
 */
public abstract class AbstractObjectToSketchTransformator implements MetricObjectTransformerInterface, Serializable {

    public static final long serialVersionUID = 836012244806410000l;

    protected ExecutorService threadPool = null;

    protected final AbstractMetricSpace<Object> metricSpace;
    protected Object[] pivots;
    protected final DistanceFunctionInterface distanceFunc;
    protected final Object[] additionalInfo;

    public AbstractObjectToSketchTransformator(DistanceFunctionInterface<Object> distanceFunc, AbstractMetricSpace<Object> metricSpace, List<Object> pivots, Object... additionalInfo) {
        this(distanceFunc, metricSpace, pivots.toArray());
    }

    public AbstractObjectToSketchTransformator(DistanceFunctionInterface<Object> distanceFunc, AbstractMetricSpace<Object> metricSpace, Object[] pivots, Object... additionalInfo) {
        this.metricSpace = metricSpace;
        this.pivots = pivots;
        this.distanceFunc = distanceFunc;
        this.additionalInfo = additionalInfo;
    }

    /**
     *
     * @param fullDatasetName
     * @param params params[0] is the length of sketches, params[1] is the float
     * between 0 and 1 denoting the balance of bits
     * @return
     */
    @Override
    public final String getNameOfTransformedSetOfObjects(String fullDatasetName, Object... params) {
        int sketchLength = (int) params[0];
        float balance = (float) params[1];
        int balanceInt = (int) (balance * 100);
        return fullDatasetName + "_" + getTechniqueAbbreviation() + "_" + balanceInt + "_" + sketchLength;
    }

    public Object[] getPivots() {
        return pivots;
    }

    @Override
    public abstract Object transformMetricObject(Object obj, Object... params);

    @Override
    public abstract String getTechniqueAbbreviation();

    public abstract List<BitSet> createColumnwiseSketches(AbstractMetricSpace<Object> metricSpace, List<Object> sampleObjects, DistanceFunctionInterface<Object> df);

    protected abstract int getSketchLength();

    public abstract void redefineSketchingToSwitchBit(int i);

    public abstract void preserveJustGivenBits(int[] bitsToPreserve);

    public abstract void setPivotPairsFromStorage(PivotPairsStoreInterface storage, String sketchingTechniqueName);

}
