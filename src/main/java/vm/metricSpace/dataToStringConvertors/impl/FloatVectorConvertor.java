package vm.metricSpace.dataToStringConvertors.impl;

import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.DataTypeConvertor;
import vm.metricSpace.dataToStringConvertors.MetricObjectDataToStringInterface;

/**
 *
 * @author Vlada
 */
public class FloatVectorConvertor implements MetricObjectDataToStringInterface<float[]> {

    private final String columnDelimiter = ",";

    @Override
    public float[] parseString(String dbString) {
        try {
            return DataTypeConvertor.stringToFloats(dbString, columnDelimiter);
        } catch (NumberFormatException ex) {
            Logger.getLogger(FloatVectorConvertor.class.getName()).log(Level.SEVERE, "Wrong string: " + dbString);
            throw ex;
        }
    }

    @Override
    public String metricObjectDataToString(float[] metricObjectData) {
        return DataTypeConvertor.floatsToString(metricObjectData, columnDelimiter);
    }

}
