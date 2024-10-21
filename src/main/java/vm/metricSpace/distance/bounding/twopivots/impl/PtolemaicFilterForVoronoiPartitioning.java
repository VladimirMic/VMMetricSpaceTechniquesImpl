/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package vm.metricSpace.distance.bounding.twopivots.impl;

/**
 *
 * @author Vlada
 */
public interface PtolemaicFilterForVoronoiPartitioning {

    public float lowerBound(float distOPi, float distOPj, int iIdx, int jIdx, int pCur);

    public float upperBound(float distOPi, float distOPj, int iIdx, int jIdx, int pCur);

    public boolean isQueryDynamicPivotPairs();

    public int[] pivotsOrderForLB();
}
