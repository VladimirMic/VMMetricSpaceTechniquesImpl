package vm.metricSpace.data.toStringConvertors.impl;

import java.util.BitSet;
import vm.metricSpace.data.toStringConvertors.MetricObjectDataToStringInterface;

/**
 *
 * @author Vlada
 */
public class BitSetToStringConvertor implements MetricObjectDataToStringInterface<BitSet> {

    private static final LongVectorToStringConvertor LONGS_CONV = new LongVectorToStringConvertor();

    @Override
    public BitSet parseString(String dbString) {
        long[] longs = LONGS_CONV.parseString(dbString);
        return BitSet.valueOf(longs);
    }

    @Override
    public String metricObjectDataToString(BitSet metricObjectData) {
        return LONGS_CONV.metricObjectDataToString(metricObjectData.toLongArray());
    }

}
