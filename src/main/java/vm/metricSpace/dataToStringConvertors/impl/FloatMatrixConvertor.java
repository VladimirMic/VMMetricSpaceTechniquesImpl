package vm.metricSpace.dataToStringConvertors.impl;

import vm.datatools.DataTypeConvertor;
import vm.metricSpace.dataToStringConvertors.MetricObjectDataToStringInterface;

/**
 *
 * @author Vlada
 */
public class FloatMatrixConvertor implements MetricObjectDataToStringInterface<float[][]> {

    private final String columnDelimiter = ",";

    @Override
    public float[][] parseString(String dbString) {
        return DataTypeConvertor.stringToFloatMatrix(dbString, columnDelimiter);
    }

    @Override
    public String metricObjectDataToString(float[][] metricObjectData) {
        return DataTypeConvertor.floatMatrixToCsvString(metricObjectData, columnDelimiter);
    }

}
