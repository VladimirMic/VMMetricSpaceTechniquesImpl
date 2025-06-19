package vm.searchSpace.data.toStringConvertors.impl;

import vm.searchSpace.data.toStringConvertors.SearchObjectDataToStringInterface;

/**
 *
 * @author Vlada
 */
public class LongVectorToStringConvertor implements SearchObjectDataToStringInterface<long[]> {

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
    public String searchObjectDataToString(long[] searchObjectData) {
        StringBuilder sb = new StringBuilder(searchObjectData.length * 16);
        for (int i = 0; i < searchObjectData.length; i++) {
            sb.append(searchObjectData[i]).append(";");
        }
        String ret = sb.toString();
        return ret.substring(0, ret.length() - 1);
    }

}
