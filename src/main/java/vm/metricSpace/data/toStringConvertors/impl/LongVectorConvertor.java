package vm.metricSpace.data.toStringConvertors.impl;

import vm.metricSpace.data.toStringConvertors.MetricObjectDataToStringInterface;

/**
 *
 * @author Vlada
 */
public class LongVectorConvertor implements MetricObjectDataToStringInterface<long[]> {

    @Override
    public long[] parseString(String dbString) {
        String[] split = dbString.split(";");
        long[] ret = new long[split.length];
        for (int i = 0; i < split.length; i++) {
            ret[i] = Long.parseLong(split[i]);
        }
        return ret;
    }

    @Override
    public String metricObjectDataToString(long[] metricObjectData) {
        StringBuilder sb = new StringBuilder(metricObjectData.length * 16);
        for (int i = 0; i < metricObjectData.length; i++) {
            sb.append(metricObjectData[i]).append(";");
        }
        String ret = sb.toString();
        return ret.substring(0, ret.length() - 1);
    }

}
