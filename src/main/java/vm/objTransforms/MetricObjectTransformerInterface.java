package vm.objTransforms;

/**
 *
 * @author Vlada
 */
public interface MetricObjectTransformerInterface {

    public Object transformMetricObject(Object obj, Object... params);

    public String getNameOfTransformedSetOfObjects(String origSetName, Object... otherParams);

    public String getTechniqueAbbreviation();

}
