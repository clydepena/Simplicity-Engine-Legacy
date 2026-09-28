# How graphs are stored, connected, run and compiled

Status: **explanation and plan, not started.** Part of [node graphs for game objects](node-graphs.md). Related: [graph channels](graph-channels.md), [blackboards](blackboard.md), [asset pipeline](../asset-pipeline.md) (the codec that loads and compiles a graph).

**For a first state machine, most of this isn't needed:** states, transitions and action lists run directly with the tick loop in section 3. Compiling (section 4) matters for visual scripting and dataflow graphs, if they ever come.

---

## 1. A graph in code

**The saved form (the asset)** is nodes and links. A **link** connects one node's output pin to another node's input pin:
```java
record Pin(String name, PinKind kind, Class<?> type) {}   // kind: FLOW or DATA
record Node(int id, String kind, Map<String, Object> properties) {}   // kind = "Branch", "Add", "Door.Open"
record Link(int fromNode, String fromPin, int toNode, String toPin) {}

class GraphAsset {
    List<Node> nodes;
    List<Link> links;
}
```
```json
{ "nodes": [ { "id": 1, "kind": "OnDamaged" },
             { "id": 2, "kind": "Subtract" },
             { "id": 3, "kind": "GetVariable", "properties": { "name": "self.hp" } } ],
  "links": [ { "fromNode": 3, "fromPin": "value",  "toNode": 2, "toPin": "a" },
             { "fromNode": 1, "fromPin": "amount", "toNode": 2, "toPin": "b" } ] }
```

**Node kinds live in a registry, in code.** Each kind says which pins it has and what it does when run; the asset only stores the kind's *name*, as codecs are looked up by extension:
```java
interface NodeKind {
    List<Pin> inputs();
    List<Pin> outputs();
    void run(NodeRun run);   // read inputs, do the work, write outputs, pick the next flow pin
}
```

---

## 2. How connections work: the fan-in rules

Enforced when a wire is drawn:

| Pin | Links allowed | Why |
|---|---|---|
| **data input** | **at most 1** | a value must come from *one* place: `a` can't be both 5 and 7. Unconnected, it uses its default, typed in the node's field |
| **data output** | **many** | one value can feed many nodes (fan-out) |
| **flow output** | **at most 1** | "what happens next" must be unambiguous. To do two things, use a **Sequence** node (`then 0`, `then 1`) |
| **flow input** | **many** | several paths can lead to the same step; each arrival runs it |

Two nodes **can't** write into the *same* data input; combining two values takes a node that says *how* (`Add`, `Max`, `Select(condition, a, b)`). Two nodes feeding a node's **different** inputs is normal (the Subtract above gets `a` from one node and `b` from another), and there's no conflict, as section 3 shows.

---

## 3. How it runs: the order problem

"Several nodes feed one node; which runs first?" **A node runs only after everything it depends on.** That's a **dependency graph**, and the standard algorithm for turning it into one sequence is a **topological sort**. Each graph type uses it in its own way.

### Dataflow graphs (VFX, placement, materials): pull with memoization

To compute an output, compute its inputs first, recursively, remembering each result so shared nodes aren't computed twice:
```java
Object evaluate(int node, String outputPin, Map<Integer, Object[]> cache) {
    Object[] outs = cache.get(node);
    if (outs == null) {
        Object[] ins = new Object[inputCount(node)];
        for (int i = 0; i < ins.length; i++) {
            Link link = linkInto(node, i);                    // at most one, by the rules
            ins[i] = link == null ? defaultValue(node, i)
                                  : evaluate(link.fromNode(), link.fromPin(), cache);   // depends first
        }
        outs = kindOf(node).compute(ins);                     // then this node
        cache.put(node, outs);
    }
    return outs[pinIndex(node, outputPin)];
}
```
For the Subtract above: evaluate `self.hp`, then the event's `amount`, then subtract. Which input goes first doesn't matter, because these nodes have no side effects: they only compute. The sequence comes from the dependencies, not from how the wires look.

**Cycles are forbidden** (A needs B, B needs A); the editor refuses a wire that would create one (a depth-first search when connecting).

### Flow graphs (visual scripting, level wiring): follow the flow wires

An "instruction pointer" walks along flow links:
1. **An event fires:** start at its event node.
2. **Run the node:** first *pull* its data inputs (the dataflow evaluation above), then do its action.
3. **The node picks one flow output:** `Branch` picks `true` or `false`; a plain action has only `then`.
4. **Follow that output's single link** to the next node, and repeat until a flow output has no link.

Flow wires decide **when**; data wires decide **with what values**. A data input is computed at the moment its node runs, so `self.hp` is read fresh each time.

**Latent nodes** (`Wait 2 s`, `Move To`) **pause the walk**: the executor saves "continue at node X, pin `then`" and resumes when the wait is over, like a coroutine. Two events firing close together give two independent walks through the same graph.

### State machines: no sort needed

```
each tick:
  for each transition out of the current state (in priority order):
      if its condition is true (or its event arrived):
          run current state's On Exit actions
          current = transition.target
          run new state's On Enter actions
          break
  run current state's On Update actions
```

### Behaviour trees: a recursive walk from the root, each tick

- **Sequence:** run children left to right; stop at the first that fails or is still running.
- **Selector:** run children left to right; stop at the first that succeeds or is still running.
- Each node returns **success / failure / running**; a *running* leaf is remembered and resumed next tick.

---

## 4. Is it compiled?

It can be interpreted or compiled. **Most engines "compile" it, but not into machine code:** they turn the editor's form into a faster runtime form.

| Approach | What happens | Who does it |
|---|---|---|
| **Interpret the editor form** | walk nodes and look up links in the maps every time | simple; fine to start. Unity Visual Scripting is close to this |
| **Compile to a flat program, then interpret that** | resolve names to array indices once, pre-sort, give every pin a numbered slot, emit a list of instructions | **Unreal Blueprints** (compiled to bytecode run by a small virtual machine) |
| **Generate source code** | turn the graph into text for another compiler | **shader and material graphs** (graph -> GLSL/HLSL -> GPU compiler) |

For the damage example, a flat compiled program: every pin became a numbered slot, every node an instruction.
```
slots: s0 = amount (event), s1 = hp, s2 = hp - amount, s3 = died?
On Damaged:
  0: GET_VAR     self.hp       -> s1
  1: SUB         s1, s0        -> s2
  2: LESS_EQ     s2, 0         -> s3
  3: JUMP_IF_NOT s3            -> 6
  4: CALL        PlayAnimation("die")
  5: CALL        Destroy; END
  6: SET_VAR     self.hp       <- s2; END
```
The topological sort happened **once, when compiling**. At runtime there are no maps and no recursion, just stepping through an array: much faster when 200 guards each run a graph every frame.

**It fits the asset pipeline:**
- **The codec's `decode` reads the JSON and "compiles" it:** checks the rules (fan-in, types, no cycles), sorts, and builds the flat form. Pure data work, so it's safe on a worker thread.
- **The runtime component runs the compiled form.**
- **The editor keeps using the node/link form,** and saving writes that, never the compiled one.
