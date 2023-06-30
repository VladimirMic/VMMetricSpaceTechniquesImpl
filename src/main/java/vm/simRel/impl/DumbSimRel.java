/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.simRel.impl;

import vm.simRel.SimRelInterface;

/**
 *
 * @author Vlada
 */
public class DumbSimRel<T> implements SimRelInterface<T> {

    @Override
    public short getMoreSimilar(T q, T o1, T o2) {
        return 0;
    }

}
