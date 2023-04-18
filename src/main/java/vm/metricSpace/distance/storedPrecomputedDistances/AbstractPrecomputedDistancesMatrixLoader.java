package vm.metricSpace.distance.storedPrecomputedDistances;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 * @author Vlada
 */
public abstract class AbstractPrecomputedDistancesMatrixLoader {

    private final static Logger LOG = Logger.getLogger(AbstractPrecomputedDistancesMatrixLoader.class.getName());
    protected Map<String, Integer> columnHeaders;
    protected Map<String, Integer> rowHeaders;

    public AbstractPrecomputedDistancesMatrixLoader() {
        this.rowHeaders = new HashMap<>();
        this.columnHeaders = new HashMap<>();
    }

    public abstract float[][] loadPrecomPivotsToObjectsDists(String datasetName, String pivotSetName, int pivotCount);

    public float[][] loadPrecomPivotsToObjectsDists(String datasetName, String pivotSetName) {
        return this.loadPrecomPivotsToObjectsDists(datasetName, pivotSetName, -1);
    }

    public Map<String, Integer> getRowHeaders() {
        return Collections.unmodifiableMap(rowHeaders);
    }

    public Map<String, Integer> getColumnHeaders() {
        return Collections.unmodifiableMap(columnHeaders);
    }

}
