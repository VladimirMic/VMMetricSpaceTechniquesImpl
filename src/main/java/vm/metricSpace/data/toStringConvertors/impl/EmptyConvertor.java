/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.metricSpace.data.toStringConvertors.impl;

import vm.metricSpace.data.toStringConvertors.MetricObjectDataToStringInterface;

/**
 * Does not serialise anything
 *
 * @author xmic
 */
public class EmptyConvertor implements MetricObjectDataToStringInterface {

    @Override
    public Object parseString(String dbString) {
        return null;
    }

    @Override
    public String metricObjectDataToString(Object metricObjectData) {
        return "";
    }

}
