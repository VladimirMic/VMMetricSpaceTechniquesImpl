/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.metricSpace.distance.bounding.twopivots;

import vm.metricSpace.distance.bounding.BoundsOnDistanceEstimation;

/**
 *
 * @author Vlada
 */
public abstract class AbstractPtolemaicBasedFiltering extends BoundsOnDistanceEstimation {

    public AbstractPtolemaicBasedFiltering(String resultNamePrefix) {
        super(resultNamePrefix);
    }

    public abstract float lowerBound(float distP2O, float distP1QMultipliedByCoef, float distP1O, float distP2QMultipliedByCoef);

    public abstract float upperBound(float distP2O, float distP1QMultipliedByCoef, float distP1O, float distP2QMultipliedByCoef);

    public abstract float getCoefPivotPivotForLB(int p1Idx, int p2Idx);

    public abstract float getCoefPivotPivotForUB(int p1Idx, int p2Idx);

    @Override
    public float lowerBound(Object... args) {
        return lowerBound(Float.parseFloat(args[0].toString()), Float.parseFloat(args[1].toString()), Float.parseFloat(args[2].toString()), Float.parseFloat(args[3].toString()));
    }

    @Override
    public float upperBound(Object... args) {
        return upperBound(Float.parseFloat(args[0].toString()), Float.parseFloat(args[1].toString()), Float.parseFloat(args[2].toString()), Float.parseFloat(args[3].toString()));
    }

}
