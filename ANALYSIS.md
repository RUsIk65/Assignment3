# Assignment 3: Graph Algorithms

## Implementation and interface

Language: Java, compatible with Java 17 (`javac --release 17`). The original DSU
constructor was retained and placed in the static nested class `Algorithms.DSU` in `src/Java/Algorithms.java`. The student's unfinished
menu and placeholder classes are preserved as text in `notes/`. Graph algorithms
are in `src/Java/Algorithms.java`. The official `Main`, `Input` and `Json` adapters
are unchanged. `submission.json` selects Java; other language directories remain
the official template stubs and are not used to run this solution.

Requests and responses use UTF-8 JSON Lines. `ping` returns `{"status":"ready"}`.
Only JSON answers are printed on stdout. Every request creates fresh arrays,
collections, DSU and graph objects. No mutable static algorithm state persists.
Edge IDs always refer to the original input index; sorting never mutates input rows.
Costs and distances use `long`/`Long`; distances of unreachable vertices use null.
Within the limits, even a simple path of 99999 edges of magnitude 10^9 fits in long.

## DSU

Initially parent[v]=v, size[v]=1, components=n, and largest is 0 for n=0 and 1
otherwise. `find` first walks to the root and then redirects every vertex on the
walk to that root. `union` finds both roots, returns false if they coincide, and
otherwise attaches the smaller tree below the larger tree, updates its size,
decrements components, and updates largest. A result pair is recorded after every
operation, including repetitions and self-unions.

Invariant: the parent forest partitions the vertices exactly according to all
connections processed so far. Only roots have meaningful component sizes; their
sizes count all vertices in their trees. Root links join exactly two distinct
components. Compression changes the representation without changing membership.
Induction over the operations proves each reported count and maximum size correct.
An empty graph produces no states. Iterative find avoids recursion.

Time: O(n+q alpha(n)) amortized. Memory including returned states: O(n+q).

## Kruskal

Sort original edge IDs by increasing weight, breaking ties by ID for determinism.
Maintain a DSU and an initially empty selected forest. Include an edge exactly when
its endpoints belong to different DSU sets; add its weight to a long accumulator.

Invariant: selected edges form an acyclic forest, DSU sets are its components, and
some minimum spanning forest contains all selected edges. For a selected edge,
consider the cut defined by one current component. No lighter crossing edge could
have been skipped while its endpoints remained in different sets. The cut property
(or exchanging a heavier crossing edge in an optimum forest) preserves optimality.
After all edges, each input component is spanned. Hence the returned forest is
minimum and has n-components edges. DSU counts isolated vertices as components.

Self-loops and already-connected parallel edges are rejected. Negative weights and
ties work with ordinary comparisons; an empty input returns cost=0, no IDs and
components=0. Comparator methods avoid subtraction overflow.

Time: O(n+m log(m+1)); DSU work is dominated by this bound. Memory: O(n+m),
including sorted IDs, DSU and selected IDs.

## Prim

Build undirected adjacency arrays, storing both incidences with their original ID.
Start at each unvisited vertex. Mark it visited and push incident edges leading to
unvisited vertices into a lazy minimum priority queue. Remove the lightest edge;
discard it if both endpoints are visited, otherwise select it and visit the new
endpoint. Drain the queue before starting the next component.

Invariant: selected edges form a tree on the vertices reached in the current
component; the heap contains every edge crossing from visited to unvisited, plus
possibly stale internal edges. Every non-stale minimum crosses this cut and is safe
by the cut property. Visiting a fresh vertex cannot create a cycle. An empty heap
means the entire component is exhausted; restarting accounts for all components
including isolated vertices. Applying the argument component by component proves
the resulting forest is optimal. This is a separate implementation from Kruskal.

Self-loops are never pushed because their endpoint is already visited. Parallel
edges compete normally; stale ones are discarded. Negative weights and empty
graphs are supported. Time: O(n+m log(m+1)) for the lazy heap. Memory: O(n+m).

## Kosaraju SCC and condensation

First perform DFS over every vertex of the original directed graph and record
vertices at finish time. Build the transposed adjacency structure. In decreasing
finish order, start an iterative traversal in the transpose at each unlabelled
vertex and give all vertices reached the same new label. Finally scan original
edges and add each distinct cross-label pair to the condensation.

The first DFS invariant is that an active vertex is finished only after all its
outgoing adjacency positions have been processed. In the condensation DAG, the
maximum finish time of a component exceeds that of each component reachable by a
cross edge. Thus the next unassigned component in reverse finish order cannot reach
another unassigned component in the transpose. It reaches all its own vertices
because they are mutually reachable. Each second-pass traversal labels exactly one
SCC. Marking on push prevents duplicate stack entries. Scanning all original edges
and hash-deduplicating their component pairs yields exactly the condensation edges.

Pairs are encoded injectively as `(long)a << 32 | (b & 0xffffffffL)`; no information
is lost. Loops/internal edges are excluded, duplicate cross edges are collapsed,
isolates get distinct labels, and n=0 gives two empty arrays.
Expected time with hashing: O(n+m). Memory: O(n+m).

