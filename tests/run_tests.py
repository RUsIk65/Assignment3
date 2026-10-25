"""Independent small-graph oracles, semantic witnesses, helper and full-limit tests."""
import argparse
import copy
import itertools
import json
from pathlib import Path
import random
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools'))
from run import build, execute
from public_validate import check


def closure(n, edges):
    reach = [[i == j for j in range(n)] for i in range(n)]
    for u, v, *_ in edges:
        reach[u][v] = True
    for k in range(n):
        for i in range(n):
            for j in range(n):
                reach[i][j] |= reach[i][k] and reach[k][j]
    return reach


def oracle(x):
    n, task = x['n'], x['task']
    if task == 'dsu':
        groups = [{v} for v in range(n)]
        states = []
        for u, v in x['operations']:
            a = next(g for g in groups if u in g)
            b = next(g for g in groups if v in g)
            if a is not b:
                a.update(b)
                groups.remove(b)
            states.append([len(groups), max(map(len, groups), default=0)])
        return {'states': states}
    edges = x['edges']
    if task in ('kruskal', 'prim'):
        reach = closure(n, edges + [[v, u, w] for u, v, w in edges])
        components = sum(not any(reach[i][j] for j in range(i)) for i in range(n))
        best = None
        # Enumerate ALL edge subsets of the required size, not another greedy MST.
        for ids in itertools.combinations(range(len(edges)), n - components):
            selected = [edges[i] for i in ids]
            connected = closure(n, selected + [[v, u, w] for u, v, w in selected])
            if connected == reach:
                cost = sum(edges[i][2] for i in ids)
                best = cost if best is None else min(best, cost)
        return {'cost': best, 'components': components}
    reach = closure(n, edges)
    if task == 'scc':
        labels = [min(j for j in range(n) if reach[i][j] and reach[j][i]) for i in range(n)]
        return {'component': labels}
    cyclic = any(reach[v][u] for u, v, *_ in edges)
    if task == 'topo':
        return {'acyclic': not cyclic}
    if cyclic:
        return {'acyclic': False}
    distance = [None] * n
    distance[x['source']] = 0
    # Bellman-Ford relaxation is independent of the implementation's DFS order.
    for _ in range(n - 1):
        for u, v, w in edges:
            if distance[u] is not None and (distance[v] is None or distance[u] + w < distance[v]):
                distance[v] = distance[u] + w
    return {'acyclic': True, 'distance': distance}


def small_cases():
    cases = []
    def add(task, n, rows, **extra):
        x = dict(task=task, n=n, **extra)
        x['operations' if task == 'dsu' else 'edges'] = rows
        cases.append(x)
    for task in ('dsu', 'kruskal', 'prim', 'scc', 'topo'):
        add(task, 0, [])
        add(task, 5, [])
    add('dag', 1, [], source=0)
    add('dsu', 5, [[0,0], [0,1], [0,1], [2,3], [1,3]])
    weighted = [[0,0,-1000000000], [0,1,-4], [0,1,-4], [1,2,2], [0,2,2]]
    for task in ('kruskal', 'prim'):
        add(task, 5, weighted)
        add(task, 5, [[i,i+1,1000000000] for i in range(4)])
    add('scc', 6, [[0,1], [1,0], [1,2], [0,2], [1,2], [2,3], [3,2], [4,4]])
    add('topo', 5, [[0,1], [0,2], [1,3], [2,3], [0,3]])
    add('topo', 5, [[0,1], [3,4], [4,3]])
    add('dag', 5, [[0,1,3], [0,2,8], [1,2,-7], [2,3,4]], source=0)
    add('dag', 4, [[0,1,2], [2,3,1], [3,2,1]], source=0)
    add('dag', 4, [[0,1,2], [0,1,2], [1,2,-1], [0,2,1]], source=0)
    add('dag', 5, [[i,i+1,-1000000000] for i in range(4)], source=0)
    rng = random.Random(20261025)
    for _ in range(70):
        n = rng.randrange(1, 7)
        edges = [[rng.randrange(n), rng.randrange(n), rng.randrange(-8, 9)]
                 for _ in range(rng.randrange(0, 10))]
        for task in ('kruskal', 'prim'):
            add(task, n, edges)
        for task in ('scc', 'topo'):
            add(task, n, [e[:2] for e in edges])
        add('dsu', n, [e[:2] for e in edges])
        add('dag', n, edges, source=rng.randrange(n))
        add('dag', n, [e for e in edges if e[0] < e[1]], source=rng.randrange(n))
    return cases


