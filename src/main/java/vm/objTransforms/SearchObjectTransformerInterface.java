package vm.objTransforms;

/**
 *
 * @author Vlada
 */
public interface SearchObjectTransformerInterface {

    public Object transformSearchObject(Object obj, Object... params);

    public String getNameOfTransformedSetOfObjects(String origSetName, Object... otherParams);

    public String getTechniqueAbbreviation();

}
