import java.util.*;

public final class Algorithms {
    private Algorithms() {}

    public static final class DSU {
        private final int[] parent;
        private final int[] size;
        private int components;
        private int largest;

        public DSU(int n) {
            parent = new int[n];
            size = new int[n];
            components = n;
            if (n == 0) {
                largest = 0;
            } else {
                largest = 1;
            }
            for (int i = 0; i < n; i++) {
                parent[i] = i;
                size[i] = 1;
            }
        }

        public int find(int v) {
            int root = v;
            while (root != parent[root]) root = parent[root];
            while (v != root) {
                int next = parent[v];
                parent[v] = root;
                v = next;
            }
            return root;
        }

        public boolean union(int u, int v) {
            int a = find(u), b = find(v);
            if (a == b) return false;
            if (size[a] < size[b]) {
                int temp = a;
                a = b;
                b = temp;
            }
            parent[b] = a;
            size[a] += size[b];
            components--;
            largest = Math.max(largest, size[a]);
            return true;
        }

        public int getComponents() { return components; }
        public int getLargest() { return largest; }
    }

    static Map<String, Object> dsu(Map<String, Object> input) {
        DSU sets = new DSU(Input.vertexCount(input));
        int[][] operations = Input.operations(input);
        int[][] states = new int[operations.length][2];
        for (int i = 0; i < operations.length; i++) {
            sets.union(operations[i][0], operations[i][1]);
            states[i][0] = sets.getComponents();
            states[i][1] = sets.getLargest();
        }
        return Map.of("states", states);
    }

    static Map<String, Object> kruskal(Map<String, Object> input) {
        return kruskal(Input.vertexCount(input), Input.edges(input));
    }

    static Map<String, Object> kruskal(int n, int[][] edges) {
        Integer[] ids = new Integer[edges.length];
        for (int i = 0; i < ids.length; i++) ids[i] = i;
        Arrays.sort(ids, Comparator.<Integer>comparingInt(i -> edges[i][2])
                .thenComparingInt(i -> i));
        DSU sets = new DSU(n);
        List<Integer> selected = new ArrayList<>();
        long cost = 0;
        for (int id : ids) {
            int[] edge = edges[id];
            if (sets.union(edge[0], edge[1])) {
                selected.add(id);
                cost += edge[2];
            }
        }
        return Map.of("cost", cost, "edge_ids", selected,
                "components", sets.getComponents());
    }

    static Map<String, Object> prim(Map<String, Object> input) {
        return prim(Input.vertexCount(input), Input.edges(input));
    }

    static Map<String, Object> prim(int n, int[][] edges) {
        Graph graph = new Graph(n, edges, false, true);
        boolean[] visited = new boolean[n];
        PriorityQueue<Integer> queue = new PriorityQueue<>(
                Comparator.<Integer>comparingInt(i -> edges[i][2]).thenComparingInt(i -> i));
        List<Integer> selected = new ArrayList<>();
        long cost = 0;
        int components = 0;
        for (int start = 0; start < n; start++) {
            if (visited[start]) continue;
            components++;
            visit(start, graph, visited, queue);
            while (!queue.isEmpty()) {
                int id = queue.remove();
                int[] edge = edges[id];
                if (visited[edge[0]] && visited[edge[1]]) continue;
                int next = visited[edge[0]] ? edge[1] : edge[0];
                selected.add(id);
                cost += edge[2];
                visit(next, graph, visited, queue);
            }
        }
        return Map.of("cost", cost, "edge_ids", selected, "components", components);
    }

    private static void visit(int v, Graph graph, boolean[] visited,
                              PriorityQueue<Integer> queue) {
        visited[v] = true;
        for (int p = graph.offset[v]; p < graph.offset[v + 1]; p++) {
            if (!visited[graph.to[p]]) queue.add(graph.id[p]);
        }
    }

