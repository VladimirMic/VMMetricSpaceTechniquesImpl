package vm.searchSpace.distance.bounding.nopivot;

import vm.searchSpace.distance.bounding.BoundsOnDistanceEstimation;

/**
 *
 * @author Vlada
 */
public abstract class NoPivotFilter extends BoundsOnDistanceEstimation {

    public NoPivotFilter(String namePrefix) {
        super(namePrefix);
    }
    
    // TODO Secondary filtering - is implemented, but give it an abstract method here

}
