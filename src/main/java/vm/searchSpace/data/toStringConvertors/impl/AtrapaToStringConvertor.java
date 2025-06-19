package vm.searchSpace.data.toStringConvertors.impl;

import vm.searchSpace.data.toStringConvertors.SearchObjectDataToStringInterface;

/**
 * Does not serialise anything
 *
 * @author xmic
 */
public class AtrapaToStringConvertor implements SearchObjectDataToStringInterface {

    @Override
    public Object parseString(String dbString) {
        return null;
    }

    @Override
    public String searchObjectDataToString(Object searchObjectData) {
        return "";
    }

}