    static Map<String, Object> scc(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);
        int[] finish = finishOrder(new Graph(n, edges, false, false), false);
        Graph reverse = new Graph(n, edges, true, false);
        int[] component = new int[n];
        Arrays.fill(component, -1);
        int[] stack = new int[n];
        int label = 0;
        for (int i = n - 1; i >= 0; i--) {
            int start = finish[i];
            if (component[start] != -1) continue;
            int count = 0;
            stack[count++] = start;
            component[start] = label;
            while (count > 0) {
                int v = stack[--count];
                for (int p = reverse.offset[v]; p < reverse.offset[v + 1]; p++) {
                    int next = reverse.to[p];
                    if (component[next] == -1) {
                        component[next] = label;
                        stack[count++] = next;
                    }
                }
            }
            label++;
        }
        Set<Long> seen = new HashSet<>();
        List<int[]> condensation = new ArrayList<>();
        for (int[] edge : edges) {
            int a = component[edge[0]], b = component[edge[1]];
            long key = ((long) a << 32) | (b & 0xffffffffL);
            if (a != b && seen.add(key)) condensation.add(new int[]{a, b});
        }
        return Map.of("component", component, "condensation", condensation);
    }

    static Map<String, Object> topo(Map<String, Object> input) {
        Graph graph = new Graph(Input.vertexCount(input), Input.edges(input), false, false);
        int[] order = topologicalOrder(graph);
        return Map.of("acyclic", order != null, "order", order == null ? new int[0] : order);
    }

    static Map<String, Object> dag(Map<String, Object> input) {
        return dag(Input.vertexCount(input), Input.edges(input), Input.source(input));
    }

    static Map<String, Object> dag(int n, int[][] edges, int source) {
        Graph graph = new Graph(n, edges, false, false);
        int[] order = topologicalOrder(graph);
        if (order == null) {
            return Map.of("acyclic", false, "distance", new Long[0], "parent_edge", new int[0]);
        }
        Long[] distance = new Long[n];
        int[] parentEdge = new int[n];
        Arrays.fill(parentEdge, -1);
        distance[source] = 0L;
        for (int v : order) {
            if (distance[v] == null) continue;
            for (int p = graph.offset[v]; p < graph.offset[v + 1]; p++) {
                int next = graph.to[p], id = graph.id[p];
                long candidate = distance[v] + edges[id][2];
                if (distance[next] == null || candidate < distance[next]) {
                    distance[next] = candidate;
                    parentEdge[next] = id;
                }
            }
        }
        return Map.of("acyclic", true, "distance", distance, "parent_edge", parentEdge);
    }

    public static List<Integer> reconstructPath(int source, int target,
                                               int[][] edges, int[] parentEdge) {
        List<Integer> path = new ArrayList<>();
        if (target != source && parentEdge[target] == -1) return path;
        int v = target;
        while (v != source) {
            if (path.size() >= parentEdge.length) throw new IllegalArgumentException("Parent cycle");
            path.add(v);
            int id = parentEdge[v];
            if (id < 0 || id >= edges.length || edges[id][1] != v) {
                throw new IllegalArgumentException("Invalid parent edge");
            }
            v = edges[id][0];
        }
        path.add(source);
        Collections.reverse(path);
        return path;
    }

    private static int[] topologicalOrder(Graph graph) {
        int[] finish = finishOrder(graph, true);
        if (finish == null) return null;
        for (int i = 0, j = finish.length - 1; i < j; i++, j--) {
            int temp = finish[i]; finish[i] = finish[j]; finish[j] = temp;
        }
        return finish;
    }

    private static int[] finishOrder(Graph graph, boolean rejectCycle) {
        int n = graph.offset.length - 1;
        byte[] state = new byte[n];
        int[] stack = new int[n], cursor = new int[n], finish = new int[n];
        int completed = 0;
        for (int start = 0; start < n; start++) {
            if (state[start] != 0) continue;
            int top = 0;
            stack[top] = start;
            cursor[top] = graph.offset[start];
            state[start] = 1;
            while (top >= 0) {
                int v = stack[top];
                if (cursor[top] == graph.offset[v + 1]) {
                    state[v] = 2;
                    finish[completed++] = v;
                    top--;
                    continue;
                }
                int next = graph.to[cursor[top]++];
                if (state[next] == 1 && rejectCycle) return null;
                if (state[next] == 0) {
                    state[next] = 1;
                    stack[++top] = next;
                    cursor[top] = graph.offset[next];
                }
            }
        }
        return finish;
    }

    private static final class Graph {
        final int[] offset, to, id;

        Graph(int n, int[][] edges, boolean reverse, boolean undirected) {
            offset = new int[n + 1];
            int arcs = edges.length * (undirected ? 2 : 1);
            to = new int[arcs];
            id = new int[arcs];
            for (int[] edge : edges) {
                offset[(reverse ? edge[1] : edge[0]) + 1]++;
                if (undirected) offset[edge[1] + 1]++;
            }
            for (int v = 1; v <= n; v++) offset[v] += offset[v - 1];
            int[] next = offset.clone();
            for (int i = 0; i < edges.length; i++) {
                int u = edges[i][reverse ? 1 : 0], v = edges[i][reverse ? 0 : 1];
                int p = next[u]++;
                to[p] = v; id[p] = i;
                if (undirected) {
                    p = next[v]++;
                    to[p] = u; id[p] = i;
                }
            }
        }
    }
}
