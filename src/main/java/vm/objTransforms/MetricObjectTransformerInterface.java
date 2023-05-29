package vm.objTransforms;

/**
 *
 * @author Vlada
 */
public interface MetricObjectTransformerInterface {

    public Object transformMetricObject(Object obj, Object... params);

    public String getNameOfTransformedSetOfObjects(String origSetName, boolean learning, Object... otherParams);

    public String getTechniqueAbbreviation();

}
