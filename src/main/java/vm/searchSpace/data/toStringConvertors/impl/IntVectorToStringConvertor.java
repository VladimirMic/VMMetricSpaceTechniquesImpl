/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.searchSpace.data.toStringConvertors.impl;

import vm.datatools.DataTypeConvertor;
import vm.searchSpace.data.toStringConvertors.SearchObjectDataToStringInterface;

/**
 *
 * @author au734419
 */
public class IntVectorToStringConvertor implements SearchObjectDataToStringInterface<int[]> {

    private final String columnDelimiter = ",";

    @Override
    public int[] parseString(String dbString) {
        return DataTypeConvertor.stringToInts(dbString, columnDelimiter);
    }

    @Override
    public String searchObjectDataToString(int[] searchObjectData) {
        return DataTypeConvertor.intsToString(searchObjectData, columnDelimiter);
    }
}
