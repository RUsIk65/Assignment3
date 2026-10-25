import java.util.*;

public final class AlgorithmChecks {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void route(int n, int[][] edges, int source, int target,
                              List<Integer> expected, long expectedCost) {
        Map<String, Object> result = Algorithms.dag(n, edges, source);
        int[] parents = (int[]) result.get("parent_edge");
        List<Integer> path = Algorithms.reconstructPath(source, target, edges, parents);
        require(path.equals(expected), "Incorrect reconstructed path: " + path);
        List<Integer> ids = new ArrayList<>();
        long cost = 0;
        for (int i = 1; i < path.size(); i++) {
            int id = parents[path.get(i)];
            require(edges[id][0] == path.get(i - 1) && edges[id][1] == path.get(i), "Direction");
            ids.add(id);
            cost += edges[id][2];
        }
        require(cost == expectedCost, "Route cost");
        Long distance = ((Long[]) result.get("distance"))[target];
        require(path.isEmpty() ? distance == null : distance != null && distance == cost, "Distance");
        System.out.println(Json.write(Map.of("source", source, "target", target,
                "path", path, "edge_ids", ids, "path_cost", cost)));
    }

    private static void paths() {
        int[][] edges = {{0,1,3}, {0,2,8}, {1,2,-7}, {2,3,4}};
        route(5, edges, 0, 3, List.of(0,1,2,3), 0);
        route(5, edges, 0, 0, List.of(0), 0);
        route(5, edges, 0, 4, List.of(), 0);
        route(4, new int[][]{{0,1,2}, {0,1,2}, {1,2,-1}, {0,2,1}},
                0, 2, List.of(0,2), 1);

        route(3, new int[][]{{0,1,2}, {0,1,2}, {1,2,-1}},
                0, 2, List.of(0,1,2), 1);
        int n = 100000;
        int[][] chain = new int[n - 1][3];
        for (int i = 0; i < n - 1; i++) chain[i] = new int[]{i, i + 1, -1000000000};
        Map<String, Object> result = Algorithms.dag(n, chain, 0);
        List<Integer> path = Algorithms.reconstructPath(0, n - 1, chain, (int[]) result.get("parent_edge"));
        require(path.size() == n && path.get(0) == 0 && path.get(n - 1) == n - 1, "Deep path");
        require(((Long[]) result.get("distance"))[n - 1] == -99999000000000L, "Long distance");
        System.out.println("PASS route helper, including 100000-vertex path");
    }

    private static int[][] graph(int n, int m, long seed) {
        Random random = new Random(seed);
        int[][] edges = new int[m][3];
        for (int i = 0; i < n - 1; i++) edges[i] = new int[]{i, i + 1, -1000000000};
        for (int i = n - 1; i < m; i++) {
            edges[i] = new int[]{random.nextInt(n), random.nextInt(n), random.nextInt(1000000001)};
        }

        for (int i = m - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int[] temp = edges[i]; edges[i] = edges[j]; edges[j] = temp;
        }
        return edges;
    }

    private static void verify(int n, int[][] edges, Map<String, Object> result) {
        require(((Number) result.get("cost")).longValue() == -(n - 1L) * 1000000000L, "Known optimum");
        require(((Number) result.get("components")).intValue() == 1, "Connected graph");
        List<?> ids = (List<?>) result.get("edge_ids");
        require(ids.size() == n - 1, "Forest size");
        Set<Integer> unique = new HashSet<>();
        Algorithms.DSU sets = new Algorithms.DSU(n);
        long cost = 0;
        for (Object value : ids) {
            int id = ((Number) value).intValue();
            require(id >= 0 && id < edges.length && unique.add(id), "Unique valid IDs");
            require(sets.union(edges[id][0], edges[id][1]), "Acyclic forest");
            cost += edges[id][2];
        }
        require(sets.getComponents() == 1 && cost == -(n - 1L) * 1000000000L, "Forest witness");
    }

    private static Map<String, Object> run(String method, int n, int[][] edges) {
        return method.equals("kruskal") ? Algorithms.kruskal(n, edges) : Algorithms.prim(n, edges);
    }

    private static void benchmark() {
        System.out.println(Json.write(Map.of("java", System.getProperty("java.runtime.version"),
                "vm", System.getProperty("java.vm.name"), "os", System.getProperty("os.name"),
                "arch", System.getProperty("os.arch"), "processors", Runtime.getRuntime().availableProcessors(),
                "max_heap_bytes", Runtime.getRuntime().maxMemory())));
        for (int n : new int[]{1000, 10000, 100000}) {
            int m = n * 5 / 2;
            long seed = 20261025L + n;
            int[][] edges = graph(n, m, seed);
            String[] names = {"kruskal", "prim"};
            for (String name : names) {
                verify(n, edges, run(name, n, edges));
                for (int i = 0; i < 3; i++) verify(n, edges, run(name, n, edges));
            }
            double[][] samples = new double[2][5];
            for (int round = 0; round < 5; round++) {
                for (int position = 0; position < 2; position++) {
                    int method = (round + position) % 2;
                    long start = System.nanoTime();
                    Map<String, Object> result = run(names[method], n, edges);
                    samples[method][round] = (System.nanoTime() - start) / 1_000_000.0;
                    verify(n, edges, result);
                }
            }
            for (int method = 0; method < 2; method++) {
                double[] sorted = samples[method].clone();
                Arrays.sort(sorted);
                System.out.println(Json.write(Map.of("n", n, "m", m, "seed", seed,
                        "method", names[method], "milliseconds", samples[method], "median_ms", sorted[2])));
            }
        }
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("benchmark")) benchmark();
        else paths();
    }
}
