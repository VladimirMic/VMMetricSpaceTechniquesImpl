package vm.searchSpace.data.toStringConvertors.impl;

import java.util.ArrayList;
import java.util.List;
import vm.datatools.DataTypeConvertor;
import vm.searchSpace.data.toStringConvertors.SearchObjectDataToStringInterface;

/**
 *
 * @author xmic
 */
public class MocapToStringConvertor implements SearchObjectDataToStringInterface<List<float[][]>> {

    @Override
    public List<float[][]> parseString(String string) {
        List<float[][]> movement = new ArrayList<>();
        String[] lines = string.split("\\|");
        for (String line : lines) {
            String[] split = line.split(";");
            float[][] matrix = new float[split.length][3];
            for (int i = 0; i < split.length; i++) {
                String[] coords = split[i].split(",");
                matrix[i] = DataTypeConvertor.stringArrayToFloats(coords);
            }
            movement.add(matrix);
        }
        return movement;
    }

    @Override
    public String searchObjectDataToString(List<float[][]> searchObjectData) {
        StringBuilder sb = new StringBuilder();
        for (float[][] pose : searchObjectData) {
            String floatMatrixToCsvString = DataTypeConvertor.floatMatrixToCsvString(pose, ",", ";");
            sb.append(floatMatrixToCsvString).append("|");
        }
        return sb.toString();
    }

}
