/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.metricSpace.data.toStringConvertors.impl;

import java.util.ArrayList;
import java.util.List;
import vm.datatools.DataTypeConvertor;
import vm.metricSpace.data.toStringConvertors.MetricObjectDataToStringInterface;

/**
 *
 * @author xmic
 */
public class MocapToStringConvertor implements MetricObjectDataToStringInterface<List<float[][]>> {

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
    public String metricObjectDataToString(List<float[][]> metricObjectData) {
        StringBuilder sb = new StringBuilder();
        for (float[][] pose : metricObjectData) {
            String floatMatrixToCsvString = DataTypeConvertor.floatMatrixToCsvString(pose, ",", ";");
            sb.append(floatMatrixToCsvString).append("|");
        }
        return sb.toString();
    }

}
