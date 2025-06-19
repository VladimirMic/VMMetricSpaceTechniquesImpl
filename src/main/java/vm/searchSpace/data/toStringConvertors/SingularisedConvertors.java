package vm.searchSpace.data.toStringConvertors;

import vm.searchSpace.data.toStringConvertors.impl.AtrapaToStringConvertor;
import vm.searchSpace.data.toStringConvertors.impl.FloatMatrixToStringConvertor;
import vm.searchSpace.data.toStringConvertors.impl.FloatVectorToStringConvertor;
import vm.searchSpace.data.toStringConvertors.impl.LongVectorToStringConvertor;
import vm.searchSpace.data.toStringConvertors.impl.MPEG7ToStringConvertor3;
import vm.searchSpace.data.toStringConvertors.impl.MocapToStringConvertor;

/**
 *
 * @author Vlada
 */
public class SingularisedConvertors {

    public static final FloatVectorToStringConvertor FLOAT_VECTOR_SPACE = new FloatVectorToStringConvertor();
    public static final LongVectorToStringConvertor LONG_VECTOR_SPACE = new LongVectorToStringConvertor();
    public static final FloatMatrixToStringConvertor FLOAT_MATRIX_SPACE = new FloatMatrixToStringConvertor();
    public static final MPEG7ToStringConvertor3 MPEG7_SPACE = new MPEG7ToStringConvertor3();
    public static final MocapToStringConvertor MOCAP_SPACE = new MocapToStringConvertor();
    public static final AtrapaToStringConvertor EMPTY_CONVERTOR = new AtrapaToStringConvertor();
}