def witnesses():
    examples = [
        ({'task':'kruskal','n':3,'edges':[[0,1,1],[1,2,1],[0,2,1]]},
         {'cost':2,'components':1},
         {'cost':2,'components':1,'edge_ids':[0,1]},
         {'cost':2,'components':1,'edge_ids':[0,2]},
         {'cost':2,'components':1,'edge_ids':[0,0]}),
        ({'task':'scc','n':3,'edges':[[0,1],[1,0],[1,2]]}, {'component':[0,0,1]},
         {'component':[0,0,1],'condensation':[[0,1]]},
         {'component':[8,8,4],'condensation':[[8,4]]},
         {'component':[0,0,1],'condensation':[]}),
        ({'task':'topo','n':3,'edges':[[0,2],[1,2]]}, {'acyclic':True},
         {'acyclic':True,'order':[0,1,2]}, {'acyclic':True,'order':[1,0,2]},
         {'acyclic':True,'order':[2,0,1]}),
        ({'task':'dag','n':3,'source':0,'edges':[[0,1,1],[0,1,1],[1,2,1],[0,2,2]]},
         {'acyclic':True,'distance':[0,1,2]},
         {'acyclic':True,'distance':[0,1,2],'parent_edge':[-1,0,2]},
         {'acyclic':True,'distance':[0,1,2],'parent_edge':[-1,1,3]},
         {'acyclic':True,'distance':[0,1,2],'parent_edge':[-1,2,3]})]
    prim = copy.deepcopy(examples[0])
    prim[0]['task'] = 'prim'
    examples.append(prim)
    for x, expected, first, alternative, invalid in examples:
        assert check(x, first, expected) and check(x, alternative, expected)
        assert not check(x, invalid, expected)
    (ROOT/'tests/witness_cases.json').write_text(json.dumps(examples, indent=2), encoding='utf-8')
    print('PASS semantic validators: original + alternative accepted, invalid rejected (5 tasks)')


def scale_cases():
    n, m = 100000, 250000
    chain = [[i, i+1] for i in range(n-1)]
    rows = chain + [[0,n-1]] * (m-len(chain))
    yield {'task':'dsu','n':n,'operations':rows}, {'states':
        [[n-i-1,i+2] for i in range(n-1)] + [[1,n]]*(m-n+1)}
    weighted = [[u,v,1000000000] for u,v in rows]
    for task in ('kruskal','prim'):
        yield {'task':task,'n':n,'edges':weighted}, {'cost':99999000000000,'components':1}
    yield {'task':'scc','n':n,'edges':rows}, {'component':list(range(n))}
    cycle = chain + [[n-1,0]] + [[0,n-1]]*(m-n)
    yield {'task':'scc','n':n,'edges':cycle}, {'component':[0]*n}
    yield {'task':'topo','n':n,'edges':rows}, {'acyclic':True}
    yield {'task':'topo','n':n,'edges':cycle}, {'acyclic':False}
    negative = [[u,v,-1000000000] for u,v in rows]
    yield {'task':'dag','n':n,'source':0,'edges':negative}, {
        'acyclic':True,'distance':[-i*1000000000 for i in range(n)]}
    # Source zero is isolated; the deep cycle in the remaining vertices must still be found.
    unreachable = [[i,i+1,1] for i in range(1,n-1)] + [[n-1,1,1]]
    unreachable += [[1,2,1]]*(m-len(unreachable))
    yield {'task':'dag','n':n,'source':0,'edges':unreachable}, {'acyclic':False}


def jsonl(path, values):
    with path.open('w', encoding='utf-8') as stream:
        for value in values:
            stream.write(json.dumps(value, separators=(',', ':'))+'\n')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--scale', action='store_true')
    parser.add_argument('--benchmark', action='store_true')
    args = parser.parse_args()
    command = build('Java')
    cases = small_cases()
    expected = [oracle(x) for x in cases]
    actual = execute(command, cases + cases)
    for i, (x, e, a) in enumerate(zip(cases*2, expected*2, actual)):
        assert check(x, a, e), (i, x, e, a)
    jsonl(ROOT/'tests/cases.jsonl', cases)
    jsonl(ROOT/'tests/expected.jsonl', expected)
    jsonl(ROOT/'tests/actual.jsonl', actual[:len(cases)])
    print(f'PASS {len(cases)} independently checked cases, repeated twice in one process')
    witnesses()
    output = ROOT/'.build/Java'
    subprocess.run(['javac','--release','17','-encoding','UTF-8','-cp',str(output),'-d',str(output),
                    str(ROOT/'tests/java/AlgorithmChecks.java')], check=True, timeout=60)
    direct = ['java','-Xmx384m','-cp',str(output),'AlgorithmChecks']
    result = subprocess.run(direct, capture_output=True, text=True, check=True, timeout=20)
    (ROOT/'tests/path_results.txt').write_text(result.stdout, encoding='utf-8')
    print(result.stdout, end='')
    if args.scale:
        folder = ROOT/'tests/generated'
        folder.mkdir(exist_ok=True)
        summary = []
        for i, (x,e) in enumerate(scale_cases(), 1):
            a = execute(command, [x])[0]
            assert check(x,a,e), (i,x['task'])
            jsonl(folder/f'{i}_input.jsonl', [x])
            jsonl(folder/f'{i}_expected.jsonl', [e])
            jsonl(folder/f'{i}_actual.jsonl', [a])
            line = f'PASS full-limit {i}: {x["task"]}, n={x["n"]}, rows=250000'
            summary.append(line)
            print(line)
        (ROOT/'tests/scale_results.txt').write_text('\n'.join(summary)+'\n', encoding='utf-8')
    if args.benchmark:
        result = subprocess.run(direct+['benchmark'], capture_output=True, text=True, check=True, timeout=120)
        (ROOT/'tests/benchmark.jsonl').write_text(result.stdout, encoding='utf-8')
        print(result.stdout, end='')


if __name__ == '__main__':
    main()
