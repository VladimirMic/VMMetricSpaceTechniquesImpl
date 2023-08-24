/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.metricSpace.datasetPartitioning.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.datasetPartitioning.AbstractDatasetPartitioning;
import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author Vlada
 */
public class GRAPPLEPartitioning extends VoronoiPartitioning {

    public static final Logger LOG = Logger.getLogger(GRAPPLEPartitioning.class.getName());
    private final Map<Object, float[]> coefsForBoundsOnPivotPairs;

    public GRAPPLEPartitioning(Map<Object, float[]> coefsForBoundsOnPivotPairs, AbstractMetricSpace metricSpace, DistanceFunctionInterface df, List<Object> pivots) {
        super(metricSpace, df, pivots);
        this.coefsForBoundsOnPivotPairs = coefsForBoundsOnPivotPairs;
    }

    @Override
    protected AbstractDatasetPartitioning.BatchProcessor getBatchProcesor(List batch, AbstractMetricSpace metricSpace, CountDownLatch latch, Map<Object, Float> pivotLengths, Map<Object, Float> objectsLengths) {
        return new ProcessBatch(batch, metricSpace, latch, pivotLengths, objectsLengths);
    }

    private class ProcessBatch extends AbstractDatasetPartitioning.BatchProcessor {

        public ProcessBatch(List batch, AbstractMetricSpace metricSpace, CountDownLatch latch, Map<Object, Float> pivotLengths, Map<Object, Float> objectsLengths) {
            super(batch, metricSpace, latch, pivotLengths, objectsLengths);
        }

        @Override
        public void run() {
            long t = -System.currentTimeMillis();
            Iterator dataObjects = batch.iterator();
            Map<String, Float> interPivotDists = new HashMap<>();
            for (int i = 0; dataObjects.hasNext(); i++) {
                Object o = dataObjects.next();
                Object oData = metricSpace.getDataOfMetricObject(o);
                Object oID = metricSpace.getIDOfMetricObject(o);

                float minFDOverADefinesPartitioning = Float.MAX_VALUE;
                float maxCosPi1DefinesFiltering = 0;

                Object p1IDForPartitioning = null;
                Object p2IDForPartitioning = null;

                Object p1IDForFiltering = null;
                Object p2IDForFiltering = null;

                float dp1ForLB = 0, dp2ForLB = 0, dp1ForUB = 0, dp2ForUB = 0, dp1p2ForLB = 0, dp1p2ForUB = 0;

                Float oLength = objectsLengths.get(oID);
                for (int j = 0; j < pivotsList.size() - 1; j++) {
                    Object p1 = pivotsList.get(j);
                    Object p1ID = metricSpace.getIDOfMetricObject(p1);
                    Object p1Data = metricSpace.getDataOfMetricObject(p1);
                    float distOP1 = df.getDistance(oData, p1Data, oLength, pivotLengths.get(p1ID));
                    for (int k = j + 1; k < pivotsList.size(); k++) {
                        Object p2 = pivotsList.get(k);
                        Object p2ID = metricSpace.getIDOfMetricObject(p2);
                        Object p2Data = metricSpace.getDataOfMetricObject(p2);
                        float distOP2 = df.getDistance(oData, p2Data, oLength, pivotLengths.get(p2ID));
                        Float distP1P2 = interPivotDists.get(p1ID + "-" + p2ID);
                        if (distP1P2 == null) {
                            distP1P2 = df.getDistance(p1Data, p2Data, pivotLengths.get(p1ID), pivotLengths.get(p2ID));
                            interPivotDists.put(p1ID + "-" + p2ID, distP1P2);
                        }
                        // is this pivot pair best for the partitioning, i.e., UB?
                        float FDOverA = distOP2 * distOP1 / distP1P2;
                        if (FDOverA < minFDOverADefinesPartitioning) { // yes
                            minFDOverADefinesPartitioning = FDOverA;
                            dp1ForUB = Math.min(distOP1, distOP2);
                            dp2ForUB = Math.max(distOP1, distOP2);
                            dp1p2ForUB = distP1P2;
                            if (distOP1 < distOP2) {
                                p1IDForPartitioning = p1ID;
                                p2IDForPartitioning = p2ID;
                            } else {
                                p1IDForPartitioning = p2ID;
                                p2IDForPartitioning = p1ID;
                            }
                        }
                        // is this pivot pair best for the filtering? -- the order of pivots matters!
                        float cosPi1 = (distOP2 * distOP2 - distP1P2 * distP1P2 - distOP1 * distOP1) / (2 * distP1P2 * distOP1);
                        if (cosPi1 > maxCosPi1DefinesFiltering) { // yes
                            dp1p2ForLB = distP1P2;
                            dp1ForLB = distOP1;
                            dp2ForLB = distOP2;
                            maxCosPi1DefinesFiltering = cosPi1;
                            p1IDForFiltering = p1ID;
                            p2IDForFiltering = p2ID;
                        }
                        // is this pivot pair best for the filtering? -- opposite order
                        cosPi1 = (distOP1 * distOP1 - distP1P2 * distP1P2 - distOP2 * distOP2) / (2 * distP1P2 * distOP2);
                        if (cosPi1 > maxCosPi1DefinesFiltering) { // yes
                            dp1p2ForLB = distP1P2;
                            maxCosPi1DefinesFiltering = cosPi1;
                            dp1ForLB = distOP2;
                            dp2ForLB = distOP1;
                            p1IDForFiltering = p2ID;
                            p2IDForFiltering = p1ID;
                        }
                    }

                }
                String key = p1IDForPartitioning + "-" + p2IDForPartitioning;
                if (!ret.containsKey(key)) {
                    ret.put(key, new TreeSet<>());
                }
                Float coefP1P2ForLB = coefsForBoundsOnPivotPairs.get(p1IDForFiltering + "-" + p2IDForFiltering)[2];
                Float coefP1P2ForUB = coefsForBoundsOnPivotPairs.get(p1IDForPartitioning + "-" + p2IDForPartitioning)[1];
                ObjectMetadata oMetadata = new ObjectMetadata(oID,
                        p1IDForPartitioning, p2IDForPartitioning,
                        p1IDForFiltering, p2IDForFiltering,
                        dp1ForLB, dp2ForLB,
                        dp1ForUB, dp2ForUB,
                        coefP1P2ForLB,
                        coefP1P2ForUB,
                        dp1p2ForLB,
                        dp1p2ForUB);
                ret.get(key).add(oMetadata);
            }
            latch.countDown();
            t += System.currentTimeMillis();
            LOG.log(Level.INFO, "Batch finished in {0} ms", t);
        }
    }

