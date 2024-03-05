package vm.metricSpace.data.toStringConvertors;

import vm.metricSpace.data.toStringConvertors.impl.FloatMatrixConvertor;
import vm.metricSpace.data.toStringConvertors.impl.FloatVectorConvertor;
import vm.metricSpace.data.toStringConvertors.impl.MPEG7Convertor3;
import vm.metricSpace.data.toStringConvertors.impl.LongVectorConvertor;

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