## Three-state DFS topological ordering

The shared iterative DFS uses states unseen=0, active=1 and finished=2. Each stack
frame stores a vertex and its next adjacency cursor. A child advances the parent's
cursor before being pushed; a return resumes without rescanning previous neighbors.
Encountering an active target reports a cycle; finished targets are harmless.
When every component finishes, reverse the finish order.

Invariant: the active stack is a directed DFS path and each frame has processed
exactly the adjacency prefix preceding its cursor. A back edge closes a directed
cycle. Conversely a directed cycle cannot finish all its vertices without exposing
an active target, so a complete traversal without one certifies acyclicity. For
every edge of a DAG, its target finishes before its source; reversing finish order
makes every edge go forward. Check every vertex, including disconnected components.

A self-loop is immediately cyclic; duplicates and edges to finished vertices do not
give false positives. n=0 returns true and an empty order. Any cycle returns false
and an empty order. Time and memory: O(n+m). Explicit stacks of length n handle
100000-deep chains without using the Java call stack.

## DAG shortest paths and path reconstruction

Build directed adjacency and run the full-graph cycle check above. A cycle anywhere,
including outside source reachability, returns false and two empty arrays. Otherwise
initialize all distances to null, all parents to -1, and distance[source]=0. Visit
vertices in topological order; skip unreachable ones. Relax each outgoing edge when
it offers a strictly smaller distance and store the edge's original ID as parent.

Invariant: when a reachable vertex is processed, all its possible predecessors
have already been processed, so its distance is final. Induction over the order
proves each finite distance is the minimum of all source paths. Negative weights
do not invalidate this argument in a DAG. Each chosen parent ends at the vertex,
realizes its distance and comes earlier in topological order. Consequently following
parents must terminate at the source. Null distances and -1 parents identify
unreachable vertices; the source retains 0 and -1. Equal distances retain the first
parent selected, which is valid even for parallel edges.

`reconstructPath(source,target,edges,parentEdge)` is used with a successful DAG
result. It follows incoming parent IDs backward, appends source and reverses the
vertices. It returns [source] for source and [] for an unreachable target. Guard
checks reject malformed parent chains, including cycles; the helper is not a JSON
task. DAG time/memory: O(n+m). Reconstructing L vertices: O(L) time and output space.

Actual helper calls in `tests/java/AlgorithmChecks.java`, recorded in
`tests/path_results.txt`:

| Edges in original ID order | source,target | Vertices returned | Selected IDs | Cost |
|---|---|---|---|---|
| [0,1,3], [0,2,8], [1,2,-7], [2,3,4]; n=5 | 0,3 | [0,1,2,3] | [0,2,3] | 0 |
| Same | 0,0 | [0] | [] | 0 |
| Same | 0,4 | [] | [] | No route; distance=null |
| [0,1,2], [0,1,2], [1,2,-1], [0,2,1]; n=4 | 0,2 | [0,2] | [3] | 1 |
| [0,1,2], [0,1,2], [1,2,-1]; n=3 | 0,2 | [0,1,2] | [0,2] | 1 |

Each call checks the returned vertices, edge direction, weight sum and agreement
with distance. A separate 100000-vertex negative chain checks a full-length route
and final distance -99999000000000.

## Build, run and validation

All commands below run at the project root with Python 3.12+ and JDK 17+ on PATH:

```text
python tools/run.py --setup
python tools/run.py --check
python tools/run.py --input tests/demo.jsonl
python tests/run_tests.py --scale --benchmark
python -m unittest discover -s tools -p "test_*.py"
```

Manual PowerShell compilation and execution:

```powershell
New-Item -ItemType Directory -Force .build/Java | Out-Null
javac --release 17 -encoding UTF-8 -d .build/Java src/Java/*.java
java -Xmx384m -cp .build/Java Main
```