    private class ObjectMetadata {

        private final Object oID;

        private final Object p1IDForUB;
        private final Object p2IDForUB;
        private final Object p1IDForLB;
        private final Object p2IDForLB;

        private final float dOP1ForLB;
        private final float dOP2ForLB;
        private final float dOP1ForUB;
        private final float dOP2ForUB;

        private final float coefP1P2ForLB;
        private final float coefP1P2ForUB;

        private final float dP1P2ForLB;
        private final float dP1P2ForUB;

        public ObjectMetadata(Object oID, Object p1IDForUB, Object p2IDForUB, Object p1IDForLB, Object p2IDForLB, float dOP1ForLB, float dOP2ForLB, float dOP1ForUB, float dOP2ForUB, float coefP1P2ForLB, float coefP1P2ForUB, float dP1P2ForLB, float dP1P2ForUB) {
            this.oID = oID;
            this.p1IDForUB = p1IDForUB;
            this.p2IDForUB = p2IDForUB;
            this.p1IDForLB = p1IDForLB;
            this.p2IDForLB = p2IDForLB;
            this.dOP1ForLB = dOP1ForLB;
            this.dOP2ForLB = dOP2ForLB;
            this.dOP1ForUB = dOP1ForUB;
            this.dOP2ForUB = dOP2ForUB;
            this.coefP1P2ForLB = coefP1P2ForLB;
            this.coefP1P2ForUB = coefP1P2ForUB;
            this.dP1P2ForLB = dP1P2ForLB;
            this.dP1P2ForUB = dP1P2ForUB;
        }

        public float getUBdOQ(Map<Object, Float> queryToPivotsDists) {
            float dQP1 = queryToPivotsDists.get(p1IDForUB);
            float dQP2 = queryToPivotsDists.get(p2IDForUB);
            return getUBdOQ(dQP1, dQP2);
        }

        public float getUBdOQ(float dQP1, float dQP2) {
            return coefP1P2ForUB * (dQP1 * dOP2ForUB + dQP2 * dOP1ForUB) / (dP1P2ForUB);
        }

        public float getLBdOQ(Map<Object, Float> queryToPivotsDists) {
            float dQP1 = queryToPivotsDists.get(p1IDForLB);
            float dQP2 = queryToPivotsDists.get(p2IDForLB);
            return getLBdOQ(dQP1, dQP2);
        }

        public float getLBdOQ(float dQP1, float dQP2) {
            return coefP1P2ForLB * (dQP1 * dOP2ForLB - dQP2 * dOP1ForLB) / (dP1P2ForLB);
        }

        public String getAsCSVString() {
            return oID + ";" + p1IDForUB + ";" + p2IDForUB + ";" + p1IDForLB + ";" + p2IDForLB + ";" + dOP1ForLB + ";" + dOP2ForLB + ";" + dOP1ForUB + ";" + dOP2ForUB + ";" + coefP1P2ForLB + ";" + coefP1P2ForUB + ";" + dP1P2ForLB + ";" + dP1P2ForUB;
        }
    }

}
