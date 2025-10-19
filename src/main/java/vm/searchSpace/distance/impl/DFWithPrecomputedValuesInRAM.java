package vm.searchSpace.distance.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.Dataset;
import vm.searchSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author au734419
 * @param <T>
 */
public class DFWithPrecomputedValuesInRAM<T> extends DistanceFunctionInterface<T> {

    private static final Logger LOG = Logger.getLogger(DFWithPrecomputedValuesInRAM.class.getName());

    private final Map<Integer, Integer> idMap;
    private final float[][] dists;

    public DFWithPrecomputedValuesInRAM(Dataset<T> dataset) {
        this(Tools.getObjectsFromIterator(dataset.getSearchObjectsFromDataset()), dataset.getSearchSpace());
    }

    public DFWithPrecomputedValuesInRAM(List objects, AbstractSearchSpace<T> space) {
        idMap = new HashMap<>();
        DistanceFunctionInterface<T> df = space.getDistanceFunction();
        dists = new float[objects.size()][objects.size()];
        for (int i = 0; i < objects.size() - 1; i++) {
            if (i % 1000 == 0) {
                LOG.log(Level.INFO, "Processing obj {0} out of {1}", new Object[]{i, objects.size()});
            }
            Object o1 = objects.get(i);
            T o1Data = space.getDataOfObject(o1);
            int hashCode = Tools.hashArray(o1Data);
            if (idMap.containsKey(hashCode)) {
                throw new IllegalArgumentException();
            }
            idMap.put(hashCode, i);
            for (int j = i + 1; j < objects.size(); j++) {
                Object o2 = objects.get(j);
                T o2Data = space.getDataOfObject(o2);
                if (j == objects.size() - 1) {
                    hashCode = Tools.hashArray(o2Data);
                    idMap.put(hashCode, j);
                }
                float dist = df.getDistance(o1Data, o2Data);
                dists[i][j] = dist;
                dists[j][i] = dist;
            }
        }
    }

    @Override
    public float getDistance(T obj1, T obj2) {
        int h1 = Tools.hashArray(obj1);
        int h2 = Tools.hashArray(obj2);
        Integer i = idMap.get(h1);
        Integer j = idMap.get(h2);
        return dists[i][j];
    }

    public void makeItUtrametric() {
        int n = dists.length;

        // Step 1: Build MST using Prim's algorithm
        boolean[] visited = new boolean[n];
        float[] minEdge = new float[n];
        int[] parent = new int[n];
        Arrays.fill(minEdge, Float.MAX_VALUE);
        minEdge[0] = 0;
        parent[0] = -1;

        for (int i = 0; i < n - 1; i++) {
            // Pick minimum edge vertex not yet visited
            int u = -1;
            float min = Float.MAX_VALUE;
            for (int v = 0; v < n; v++) {
                if (!visited[v] && minEdge[v] < min) {
                    min = minEdge[v];
                    u = v;
                }
            }
            visited[u] = true;

            // Update edges
            for (int v = 0; v < n; v++) {
                if (!visited[v] && dists[u][v] < minEdge[v]) {
                    minEdge[v] = dists[u][v];
                    parent[v] = u;
                }
            }
        }

        // Step 2: Build adjacency list for MST
        List<int[]>[] adj = new ArrayList[n];
        for (int i = 0; i < n; i++) {
            adj[i] = new ArrayList<>();
        }
        for (int v = 1; v < n; v++) {
            int u = parent[v];
            float w = dists[u][v];
            adj[u].add(new int[]{v, Float.floatToIntBits(w)});
            adj[v].add(new int[]{u, Float.floatToIntBits(w)});
        }

        // Step 3: Compute ultrametric distances using DFS (max edge on path)
        for (int i = 0; i < n; i++) {
            float[] maxEdgeOnPath = new float[n];
            Arrays.fill(maxEdgeOnPath, -1);
            dfsMaxEdge(i, -1, 0, adj, maxEdgeOnPath);
            for (int j = 0; j < n; j++) {
                dists[i][j] = maxEdgeOnPath[j];
            }
        }
    }

// Helper DFS: tracks the max edge along path from src to each node
    private void dfsMaxEdge(int node, int parent, float currentMax, List<int[]>[] adj, float[] maxEdgeOnPath) {
        maxEdgeOnPath[node] = currentMax;
        for (int[] edge : adj[node]) {
            int neighbor = edge[0];
            if (neighbor == parent) {
                continue;
            }
            float weight = Float.intBitsToFloat(edge[1]);
            dfsMaxEdge(neighbor, node, Math.max(currentMax, weight), adj, maxEdgeOnPath);
        }
    }

    public boolean isUltrametric() {
        int n = dists.length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < n; k++) {
                    if (dists[i][j] > Math.max(dists[i][k], dists[j][k]) + 1e-6) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

}