On the development computer the Oracle javapath shim did not finish launching;
the actual JDK was put first on PATH. The installed bundled Python was invoked by
its full executable path. Equivalent environment selection for reproducibility:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
$python = 'C:\Users\rusla\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe'
& $python tools/run.py --check
& $python tests/run_tests.py --scale --benchmark
```

Actual results on 2026-10-25:

- Setup ping passed; all 36 official cases passed.
- Two complete official batches in one process passed state-reset checks.
- 513 own cases passed; repeated twice in one process, 1026 answers checked.
- Semantic validators accepted original and alternative valid answers and rejected
  invalid witnesses for both forest methods, SCC, topo and DAG.
- Direct path tests passed, including the 100000-vertex route.
- Nine full-limit cases passed, each n=100000, m/q=250000, each within the launcher's
  20-second process timeout and -Xmx384m Java heap. Input generation and validation
  are outside that timeout. This is validation evidence, not a timing benchmark.
- Template tool tests: 16 discovered, 12 passed, 4 C++/C# tests skipped in a
  Java/Python-only PATH run. The first attempts hit sandbox temporary-directory
  permissions and a C# first-run permissions error. The successful run set TEMP
  and TMP to `.build/temp` and used PATH containing only the JDK, bundled Python
  and Windows directories. No tool or algorithm checks were weakened or edited.

`tests/README.md` explains independently justified expected answers: explicit sets
for DSU, exhaustive forest subsets, reachability closure for SCC/cycles, and
Bellman-Ford for distances. Small inputs, expected facts and actual outputs are
committed. Large inputs/results are generated deterministically in the ignored
`tests/generated/` directory; compact outcomes are in `tests/scale_results.txt`.
The supplied validator verifies edge IDs, feasibility and optimal costs, exact SCC
membership/condensation, precedence, and shortest parent paths without demanding a
particular non-unique ordering or labelling.

## Kruskal versus Prim: measured comparison

Environment: Windows 11 amd64; AMD Ryzen 7 5800H with Radeon Graphics, 16 logical
processors reported by Java; Oracle Java 21.0.11+9-LTS-211, HotSpot 64-Bit Server VM;
Python 3.12.14. JVM maximum heap: 402653184 bytes (384 MiB). CPU model was read
from the Windows processor registry. Physical RAM was unavailable through sandbox
CIM access and is not claimed.

Generator: `AlgorithmChecks.graph`, Java `Random`, seed=20261025+n. Generate a chain
of n-1 edges of weight -1000000000, then random endpoint pairs with weights from
0 through 1000000000 until m=2.5n, and Fisher-Yates shuffle all rows. Loops and
parallel edges may occur. Both algorithms receive the identical unchanged array.
The optimum is independently known: all n-1 chain edges cost -(n-1)*1000000000;
every alternative spanning tree must replace at least one of these minimum-weight
edges with a nonnegative edge. Before timing, verify each method's optimum cost,
component count and forest witness. Verify every warm-up and measured result too,
outside the timer.

For each size and method: one validation call, three additional warm-up calls, then
five measured calls. Alternate which algorithm runs first in each measured round.
Timer: `System.nanoTime()`, converted to milliseconds. Timed boundary is precisely
the typed `Algorithms.kruskal(n,edges)` / `Algorithms.prim(n,edges)` call, including
sorting or adjacency construction, DSU/heap allocations and result construction.
Exclude compilation, JVM startup, graph generation, JSON parsing, input conversion,
console/file output and validation. No forced garbage collection; collection during
a timed call counts. Run both methods in the same JVM.

Command: `python tests/run_tests.py --benchmark` (also included in the full command
above). Raw samples and runtime metadata are stored in `tests/benchmark.jsonl`.

| n | m | Seed | Method | Five samples (ms) | Median (ms) |
|---:|---:|---:|---|---|---:|
| 1000 | 2500 | 20262025 | Kruskal | 1.1221, 1.1534, 0.9924, 1.1234, 1.3716 | 1.1234 |
| 1000 | 2500 | 20262025 | Prim | 1.9387, 2.0891, 2.2586, 1.8248, 2.2021 | 2.0891 |
| 10000 | 25000 | 20271025 | Kruskal | 26.3373, 25.6609, 25.2895, 23.5343, 17.3278 | 25.2895 |
| 10000 | 25000 | 20271025 | Prim | 20.6225, 20.8915, 19.6426, 13.5677, 12.5367 | 19.6426 |
| 100000 | 250000 | 20361025 | Kruskal | 189.2045, 142.9211, 139.3366, 174.9945, 155.6155 | 155.6155 |
| 100000 | 250000 | 20361025 | Prim | 475.4736, 435.4529, 478.6934, 395.7317, 424.2883 | 435.4529 |

Observations: Kruskal was faster at the smallest and largest sizes; Prim was faster
at the middle size in this run. These results do not establish a universal ranking.
Both bounds are O(n+m log(m+1)) for this lazy-heap implementation. Different constants,
heap traffic, JIT compilation, allocation and collection can affect the samples.
Warm-up is limited and later JIT activity may remain; the benchmark has one JVM
process and only five samples per size, and the desktop was not an isolated host.
The planted optimum makes verification strong but is a biased graph family with
many equal minimum weights and a chain-shaped optimum; it does not represent all
densities or weight distributions. Asymptotic correctness follows from the analysis,
not these measurements.

## Sources and AI assistance

- Official assignment/template: https://github.com/RZhaniya/daa-assignment-3-graphs
  retrieved at commit `76c1d91daa90ceadcb225f5408d54d4bfcf397f9`. The adapters,
  public tools, other-language stubs, workflow and assignment statements originate
  from it. Algorithms follow its required methods and contracts.
- The student's initial code supplied the DSU fields and initialization; it is
  preserved in `notes/Main-original.java.txt`.

### AI tools

I used OpenAI Codex to help understand the assignment requirements, organize the
code structure, resolve minor syntax issues, and write README.md and ANALYSIS.md.
Codex also helped implement the six algorithms and the path-reconstruction helper,
create tests, and run correctness checks and performance benchmarks. The reported
results come from those automated runs.

Local completion does not imply GitHub Actions or Moodle submission. A personal
template repository, pushed commit, successful remote Public checks and the exact
commit URL submitted in Moodle are still required for delivery to the instructor.
