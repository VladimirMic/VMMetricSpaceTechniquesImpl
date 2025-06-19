package vm.searchSpace;

import java.util.Map;

/**
 * Currently used for vectors of random numbers
 *
 * @author Vlada
 * @param <T> type of data used to compute the distance
 */
public class SimpleDatasetImpl<T> extends Dataset<T> {

    private int recommendedNumberOfPivots = -1;
    private final String querysetName;
    private final String pivotsetName;

    public SimpleDatasetImpl(String datasetName, String querysetName, String pivotsetName, AbstractSearchSpacesStorage searchSpacesStorage) {
        super(datasetName, searchSpacesStorage);
        this.querysetName = querysetName;
        this.pivotsetName = pivotsetName;
    }

    @Override
    public Map<Comparable, T> getKeyValueStorage() {
        return null;
    }

    @Override
    public boolean hasKeyValueStorage() {
        return false;
    }

    @Override
    public void deleteKeyValueStorage() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public int getRecommendedNumberOfPivotsForFiltering() {
        return recommendedNumberOfPivots;
    }

    public void setRecommendedNumberOfPivots(int recommendedNumberOfPivots) {
        this.recommendedNumberOfPivots = recommendedNumberOfPivots;
    }

    @Override
    public String getQuerySetName() {
        return querysetName;
    }

    @Override
    public String getPivotSetName() {
        return pivotsetName;
    }

    @Override
    public boolean shouldStoreDistsToPivots() {
        return false;
    }

    @Override
    public boolean shouldCreateKeyValueStorage() {
        return false;
    }

}
