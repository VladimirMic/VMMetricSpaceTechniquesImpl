package vm.searchSpace.datasetPartitioning.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.datasetPartitioning.impl.batchProcessor.AbstractPivotBasedPartitioningProcessor;
import vm.searchSpace.distance.DistanceFunctionInterface;
import vm.searchSpace.distance.bounding.twopivots.impl.DataDependentPtolemaicFiltering;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class GRAPPLEPartitioning<T> extends VoronoiPartitioningWithoutFilter<T> {

    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    public static final Logger LOG = Logger.getLogger(GRAPPLEPartitioning.class.getName());
    public static final Integer LB_COUNT = 24;
    private final DataDependentPtolemaicFiltering filter;

    public GRAPPLEPartitioning(DataDependentPtolemaicFiltering filter, AbstractSearchSpace searchSpace, DistanceFunctionInterface df, List<Object> pivots) {
        super(searchSpace, df, pivots);
        this.filter = filter;
    }

    private class ProcessBatch extends AbstractPivotBasedPartitioningProcessor<T> {

        public ProcessBatch(AbstractSearchSpace searchSpace, DistanceFunctionInterface df, float[] pivotLengths) {
            super(searchSpace, df, pivotsData, pivots.size(), pivotLengths);
        }

        @Override
        @SuppressWarnings("null")
        public void run() {
            long t = -System.currentTimeMillis();
            Map<String, Float> interPivotDists = new HashMap<>();
            for (int i = 0; batch.hasNext(); i++) {
                Object o = batch.next();
                T oData = searchSpace.getDataOfObject(o);
                Comparable oID = searchSpace.getIDOfObject(o);

                float minCosAlpha = Float.MAX_VALUE;

                Integer p1IdxForUB = null;
                Integer p2IdxForUB = null;

                float dp1ForUB = 0, dp2ForUB = 0, dp1p2ForUB = 0;

                Float oLength = objectsLengths.get(oID);
                ObjectMetadata oMetadata = new ObjectMetadata(oID);
                for (int p1Index = 0; p1Index < pivots.size() - 1; p1Index++) {
                    Comparable p1ID = pivotsIDs.get(p1Index);
                    T p1Data = pivotsData.get(p1Index);
                    float distOP1 = df.getDistance(oData, p1Data, oLength, pivotLengths[p1Index]);
                    for (int p2Index = p1Index + 1; p2Index < pivots.size(); p2Index++) {
                        Comparable p2ID = pivotsIDs.get(p2Index);
                        T p2Data = pivotsData.get(p2Index);
                        float distOP2 = df.getDistance(oData, p2Data, oLength, pivotLengths[p2Index]);
                        Float distP1P2 = interPivotDists.get(p1ID + "-" + p2ID);
                        if (distP1P2 == null) {
                            distP1P2 = df.getDistance(p1Data, p2Data, pivotLengths[p1Index], pivotLengths[p2Index]);
                            interPivotDists.put(p1ID.toString() + "-" + p2ID.toString(), distP1P2);
                        }
                        // is this pivot pair best for the partitioning?
                        float alphaCosine = (distOP1 * distOP1 + distOP2 * distOP2 - distP1P2 * distP1P2);

                        if (alphaCosine < minCosAlpha) { // yes
                            minCosAlpha = alphaCosine;
                            dp1ForUB = Math.min(distOP1, distOP2);
                            dp2ForUB = Math.max(distOP1, distOP2);
                            dp1p2ForUB = distP1P2;
                            if (distOP1 < distOP2) {
                                p1IdxForUB = p1Index;
                                p2IdxForUB = p2Index;
                            } else {
                                p1IdxForUB = p2Index;
                                p2IdxForUB = p1Index;
                            }
                        }
                        float coefP1P2ForLB = filter.getCoefPivotPivotForLB(p1Index, p2Index);
                        // is this pivot pair best for the filtering? -- the order of pivots matters!                        
                        float cosPi = -coefP1P2ForLB * (distOP1 * distOP1 + distP1P2 * distP1P2 - distOP2 * distOP2) / (2 * distP1P2 * distOP1);
                        oMetadata.addDataForLB(cosPi, distOP1, distOP2, distP1P2, p1Index, p2Index, coefP1P2ForLB);
                        // -- opposite order
                        cosPi = -coefP1P2ForLB * (-distOP1 * distOP1 + distP1P2 * distP1P2 + distOP2 * distOP2) / (2 * distP1P2 * distOP2);
                        oMetadata.addDataForLB(cosPi, distOP2, distOP1, distP1P2, p1Index, p2Index, coefP1P2ForLB);
                    }
                }
                float coefP1P2ForUB = filter.getCoefPivotPivotForLB(p1IdxForUB, p2IdxForUB);
                oMetadata.setDataForUB(dp1ForUB, dp2ForUB, dp1p2ForUB, coefP1P2ForUB, p1IdxForUB, p2IdxForUB);
                String key = p1IdxForUB + "-" + p2IdxForUB;
                if (true) {
                    throw new UnsupportedOperationException();
                }
//                ret.get(key).add(oMetadata); // resolve
                ret[0].add(oMetadata); // resolve
                double angleDeg = vm.mathtools.Tools.radToDeg(Math.acos(minCosAlpha / (2 * dp1ForUB * dp2ForUB)));
                LOG.log(Level.INFO, "oID {0} assigned to {1}. Partitioning: angle {2}, dP1P2: {3}, dP1: {4}, dP1: {5}", new Object[]{oID.toString(), key, angleDeg, dp1p2ForUB, dp1ForUB, dp2ForUB});
            }
            latch.countDown();
            t += System.currentTimeMillis();
            LOG.log(Level.INFO, "Batch finished in {0} ms", t);
        }

        @Override
        protected float getDistIfSmallerThan(float radius, T oData, T pData, Float oLength, Float pLength, float[] opDists, int pCounter) {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }
    }

    public static final ObjectMetadata getObjectMetadataInstance(String string) {
        String[] split = string.split(",");;
        return new ObjectMetadata(split[0]);
    }

    public static class ObjectMetadata implements Comparable<ObjectMetadata> {

        private final Comparable oID;
        private final SortedSet<LBMetadata> lbData;

        private int p1IdxForUB;
        private int p2IdxForUB;
        private float dOP1ForUB;
        private float dOP2ForUB;
        private float dP1P2ForUB;
        private float coefP1P2ForUB;

        public ObjectMetadata(Comparable oID) {
            if (oID == null) {
                throw new IllegalArgumentException("oID cannot be null");
            }
            this.oID = oID;
            this.lbData = new TreeSet<>();
        }

        public void addDataForLB(float cosPi1, float distOP2, float distOP1, float distP1P2, int p1ID, int p2ID, float coefP1P2ForLB) {
            LBMetadata lb = new LBMetadata(cosPi1, p1ID, p2ID, distOP1, distOP2, distP1P2, coefP1P2ForLB);
            lbData.add(lb);
            if (lbData.size() > LB_COUNT) {
                lbData.remove(lbData.last());
            }
        }

        public void setDataForUB(float distOP1, float distOP2, float distP1P2, float coefP1P2ForUB, int p1IDForUB, int p2IDForUB) {
            this.dOP1ForUB = distOP1;
            this.dOP2ForUB = distOP2;
            this.dP1P2ForUB = distP1P2;
            this.coefP1P2ForUB = coefP1P2ForUB;
            this.p1IdxForUB = p1IDForUB;
            this.p2IdxForUB = p2IDForUB;
        }

        public String getAsCSVString() {
            StringBuilder sb = new StringBuilder();
            sb.append(oID).append(",").append(p1IdxForUB).append(",").append(p2IdxForUB).append(",").append(dOP1ForUB).append(",").append(dOP2ForUB).append(",").append(coefP1P2ForUB).append(",").append(dP1P2ForUB).append(",");
            for (LBMetadata lBMetadata : lbData) {
                sb.append(lBMetadata.toString());
            }
            return sb.toString();
        }

        public float getUBdOQ(Map<Object, Float> queryToPivotsDists) {
            float dQP1 = queryToPivotsDists.get(p1IdxForUB);
            float dQP2 = queryToPivotsDists.get(p2IdxForUB);
            return getUBdOQ(dQP1, dQP2);
        }

        public float getUBdOQ(float dQP1, float dQP2) {
            return coefP1P2ForUB * (dQP1 * dOP2ForUB + dQP2 * dOP1ForUB) / (dP1P2ForUB);
        }

        public boolean getLBdOQ(Map<Comparable, Float> queryToPivotsDists, float range) {
            for (LBMetadata lBMetadata : lbData) {
                float dQP1 = queryToPivotsDists.get(lBMetadata.p1IDForLB);
                float dQP2 = queryToPivotsDists.get(lBMetadata.p2IDForLB);
                float lb = lBMetadata.coefP1P2ForLB * Math.abs(dQP1 * lBMetadata.dOP2ForLB - dQP2 * lBMetadata.dOP1ForLB) / (lBMetadata.dP1P2ForLB);
                if (lb > range) {
                    return false;
                }
            }
            return true;
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

        public Comparable getoID() {
            return oID;
        }

        public static class LBMetadata implements Comparable<LBMetadata> {

            protected final float cosPi;
            protected final Object p1IDForLB;
            protected final Object p2IDForLB;
            protected final float dOP1ForLB;
            protected final float dOP2ForLB;
            protected final float dP1P2ForLB;
            protected final float coefP1P2ForLB;

            public LBMetadata(float cosPi, Object p1IDForLB, Object p2IDForLB, float dOP1ForLB, float dOP2ForLB, float dP1P2ForLB, float coefP1P2ForLB) {
                this.cosPi = cosPi;
                this.p1IDForLB = p1IDForLB;
                this.p2IDForLB = p2IDForLB;
                this.dOP1ForLB = dOP1ForLB;
                this.dOP2ForLB = dOP2ForLB;
                this.dP1P2ForLB = dP1P2ForLB;
                this.coefP1P2ForLB = coefP1P2ForLB;
            }

            @Override
            public String toString() {
                return cosPi + "," + p1IDForLB + "," + p2IDForLB + "," + dOP1ForLB + "," + dOP2ForLB + "," + dP1P2ForLB + "," + coefP1P2ForLB;
            }

            @Override
            public int compareTo(LBMetadata o) {
                int ret = Float.compare(cosPi, o.cosPi);
                if (ret != 0) {
                    return ret;
                }
                ret = p1IDForLB.toString().compareTo(o.p1IDForLB.toString());
                if (ret != 0) {
                    return ret;
                }
                return p2IDForLB.toString().compareTo(o.p2IDForLB.toString());
            }

        }

    }

}
