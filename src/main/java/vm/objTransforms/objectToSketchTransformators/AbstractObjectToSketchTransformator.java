package vm.objTransforms.objectToSketchTransformators;

import java.io.Serializable;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.distance.AbstractDistanceFunction;
import vm.objTransforms.storeLearned.PivotPairsStoreInterface;
import vm.objTransforms.SearchObjectTransformerInterface;

/**
 *
 * @author xmic
 */
public abstract class AbstractObjectToSketchTransformator implements SearchObjectTransformerInterface, Serializable {

    public static final long serialVersionUID = 836012244806410000l;

    protected ExecutorService threadPool = null;

    protected final AbstractSearchSpace<Object> searchSpace;
    protected Object[] pivots;
    protected final AbstractDistanceFunction distanceFunc;
    protected final Object[] additionalInfo;

    public AbstractObjectToSketchTransformator(AbstractDistanceFunction<Object> distanceFunc, AbstractSearchSpace<Object> searchSpace, List<Object> pivots, Object... additionalInfo) {
        this(distanceFunc, searchSpace, pivots.toArray());
    }

    public AbstractObjectToSketchTransformator(AbstractDistanceFunction<Object> distanceFunc, AbstractSearchSpace<Object> searchSpace, Object[] pivots, Object... additionalInfo) {
        this.searchSpace = searchSpace;
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
    public abstract Object transformSearchObject(Object obj, Object... params);

    @Override
    public abstract String getTechniqueAbbreviation();

    public abstract List<BitSet> createColumnwiseSketches(AbstractSearchSpace<Object> searchSpace, List<Object> sampleObjects, AbstractDistanceFunction<Object> df);

    protected abstract int getSketchLength();

    public abstract void redefineSketchingToSwitchBit(int i);

    public abstract void preserveJustGivenBits(int[] bitsToPreserve);

    public abstract void setPivotPairsFromStorage(PivotPairsStoreInterface storage, String sketchingTechniqueName);

}
