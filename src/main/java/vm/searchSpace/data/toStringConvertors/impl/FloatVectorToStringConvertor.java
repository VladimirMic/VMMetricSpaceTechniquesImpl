package vm.searchSpace.data.toStringConvertors.impl;

import vm.datatools.DataTypeConvertor;
import vm.searchSpace.data.toStringConvertors.SearchObjectDataToStringInterface;

/**
 *
 * @author Vlada
 */
public class FloatVectorToStringConvertor implements SearchObjectDataToStringInterface<float[]> {

    private final String columnDelimiter = ",";

    @Override
    public float[] parseString(String dbString) {
        return DataTypeConvertor.stringToFloats(dbString, columnDelimiter);
    }

    @Override
    public String searchObjectDataToString(float[] searchObjectData) {
        return DataTypeConvertor.floatsToString(searchObjectData, columnDelimiter);
    }

}
