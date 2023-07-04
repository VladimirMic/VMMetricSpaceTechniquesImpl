package vm.metricSpace.distance.impl;

import java.util.Map;
import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 * Distance function for Sapir (CoPhIR) objects.
 *
 * @author Vladimir Mic, Masaryk University, Brno, Czech Republic, copied form
 * the MESSIF library, xmic@fi.muni.cz
 */
public class Sapir3DistanceFunction extends DistanceFunctionInterface<Map<String, Object>> {

    private final DistanceFunctionInterface<Map<String, byte[]>> colorLayoutDF = new MPEG7ColorLayoutDistanceFunction();
    private final DistanceFunctionInterface<short[]> colorStructureDF = new L1OnShortsArray();
    private final DistanceFunctionInterface<byte[]> edgeHistogramDF = new MPEG7EdgeCompDistanceFunction();
    private final DistanceFunctionInterface<int[]> scalableColorDF = new L1OnIntsArray();

    @Override
    public float getDistance(Map<String, Object> obj1, Map<String, Object> obj2) {
        float ret = 0;

        byte[] edgeHistogram1 = (byte[]) obj1.get("EdgeHistogramType_byte");
        byte[] edgeHistogram2 = (byte[]) obj2.get("EdgeHistogramType_byte");
        if (edgeHistogram1 != null && edgeHistogram2 != null) {
            ret += edgeHistogramDF.getDistance(edgeHistogram1, edgeHistogram2) * 4.5 / 68.0;
        }
        short[] colorStructure1 = (short[]) obj1.get("ColorStructureType_short");
        short[] colorStructure2 = (short[]) obj2.get("ColorStructureType_short");
        if (colorStructure1 != null && colorStructure2 != null) {
            ret += colorStructureDF.getDistance(colorStructure1, colorStructure2) * 2.5 / 40.0 / 255.0;
        }

        int[] scalableColor1 = (int[]) obj1.get("ScalableColorType_int");
        int[] scalableColor2 = (int[]) obj2.get("ScalableColorType_int");
        if (scalableColor1 != null && scalableColor2 != null) {
            ret += scalableColorDF.getDistance(scalableColor1, scalableColor2) * 2.5 / 3000.0;
        }

        Map<String, byte[]> colorLayout1 = (Map<String, byte[]>) obj1.get("ColorLayoutType");
        Map<String, byte[]> colorLayout2 = (Map<String, byte[]>) obj2.get("ColorLayoutType");
        if (colorLayout1 != null && colorLayout2 != null) {
            ret += colorLayoutDF.getDistance(colorLayout1, colorLayout2) * 1.5 / 300.0;
        }

        return ret;
    }

}
