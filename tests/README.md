# Reproducible tests

Run from the project root with Python 3.12+ and JDK 17+ on PATH:

```text
python tools/run.py --setup
python tools/run.py --check
python tools/run.py --input tests/demo.jsonl
python tests/run_tests.py --scale --benchmark
```

`run_tests.py` compiles Java, creates 513 deterministic cases (seed 20261025),
computes independent expected facts and submits the entire batch twice to one
Java process. It saves `cases.jsonl`, `expected.jsonl`, and `actual.jsonl` in matching
line order. Expected files contain semantic facts, not always complete responses:
the supplied validator checks the returned witnesses against these facts.

Expected-answer justification:

- DSU: explicit Python sets represent each partition block; no parent trees.
- Forests: enumerate every subset of n-c edges for small graphs. Compare its
  undirected reachability to the whole input; take the smallest feasible weight.
  A connected spanning subgraph with exactly n-c edges is a forest.
- SCC: Floyd-Warshall Boolean closure; labels identify mutual-reachability classes.
- Cycles: an edge u→v belongs to a cycle iff v reaches u (including self-loops).
- DAG distances: Bellman-Ford over all edges for n-1 rounds, independent of DFS.

Hand-picked cases cover empty/isolated graphs, repeated/self unions, negative loops,
equal parallel edges, disconnected graphs, sums beyond 32 bits, unreachable cycles,
and tied paths. Seeded small random cases additionally compare every algorithm to
the independent oracle. `witness_cases.json` records original and alternative valid
answers plus an invalid witness for Kruskal, Prim, SCC, topo and DAG. Each is checked.

`java/AlgorithmChecks.java` calls the route helper directly, checks vertices,
edge directions and costs, tests ties/parallel edges and a 100000-vertex route.
Actual results are in `path_results.txt`. For an unreachable route, `path_cost:0`
is merely the empty edge sum; the shortest-path distance remains null.

The `--scale` option generates nine cases, each n=100000 and q/m=250000:

1. DSU chain followed by repeated unions: component counts descend to one;
   largest sizes increase to n and remain n.
2. Kruskal and Prim: positive-weight connected graph; every spanning tree costs
   (n-1)*1000000000 = 99999000000000.
3. SCC: forward chain plus repeated forward edge gives singleton SCCs; a return
   edge from the last vertex to the first makes all vertices one SCC.
4. Topo: the same acyclic and cyclic directed graphs.
5. DAG: negative chain and repeated shortcut; distance[i]=-i*1000000000 since the
   full chain is the cheapest route. A second graph isolates source zero but
   contains a deep cycle elsewhere and must be rejected.

Exact generated inputs, expected facts and actual answers are stored locally under
`generated/`. They are excluded from Git because they are large; the deterministic
generator is committed instead. `scale_results.txt` is the compact actual record.
Each large case has the official 20-second subprocess timeout and 384 MiB Java heap.

`benchmark.jsonl` stores environment and five raw samples plus median for each
method/size; full methodology and interpretation are in `../ANALYSIS.md`.
`tool_results.txt` records the template-tool tests run with Java/Python only;
four C++/C# tests were skipped because those compilers were excluded from that run.
