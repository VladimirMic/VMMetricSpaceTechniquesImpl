/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.objTransforms.perform;

import vm.searchSpace.AbstractSearchSpace;

/**
 *
 * @author Vlada
 */
public class PCAPrefixVectorTransformer extends PCAFloatVectorTransformer {

    private final int prefix;

    public PCAPrefixVectorTransformer(float[][] pcaMatrix, float[] meansOverColumns, AbstractSearchSpace<float[]> origFloatVectorSpace, AbstractSearchSpace<float[]> pcaSearchSpace, int prefix) {
        super(pcaMatrix, meansOverColumns, origFloatVectorSpace, pcaSearchSpace);
        this.prefix = prefix;
    }

    @Override
    public Object transformSearchObject(Object obj, Object... params) {
        Comparable objID = origFloatVectorSpace.getIDOfObject(obj);
        float[] vector = origFloatVectorSpace.getDataOfObject(obj);
        int length = Math.min(prefix, pcaMatrix.length);
        final float[] ret = new float[length];
        for (int i = 0; i < length; i++) {
            float[] matrixRow = pcaMatrix[i];
            for (int j = 0; j < vector.length; j++) {
                float v = vector[j];
                ret[i] += v * matrixRow[j];
            }
        }
        return pcaSearchSpace.createSearchObject(objID, ret);
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
