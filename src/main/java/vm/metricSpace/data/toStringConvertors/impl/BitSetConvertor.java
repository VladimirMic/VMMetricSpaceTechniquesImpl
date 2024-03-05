package vm.metricSpace.data.toStringConvertors.impl;

import java.util.BitSet;
import vm.metricSpace.data.toStringConvertors.MetricObjectDataToStringInterface;

/**
 *
 * @author Vlada
 */
public class BitSetConvertor implements MetricObjectDataToStringInterface<BitSet> {

    private static final LongVectorConvertor LONGS_CONV = new LongVectorConvertor();

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
