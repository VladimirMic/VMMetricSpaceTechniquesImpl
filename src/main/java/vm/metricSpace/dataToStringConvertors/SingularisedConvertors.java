package vm.metricSpace.dataToStringConvertors;

import vm.metricSpace.dataToStringConvertors.impl.FloatMatrixConvertor;
import vm.metricSpace.dataToStringConvertors.impl.FloatVectorConvertor;
import vm.metricSpace.dataToStringConvertors.impl.MPEG7Convertor3;
import vm.metricSpace.dataToStringConvertors.impl.LongVectorConvertor;

/**
 *
 * @author Vlada
 */
public class SingularisedConvertors {

    public static final FloatVectorConvertor FLOAT_VECTOR_SPACE = new FloatVectorConvertor();
    public static final LongVectorConvertor LONG_VECTOR_SPACE = new LongVectorConvertor();
    public static final FloatMatrixConvertor FLOAT_MATRIX_SPACE = new FloatMatrixConvertor();
    public static final MPEG7Convertor3 MPEG7_SPACE = new MPEG7Convertor3();
}
