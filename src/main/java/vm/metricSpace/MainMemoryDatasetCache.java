/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.metricSpace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;

/**
 *
 * @author xmic
 * @param <T>
 */
public class MainMemoryDatasetCache<T> extends Dataset<T> {

    private static final Logger LOG = Logger.getLogger(MainMemoryDatasetCache.class.getName());

    public MainMemoryDatasetCache(AbstractMetricSpace<T> metricSpace, String datasetName) {
        this(metricSpace, datasetName, null);
    }

    public MainMemoryDatasetCache(AbstractMetricSpace<T> metricSpace, String datasetName, AbstractMetricSpacesStorage metricSpacesStorage) {
        this.datasetName = datasetName;
        this.metricSpace = metricSpace;
        this.metricSpacesStorage = metricSpacesStorage;
    }

    public MainMemoryDatasetCache(AbstractMetricSpace<T> metricSpace) {
        this(metricSpace, "");
    }

    private final List<Object> pivots = new ArrayList();
    private final List<Object> dataObjects = new ArrayList();
    private final List<Object> queries = new ArrayList();

    public void addPivots(List<Object> pivots) {
        this.pivots.addAll(pivots);
        LOG.log(Level.INFO, "Cached {0} pivots in the main memory", this.pivots.size());
    }

    public void addAllDataObjects(Iterator<Object> dataObjects) {
        List<Object> list = Tools.getObjectsFromIterator(dataObjects);
        this.dataObjects.addAll(list);
        LOG.log(Level.INFO, "Cached {0} data objects in the main memory", this.dataObjects.size());
    }

    public void addAllDataObjects(Object[] dataObjects) {
        this.dataObjects.addAll(Arrays.asList(dataObjects));
        LOG.log(Level.INFO, "Cached {0} data objects in the main memory", this.dataObjects.size());
    }

    public void addPivots(Object[] pivots) {
        this.pivots.addAll(Arrays.asList(pivots));
        LOG.log(Level.INFO, "Cached {0} pivots in the main memory", this.pivots.size());
    }

    public void addQueries(List<Object> queries) {
        this.queries.addAll(queries);
        LOG.log(Level.INFO, "Cached {0} queries in the main memory", this.queries.size());
    }

    public void addQueries(Object[] queries) {
        this.queries.addAll(Arrays.asList(queries));
        LOG.log(Level.INFO, "Cached {0} queries in the main memory", this.queries.size());
    }

    @Override
    public List<Object> getPivots(int count) {
        if (count < 0) {
            count = this.pivots.size();
        }
        LOG.log(Level.INFO, "Provided {0} pivots from the cache main memory", count);
        return Collections.unmodifiableList(pivots.subList(0, count));
    }

    @Override
    public Iterator<Object> getMetricObjectsFromDataset(Object... params) {
        LOG.log(Level.INFO, "Provided {0} data objects from the cache main memory", this.dataObjects.size());
        return dataObjects.iterator();
    }

    @Override
    public List<Object> getMetricQueryObjects() {
        LOG.log(Level.INFO, "Provided {0} queries from the cache main memory", this.queries.size());
        return Collections.unmodifiableList(queries);
    }

    @Override
    public List<Object> getSampleOfDataset(int objCount) {
        if (!dataLoaded()) {
            return super.getSampleOfDataset(objCount);
        }
        if (objCount > dataObjects.size()) {
            LOG.log(Level.WARNING, "Just {0} are cached but {1} asked. Returning {0} objects", new Object[]{dataObjects.size(), objCount});
            return Collections.unmodifiableList(dataObjects);
        }
        return dataObjects.subList(0, objCount);
    }

    public boolean pivotsLoaded() {
        return !pivots.isEmpty();
    }

    public boolean queriesLoaded() {
        return !queries.isEmpty();
    }

    public boolean dataLoaded() {
        return !dataObjects.isEmpty();
    }

    @Override
    public Map<Object, Object> getKeyValueStorage() {
        if (!dataLoaded()) {
            loadAllDataObjets();
        }
        Map<Object, Object> ret = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(metricSpace, getMetricObjectsFromDataset(), true);
        LOG.log(Level.INFO, "Returning the cached map of objects from the main memory. Size: {0} objects.", ret.size());
        return ret;
    }

    public void setName(String newName) {
        this.datasetName = newName;
    }

    public int getDatasetSize() {
        return dataObjects.size();
    }

    public void loadAllDataObjets() {
        if (dataLoaded()) {
            LOG.log(Level.INFO, "Already cached {0} data objects in the main memory. No change made.", this.dataObjects.size());
            return;
        }
        Iterator<Object> it = super.getMetricObjectsFromDataset();
        addAllDataObjects(it);
    }

    public void unloadPivots() {
        pivots.clear();
    }

    public void unloadQueries() {
        queries.clear();
    }

    public void unloadDataObjets() {
        dataObjects.clear();
    }

}
