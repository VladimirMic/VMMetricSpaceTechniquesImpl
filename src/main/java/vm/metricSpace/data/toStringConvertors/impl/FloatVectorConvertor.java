package vm.metricSpace.data.toStringConvertors.impl;

import vm.datatools.DataTypeConvertor;
import vm.metricSpace.data.toStringConvertors.MetricObjectDataToStringInterface;

/**
 *
 * @author Vlada
 */
public class FloatVectorConvertor implements MetricObjectDataToStringInterface<float[]> {

    private final String columnDelimiter = ",";

    @Override
    public float[] parseString(String dbString) {
        return DataTypeConvertor.stringToFloats(dbString, columnDelimiter);
    }

    @Override
    public String metricObjectDataToString(float[] metricObjectData) {
        return DataTypeConvertor.floatsToString(metricObjectData, columnDelimiter);
    }

}
