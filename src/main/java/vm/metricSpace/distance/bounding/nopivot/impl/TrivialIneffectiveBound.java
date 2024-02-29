/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.metricSpace.distance.bounding.nopivot.impl;

import vm.metricSpace.distance.bounding.onepivot.AbstractOnePivotFilter;

/**
 *
 * @author Vlada
 */
public class TrivialIneffectiveBound extends AbstractOnePivotFilter {

    public TrivialIneffectiveBound(String namePrefix) {
        super(namePrefix);
    }

    @Override
    public float lowerBound(Object... args) {
        return 0;
    }

    @Override
    public float upperBound(Object... args) {
        return Float.MAX_VALUE;
    }

    @Override
    protected String getTechName() {
        return "Trivial_ineffective_bound";
    }

    @Override
    public float lowerBound(float distQP, float distOP, int pivotIdx) {
        return 0;
    }

    @Override
    public float upperBound(float distQP, float distOP, int pivotIdx) {
        return Float.MAX_VALUE;
    }

}
