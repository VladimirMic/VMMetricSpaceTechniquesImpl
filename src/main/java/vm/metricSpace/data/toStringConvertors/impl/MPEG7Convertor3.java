package vm.metricSpace.data.toStringConvertors.impl;

import java.util.HashMap;
import java.util.Map;
import vm.datatools.DataTypeConvertor;
import vm.metricSpace.data.toStringConvertors.MetricObjectDataToStringInterface;

/**
 *
 * @author Vlada
 */
public class MPEG7Convertor3 implements MetricObjectDataToStringInterface<Map<String, Object>> {

    @Override
    public Map<String, Object> parseString(String dbString) {
        Map<String, Object> ret = new HashMap<>();
        String[] split = dbString.split(";");

        String[] colorLayoutTypeSplit = split[0].split("\\|");
        byte[] yCoeff_byte = DataTypeConvertor.stringToBytes(colorLayoutTypeSplit[0], ",");
        byte[] crCoeff_byte = DataTypeConvertor.stringToBytes(colorLayoutTypeSplit[1], ",");
        byte[] cbCoeff_byte = DataTypeConvertor.stringToBytes(colorLayoutTypeSplit[2], ",");
        Map<String, byte[]> colorLayoutType = new HashMap<>();
        colorLayoutType.put("YCoeff_byte", yCoeff_byte);
        colorLayoutType.put("CrCoeff_byte", crCoeff_byte);
        colorLayoutType.put("CbCoeff_byte", cbCoeff_byte);
        ret.put("ColorLayoutType", colorLayoutType);

        ret.put("ColorStructureType_short", DataTypeConvertor.stringToShorts(split[1], ","));
        ret.put("EdgeHistogramType_byte", DataTypeConvertor.stringToBytes(split[2], ","));
        ret.put("ScalableColorType_int", DataTypeConvertor.stringToInts(split[3], ","));
        return ret;
    }

    @Override
    public String metricObjectDataToString(Map<String, Object> metricObjectData) {
        StringBuilder sb = new StringBuilder();
        Map<String, byte[]> colorLayout = (Map<String, byte[]>) metricObjectData.get("ColorLayoutType");
        sb.append(DataTypeConvertor.bytesToString((byte[]) colorLayout.get("YCoeff_byte"), ","));
        sb.append("|");
        sb.append(DataTypeConvertor.bytesToString((byte[]) colorLayout.get("CrCoeff_byte"), ","));
        sb.append("|");
        sb.append(DataTypeConvertor.bytesToString((byte[]) colorLayout.get("CbCoeff_byte"), ","));
        sb.append(";");
        sb.append(DataTypeConvertor.shortsToString((short[]) metricObjectData.get("ColorStructureType_short"), ","));
        sb.append(";");
        sb.append(DataTypeConvertor.bytesToString((byte[]) metricObjectData.get("EdgeHistogramType_byte"), ","));
        sb.append(";");
        sb.append(DataTypeConvertor.intsToString((int[]) metricObjectData.get("ScalableColorType_int"), ","));
        return sb.toString();
    }
}
