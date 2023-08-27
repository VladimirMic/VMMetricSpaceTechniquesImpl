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
import vm.metricSpace.distance.bounding.twopivots.impl.PtolemaiosFilteringWithLimitedAnglesSimpleCoef;

/**
 *
 * @author Vlada
 */
public class GRAPPLEPartitioning extends VoronoiPartitioning {

    public static final Logger LOG = Logger.getLogger(GRAPPLEPartitioning.class.getName());
    private final PtolemaiosFilteringWithLimitedAnglesSimpleCoef filter;

    public GRAPPLEPartitioning(PtolemaiosFilteringWithLimitedAnglesSimpleCoef filter, AbstractMetricSpace metricSpace, DistanceFunctionInterface df, List<Object> pivots) {
        super(metricSpace, df, pivots);
        this.filter = filter;
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

                float minCosAlpha = Float.MAX_VALUE;
                float minCosPi1DefinesFiltering = Float.MAX_VALUE;

                Object p1IDForUB = null;
                Object p2IDForUB = null;

                String p1IDForLB = null;
                String p2IDForLB = null;

                float dp1ForLB = 0, dp2ForLB = 0, dp1ForUB = 0, dp2ForUB = 0, dp1p2ForLB = 0, dp1p2ForUB = 0;

                Float oLength = objectsLengths.get(oID);
                for (int p1Index = 0; p1Index < pivotsList.size() - 1; p1Index++) {
                    Object p1 = pivotsList.get(p1Index);
                    Object p1ID = metricSpace.getIDOfMetricObject(p1);
                    Object p1Data = metricSpace.getDataOfMetricObject(p1);
                    float distOP1 = df.getDistance(oData, p1Data, oLength, pivotLengths.get(p1ID));
                    for (int p2Index = p1Index + 1; p2Index < pivotsList.size(); p2Index++) {
                        Object p2 = pivotsList.get(p2Index);
                        Object p2ID = metricSpace.getIDOfMetricObject(p2);
                        Object p2Data = metricSpace.getDataOfMetricObject(p2);
                        float distOP2 = df.getDistance(oData, p2Data, oLength, pivotLengths.get(p2ID));
                        Float distP1P2 = interPivotDists.get(p1ID + "-" + p2ID);
                        if (distP1P2 == null) {
                            distP1P2 = df.getDistance(p1Data, p2Data, pivotLengths.get(p1ID), pivotLengths.get(p2ID));
                            interPivotDists.put(p1ID + "-" + p2ID, distP1P2);
                        }
                        // is this pivot pair best for the partitioning?
                        float alphaCosine = (distOP2 * distOP2 + distOP1 * distOP1 - distP1P2 * distP1P2);
                        if (alphaCosine < minCosAlpha) { // yes
                            minCosAlpha = alphaCosine;
                            dp1ForUB = Math.min(distOP1, distOP2);
                            dp2ForUB = Math.max(distOP1, distOP2);
                            dp1p2ForUB = distP1P2;
                            if (distOP1 < distOP2) {
                                p1IDForUB = p1ID;
                                p2IDForUB = p2ID;
                            } else {
                                p1IDForUB = p2ID;
                                p2IDForUB = p1ID;
                            }
                        }
                        float coefP1P2ForLB = filter.getCoef(p1ID.toString(), p2ID.toString(), 2);
                        // is this pivot pair best for the filtering? -- the order of pivots matters!                        
                        float cosPi1 = -coefP1P2ForLB * (distOP1 * distOP1 + distP1P2 * distP1P2 - distOP2 * distOP2) / (2 * distP1P2 * distOP1);
                        if (cosPi1 < minCosPi1DefinesFiltering) { // yes
                            dp1p2ForLB = distP1P2;
                            minCosPi1DefinesFiltering = cosPi1;
                            dp1ForLB = distOP1;
                            dp2ForLB = distOP2;
                            p1IDForLB = p1ID.toString();
                            p2IDForLB = p2ID.toString();
                        }
                        // is this pivot pair best for the filtering? -- opposite order
                        cosPi1 = -coefP1P2ForLB * (-distOP1 * distOP1 + distP1P2 * distP1P2 + distOP2 * distOP2) / (2 * distP1P2 * distOP2);
                        if (cosPi1 < minCosPi1DefinesFiltering) { // yes
                            dp1p2ForLB = distP1P2;
                            minCosPi1DefinesFiltering = cosPi1;
                            dp1ForLB = distOP2;
                            dp2ForLB = distOP1;
                            p1IDForLB = p2ID.toString();
                            p2IDForLB = p1ID.toString();
                        }
                    }
                }
                float coefP1P2ForLB = filter.getCoef(p1IDForLB, p2IDForLB, 2);
                float coefP1P2ForUB = filter.getCoef(p1IDForLB, p2IDForLB, 1);
                ObjectMetadata oMetadata = new ObjectMetadata(oID,
                        p1IDForUB, p2IDForUB,
                        p1IDForLB, p2IDForLB,
                        dp1ForLB, dp2ForLB,
                        dp1ForUB, dp2ForUB,
                        coefP1P2ForLB,
                        coefP1P2ForUB,
                        dp1p2ForLB,
                        dp1p2ForUB);
                String key = p1IDForUB + "-" + p2IDForUB;
                if (!ret.containsKey(key)) {
                    ret.put(key, new TreeSet<>());
                }
                ret.get(key).add(oMetadata);
                double angleDeg = vm.math.Tools.radToDeg(Math.acos(minCosAlpha));
                LOG.log(Level.INFO, "oID {0} assigned to {1}. Partitioning: angle {2}, dP1P2: {3}, dP1: {4}, dP1: {5}, coef for LB: {6}", new Object[]{oID.toString(), key, angleDeg, dp1p2ForUB, dp1ForUB, dp2ForUB, coefP1P2ForLB});
            }
            latch.countDown();
            t += System.currentTimeMillis();
            LOG.log(Level.INFO, "Batch finished in {0} ms", t);
        }
    }

    public class ObjectMetadata implements Comparable<ObjectMetadata> {

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
            if (oID == null) {
                throw new IllegalArgumentException("oID cannot be null");
            }
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
            return oID + "," + p1IDForUB + "," + p2IDForUB + "," + p1IDForLB + "," + p2IDForLB + "," + dOP1ForLB + "," + dOP2ForLB + "," + dOP1ForUB + "," + dOP2ForUB + "," + coefP1P2ForLB + "," + coefP1P2ForUB + "," + dP1P2ForLB + "," + dP1P2ForUB;
        }

        @Override
        public String toString() {
            return getAsCSVString();
        }

        @Override
        public int compareTo(ObjectMetadata t) {
            if (t == null || t.oID == null) {
                return -1;
            }
            return oID.toString().compareTo(t.oID.toString());
        }

        public Object getoID() {
            return oID;
        }

    }

}
