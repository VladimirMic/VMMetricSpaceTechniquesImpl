package vm.search.algorithm.impl;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.Dataset;
import vm.searchSpace.distance.DistanceFunctionInterface;
import vm.searchSpace.distance.bounding.nopivot.impl.SecondaryFilteringWithSketches;
import vm.objTransforms.objectToSketchTransformators.AbstractObjectToSketchTransformator;
import vm.search.algorithm.SearchingAlgorithm;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class KNNSearchWithSketchSecondaryFiltering<T> extends SearchingAlgorithm<T> {

    private static final Logger LOG = Logger.getLogger(KNNSearchWithSketchSecondaryFiltering.class.getName());

    private final Dataset fullDataset;
    private final SecondaryFilteringWithSketches filter;
    private final AbstractObjectToSketchTransformator sketchingTechnique;

    public KNNSearchWithSketchSecondaryFiltering(Dataset fullDataset, SecondaryFilteringWithSketches filter, AbstractObjectToSketchTransformator sketchingTechnique) {
        this.filter = filter;
        this.fullDataset = fullDataset;
        this.sketchingTechnique = sketchingTechnique;
    }

    @Override
    public TreeSet<Map.Entry<Comparable, Float>> completeKnnSearch(AbstractSearchSpace<T> hammingSpace, Object fullQuery, int k, Iterator<Object> objects, Object... params) {
        long t = -System.currentTimeMillis();
        TreeSet<Map.Entry<Comparable, Float>> currAnswer = null;
        if (params.length > 0 && params[0] instanceof TreeSet) {
            currAnswer = (TreeSet<Map.Entry<Comparable, Float>>) params[0];
        }
        DistanceFunctionInterface fullDF = fullDataset.getDistanceFunction();
        AbstractSearchSpace fullDatasetSearchSpace = fullDataset.getSearchSpace();
        Object qData = fullDatasetSearchSpace.getDataOfObject(fullQuery);
        Object qSketch = sketchingTechnique.transformSearchObject(fullQuery);
        long[] qSketchData = (long[]) hammingSpace.getDataOfObject(qSketch);
        Comparable qId = hammingSpace.getIDOfObject(qSketch);
        TreeSet<Map.Entry<Comparable, Float>> ret = currAnswer == null ? new TreeSet<>(new Tools.MapByFloatValueComparator()) : currAnswer;
        AtomicInteger distComps = new AtomicInteger();
        boolean justIDsProvided = params.length > 0 && params[0] instanceof Map;
        Map fullObjectsStorage = null;
        if (justIDsProvided) {
            fullObjectsStorage = (Map) params[0];
        }
        float range = adjustAndReturnSearchRadiusAfterAddingOne(ret, k, Float.MAX_VALUE);
        while (objects.hasNext()) {
            Object fullO = objects.next();
            Comparable oId;
            if (justIDsProvided) {
                oId = (Comparable) fullO;
            } else {
                oId = hammingSpace.getIDOfObject(fullO);
            }
            if (range < Float.MAX_VALUE) {
                float lowerBound = filter.lowerBound(qSketchData, oId, range);
                if (lowerBound == Float.MAX_VALUE) {
                    continue;
                }
            }
            distComps.incrementAndGet();
            Object oData;
            if (justIDsProvided) {
                oData = fullObjectsStorage.get(oId);
            } else {
                oData = fullDatasetSearchSpace.getDataOfObject(fullO);
            }
            float distance = fullDF.getDistance(qData, oData);
            if (distance < range) {
                ret.add(new AbstractMap.SimpleEntry<>(oId, distance));
                range = adjustAndReturnSearchRadiusAfterAddingOne(ret, k, Float.MAX_VALUE);
            }
        }
        t += System.currentTimeMillis();
        incTime(qId, t);

        incDistsComps(qId, distComps.get());
        LOG.log(Level.INFO, "Evaluated query {2} using {0} dist comps. Time: {1}", new Object[]{distComps.get(), t, qId.toString()});
        return ret;
    }

    @Override
    public List candSetKnnSearch(AbstractSearchSpace hammingSearchSpace, Object skQ, int k, Iterator objects, Object... additionalParams) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public String getResultName() {
        return filter.getTechFullName();
    }

}
