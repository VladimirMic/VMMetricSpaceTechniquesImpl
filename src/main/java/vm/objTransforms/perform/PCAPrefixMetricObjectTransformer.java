/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.objTransforms.perform;

import vm.metricSpace.AbstractMetricSpace;

/**
 *
 * @author Vlada
 */
public class PCAPrefixMetricObjectTransformer extends PCAMetricObjectTransformer {

    private final int prefix;

    public PCAPrefixMetricObjectTransformer(float[][] pcaMatrix, float[] meansOverColumns, AbstractMetricSpace<float[]> origFloatVectorSpace, AbstractMetricSpace<float[]> pcaMetricSpace, int prefix) {
        super(pcaMatrix, meansOverColumns, origFloatVectorSpace, pcaMetricSpace);
        this.prefix = prefix;
    }

    @Override
    public Object transformMetricObject(Object obj, Object... params) {
        Comparable objID = origFloatVectorSpace.getIDOfMetricObject(obj);
        float[] vector = origFloatVectorSpace.getDataOfMetricObject(obj);
        int length = Math.min(prefix, pcaMatrix.length);
        final float[] ret = new float[length];
        for (int i = 0; i < length; i++) {
            float[] matrixRow = pcaMatrix[i];
            for (int j = 0; j < vector.length; j++) {
                float v = vector[j];
                ret[i] += v * matrixRow[j];
            }
        }
        return pcaMetricSpace.createMetricObject(objID, ret);
    }

    @Override
    public String getNameOfTransformedSetOfObjects(String origDatasetName, Object... otherParams) {
        return origDatasetName + "_" + getTechniqueAbbreviation() + prefix + "of" + pcaMatrix.length;
    }

    @Override
    public final String getTechniqueAbbreviation() {
        return "PCA_pref";
    }

}
