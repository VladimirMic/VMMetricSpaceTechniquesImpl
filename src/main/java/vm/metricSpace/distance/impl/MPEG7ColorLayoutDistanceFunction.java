package vm.metricSpace.distance.impl;

import java.util.Map;
import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 * Distance function for Sapir (CoPhIR) MPEG7 object - ColorLayout. Copied from
 * Messif version 2
 *
 * @author Vladimir Mic, Masaryk University, Brno, Czech Republic,
 * xmic@fi.muni.cz
 */
public class MPEG7ColorLayoutDistanceFunction extends DistanceFunctionInterface<Map<String, byte[]>> {

    /**
     * Class id for Java serialization.
     */
    private static final long serialVersionUID = 14687684L;

    private static final byte YWeights[] = {3, 3, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
    private static final byte CbWeights[] = {2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
    private static final byte CrWeights[] = {4, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};

    @Override
    public float getDistance(Map<String, byte[]> obj1, Map<String, byte[]> obj2) {
        return (float) (Math.sqrt(sumCoeff(YWeights, obj1.get("YCoeff_byte"), obj2.get("YCoeff_byte")))
                + Math.sqrt(sumCoeff(CrWeights, obj1.get("CrCoeff_byte"), obj2.get("CrCoeff_byte")))
                + Math.sqrt(sumCoeff(CbWeights, obj1.get("CbCoeff_byte"), obj2.get("CbCoeff_byte"))));
    }

    protected static float sumCoeff(byte[] weights, byte[] coeffs1, byte[] coeffs2) {
        float rtv = 0;
        for (int j = Math.min(coeffs1.length, coeffs2.length) - 1; j >= 0; j--) {
            int diff = coeffs1[j] - coeffs2[j];
            rtv += weights[j] * diff * diff;
        }
        return rtv;
    }

}
