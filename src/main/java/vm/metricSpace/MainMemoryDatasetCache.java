/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.metricSpace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        super(datasetName, metricSpace, metricSpacesStorage);
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
    public int getRecommendedNumberOfPivotsForFiltering() {
        return -1;
    }

    @Override
    public List<Object> getPivots(int count) {
        if (count < 0) {
            count = pivots.size();
        }
        if (count > pivots.size() && pivotsLoaded()) {
            LOG.log(Level.WARNING, "Just {0} pivots found. They are returned", pivots.size());
            count = pivots.size();
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
    public List<Object> getQueryObjects(Object... params) {
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
    public Map<Comparable, T> getKeyValueStorage() {
        if (!dataLoaded()) {
            loadAllDataObjets();
        }
        VMMemoryMapForDataWithIntIDs ret = new VMMemoryMapForDataWithIntIDs(metricSpace, dataObjects);
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
        LOG.log(Level.INFO, "Going to delete {0} pivots from the cache main memory", this.pivots.size());
        pivots.clear();
        System.gc();
    }

    public void unloadQueries() {
        LOG.log(Level.INFO, "Going to delete {0} query objects from the cache main memory", this.queries.size());
        queries.clear();
        System.gc();
    }

    public void unloadDataObjets() {
        LOG.log(Level.INFO, "Going to delete {0} data objects from the cache main memory", this.dataObjects.size());
        dataObjects.clear();
        System.gc();
    }

    @Override
    public boolean hasKeyValueStorage() {
        return false;
    }

    @Override
    public void deleteKeyValueStorage() {
    }

    private class VMMemoryMapForDataWithIntIDs implements Map<Comparable, T> {

        private final List<T> array;

        public VMMemoryMapForDataWithIntIDs(AbstractMetricSpace<T> metricSpace, List<Object> metricObjects) {
            array = new ArrayList<>(metricObjects.size());
            for (int i = 0; i < metricObjects.size(); i++) {
                Object metricObject = metricObjects.get(i);
                Comparable idOfMetricObject = metricSpace.getIDOfMetricObject(metricObject);
                T data = metricSpace.getDataOfMetricObject(metricObject);
                int idx = Integer.parseInt(idOfMetricObject.toString()) - 1;
                if (array.size() < idx || array.get(idx) != null) {
                    throw new Error("The array already contains the value with key " + idx);
                }
                array.remove(idx);
                array.add(idx, data);
                if ((i + 1) % 100000 == 0) {
                    LOG.log(Level.INFO, "Loaded {0} objects into map", i + 1);
                }
            }
            LOG.log(Level.INFO, "Finished loading map of size {0} objects", metricObjects.size());
        }

        @Override
        public int size() {
            return array.size();
        }

        @Override
        public boolean isEmpty() {
            return size() == 0;
        }

        private int getKeyAsIdx(Comparable key) {
            int id;
            if (key instanceof String) {
                id = Integer.parseInt(key.toString());
            } else if (!(key instanceof Long) && !(key instanceof Integer)) {
                return -1;
            } else {
                id = (int) key;
            }
            return id - 1;
        }

        @Override
        public boolean containsKey(Object key) {
            if (key instanceof Comparable) {
                int id = getKeyAsIdx((Comparable) key);
                return id > 0 && id <= size();
            }
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public T get(Object key) {
            if (key instanceof Comparable) {
                int id = getKeyAsIdx((Comparable) key);
                if (id >= 0 && id < size()) {
                    return array.get(id);
                }
            }
            return null;
        }

        @Override
        public Object put(Comparable key, Object value) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public T remove(Object key) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void putAll(java.util.Map m) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Set keySet() {
            System.out.println("Minimum is 0, maximum is " + size() + ". Use row index as the key");
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Collection values() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Set entrySet() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };

}
