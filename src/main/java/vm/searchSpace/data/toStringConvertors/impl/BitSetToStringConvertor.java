package vm.searchSpace.data.toStringConvertors.impl;

import java.util.BitSet;
import vm.searchSpace.data.toStringConvertors.SearchObjectDataToStringInterface;

/**
 *
 * @author Vlada
 */
public class BitSetToStringConvertor implements SearchObjectDataToStringInterface<BitSet> {

    private static final LongVectorToStringConvertor LONGS_CONV = new LongVectorToStringConvertor();

    @Override
    public BitSet parseString(String dbString) {
        long[] longs = LONGS_CONV.parseString(dbString);
        return BitSet.valueOf(longs);
    }

    @Override
    public String searchObjectDataToString(BitSet searchObjectData) {
        return LONGS_CONV.searchObjectDataToString(searchObjectData.toLongArray());
    }

}
