package vm.searchSpace.data.toStringConvertors.impl;

import vm.datatools.DataTypeConvertor;
import vm.searchSpace.data.toStringConvertors.SearchObjectDataToStringInterface;

/**
 *
 * @author Vlada
 */
public class FloatMatrixToStringConvertor implements SearchObjectDataToStringInterface<float[][]> {

    private final String columnDelimiter = ",";

    @Override
    public float[][] parseString(String dbString) {
        return DataTypeConvertor.stringToFloatMatrix(dbString, columnDelimiter);
    }

    @Override
    public String searchObjectDataToString(float[][] searchObjectData) {
        return DataTypeConvertor.floatMatrixToCsvString(searchObjectData, columnDelimiter);
    }

}
