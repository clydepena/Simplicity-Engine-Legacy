# Node graphs for game objects

Status: **ideas and plan, not started.** Related: [graph execution](graph-execution.md) (storing, connecting, running and compiling graphs), [graph channels](graph-channels.md) (events, parameters, actions, blackboard), [blackboards](blackboard.md) (shared variables for graphs), [rendering extensibility](../rendering/extensibility.md) (render and shader graphs), [sound assets and editors](../sound/assets-and-editors.md) (sound node graphs), [asset pipeline](../asset-pipeline.md) (graphs are assets with a loader and a saver), [open world plan](../open-world-plan.md).

What a node editor can be used for around game objects, which uses matter most for a 2D open world, and how graphs fit the engine.

---

## 1. Two kinds of graph

- **Data graphs** describe a *structure*: states and their transitions, dialogue lines and their choices. A small runtime reads the graph and walks it. Easy to build, easy to debug, and they save naturally as assets.
- **Code graphs** describe *instructions*: "when X happens, do A, then B, if C". A visual programming language (Unreal's Blueprints). Very powerful, and the most work: debugging, speed, merging changes, and a node for every engine feature.

---

## 2. The uses

Roughly from most to least valuable for a 2D open world.

| Use | Kind | What the nodes are | Example | Engines that have it |
|---|---|---|---|---|
| **State machines** | data | states, with transitions guarded by conditions | enemy: Idle -> (sees player) -> Chase -> (in range) -> Attack | Unity Animator, Godot AnimationTree, Unreal state machines |
| **Animation graphs** | data | animation states and blends | walk in 4 or 8 directions: pick the sprite animation by movement angle; Walk -> (speed = 0) -> Idle | same as above |
| **Dialogue** | data | lines, choices, conditions ("has the key?"), effects ("give quest") | an NPC conversation that branches on what the player did | Yarn Spinner, Articy (Ink is text-based) |
| **Quests** | data | objectives, their prerequisites, branches | "find the sword" unlocks "slay the troll", or "sneak past" | many RPG toolkits |
| **Level event wiring** | data | **objects placed in the world** as nodes; their outputs connected to other objects' inputs | lever *pulled* -> door *open*; pressure plate *pressed* -> trap *fire* | Source engine's entity input/output (Hammer), Unreal's level Blueprint |
| **AI behaviour trees** | data (a tree) | selectors ("try until one succeeds"), sequences ("do in order"), conditions, actions | guard: if the player is visible, chase; else if a noise was heard, investigate; else patrol | Unreal behaviour trees; plugins for Unity and Godot |
| **Procedural placement** | data (dataflow) | noise -> biome rules -> scatter objects | generate a forest cell: trees by noise, rocks near water, never on paths | Houdini, Unreal's PCG graph |
| **Particle / VFX graphs** | data (dataflow) | emit -> move -> colour over life -> draw | a torch's flame, a spell hitting | Unity VFX Graph, Unreal Niagara |
| **Visual scripting** | code | events, actions, variables, flow wires | "OnInteract -> if has key -> open door, play sound" | Unreal Blueprints, Unity Visual Scripting |
| **Debug views** | read-only | objects and what references what | "which objects use this prefab / sprite sheet?" | reference viewers in most engines |

**Why the first few matter most here:**
- **State machines and animation graphs** are needed by almost every character (player, enemies, NPCs), and they're small to build.
- **Dialogue and quests** carry an open-world RPG, and writers can work in them without code.
- **Level event wiring** builds puzzles and interactions by connecting objects, instead of writing a class per door.
- **Procedural placement** matters specifically because the worlds are large and streamed ([open world plan](../open-world-plan.md)).

**About visual scripting:** Godot shipped visual scripting in 3.x and **removed it in 4.0**: few people used it, most preferred writing code, and maintaining a node for every engine feature cost too much. `Main.java` already lists "JS scripting" as a TODO; text scripting plus the data graphs above covers what visual scripting would do, for much less work.

---

## 3. Separate editors, one shared framework

Each use is its own **graph type**, with its own nodes and rules: dialogue nodes and particle nodes never share a graph. All types share the same canvas, pins, wires, saving and undo.

Big engines work the same way:
- **Unreal:** the Blueprint editor, the Material editor, the Behaviour Tree editor, Niagara (VFX), MetaSounds and Animation Blueprints are **separate editors**, built on one internal graph framework.
- **Blender:** shader nodes, geometry nodes and the compositor look identical, but each is its own *node tree type*; a shader node doesn't exist in the compositor.

**They can't share one graph because a wire means something different in each type:**

| Graph type | A wire means | When it runs |
|---|---|---|
| state machine | "you can move from this state to that one" | on a condition, one state active at a time |
| behaviour tree | "this is a child of that" (with order = priority) | re-checked every tick, top to bottom |
| dialogue | "then say this next" | one step at a time, waiting for the player |
| VFX / placement / material | "this value flows into that input" | computed like a formula |
| visual scripting | "then do this" (flow), plus values | when an event fires |

Mixing them would make every wire ambiguous: the runtime wouldn't know whether to walk, compute or wait.

### How graphs work together: references and shared data, not wires

- A state-machine state **Talk** *starts* a dialogue asset.
- A dialogue node *sets* a quest flag; a quest graph *reacts* to that flag.
- A level-wiring connection *sends* an event ("alarm") that a guard's state machine *uses* as a transition.
- A behaviour-tree action **Play animation** *drives* the animation graph.

The usual shared data is a **blackboard**: a small set of named values per object ("target", "alarmed", "hasKey") that every graph on that object can read and write. Graphs stay separate files, each testable on its own, and still cooperate.

---

## 4. The domains

Grouped by *what they control*:

| Domain | Graph types | What it answers | Mainly for |
|---|---|---|---|
| **Behaviour** | state machines, behaviour trees, (visual scripting) | "what does this object *do*?" | programmers, designers |
| **Presentation** | animation graphs, VFX graphs, material / shader graphs | "how does it *look* while doing it?" | artists |
| **Narrative** | dialogue, quests | "what's the *story*, and how does the player move through it?" | writers |
| **World** | level event wiring, procedural placement | "how is the *place* built and connected?" | level designers |
| **Tooling** | reference and debug views | "what's going *on*?" | everyone, read-only |

### The four runtime models

Grouped by *how they run*, there are only four:

| Model | Wires mean | Graph types |
|---|---|---|
| **Transitions** | "can go from here to there" | state machines, animation graphs |
| **Tree** | "child of, in priority order" | behaviour trees |
| **Flow / steps** | "then this happens" | dialogue, quests, level wiring, visual scripting |
| **Dataflow** | "this value feeds that input" | VFX, procedural placement, materials, render graphs, sound graphs |

**This is the table that matters for building them:** not ten runtimes, only four executors. Each graph type is a *palette of nodes* plus one of those executors: dialogue, quests and level wiring can share the flow executor; VFX, placement and materials the dataflow executor. With the shared editor framework, a new graph type is then mostly defining its nodes.

---

## 5. How graphs fit the engine

The same pattern as the sound and asset work:
- **The graph is an asset: the definition, shared.** E.g. `guard.fsm` or `blacksmith.dialogue` as JSON, with a codec that loads *and* saves, like the `.sheet` files.
- **A component holds the runtime state: one per game object.** Which state this guard is in, its timers, its variables. It holds the graph as an `Asset<StateMachineGraph>` field, saved as a path through the existing asset type adapter.
- The same split as **clip vs source** in sound: 50 guards share one graph, and each has its own current state.

---

## 6. How a graph knows which object it's changing

**The graph asset doesn't know, and shouldn't.** It's a template, like a sound clip or a shader: `guard.fsm` is shared by all 50 guards, so it can't point at any one of them. The object is supplied when the graph **runs**.

**Binding happens through the component.** A `StateMachineComponent` on the guard creates a *graph instance*: the runtime state, plus a **context** saying who it belongs to:
```java
class StateMachineComponent extends Component {
    Asset<StateMachineGraph> graph;          // the shared template (saved as a path)
    private StateMachineInstance instance;   // this guard's own: current state, timers

    public void start() {
        instance = graph.get().instantiate(new GraphContext(gameObject, blackboard));
    }
    public void update(float dt) {
        instance.tick(dt);                   // nodes act through the context
    }
}
```

**Nodes are written against the context, never against a specific object.** "Play animation 'walk'" means *the owner's* animator: `context.owner().getComponent(Animator.class)`. The same idea as `this` in Java, or a shader acting on whichever sprite it's drawing.

**Three ways a graph refers to objects:**

| Kind | Meaning | Set when | Example |
|---|---|---|---|
| **Self** (implicit) | the object the graph runs on | when the component starts | "play *my* walk animation", "move *me*" |
| **Blackboard slots** | named values that change while playing | at runtime, by other code or nodes | `target` = whoever the guard saw; perception writes it, "Chase target" reads it |
| **Exposed parameters** | values the graph declares, filled in **per object in the inspector** | in the editor, saved with the object | this guard's `patrolRoute` = these 4 waypoints; that one's = 3 others |

Exposed parameters make one graph reusable, like a Unity script's serialized fields: the graph says "I need a patrol route", and every guard placed in the world gets its own.

**References between placed objects need persistent ids.** Level wiring saves links like "this lever -> that door". The link must survive saving, loading and cell streaming, so it's stored as the door's **id**, not a Java reference. The engine doesn't have stable ids yet: [open world plan](../open-world-plan.md) §1 notes that `GameObjectDeserializer` ignores the saved uid, and plans a "persistent uid". **Level wiring depends on that being done first.** State machines, behaviour trees and dialogue don't: they only use *self* and the blackboard.

---

## 7. What drives graphs, and how they trigger each other

**No graph drives the loop. The engine's game loop does,** and graphs are passive: they only run when something calls them. The engine's chain today:
```
Application.run()                  -- the loop, once per frame
  └─ layer.onUpdate(dt)
      └─ World2DLayer.onUpdate(dt)
          ├─ physics2d.update(dt)
          └─ for each GameObject: go.update(dt)
              └─ for each Component: c.update(dt)
                  └─ StateMachineComponent: instance.tick(dt)   <- the graph runs here
```
A graph component is just another component; the loop reaches it as it reaches a `Rigidbody2D`.

**Graphs run in two ways:**

| How | Graph types | Who calls them |
|---|---|---|
| **Ticked every frame** | state machines, behaviour trees, animation graphs, VFX | their component's `update(dt)` |
| **Woken by events** | dialogue, quests, level wiring | an event: "player interacted", "lever pulled", "item picked up". They sleep until then |

So when one graph "triggers" another, it sends an event, or writes a value the other reads on its next tick:
```
player presses E near the NPC
  -> InteractionEvent (the engine's event system)
  -> the NPC's state machine: transition to "Talk"
  -> state "Talk" starts blacksmith.dialogue            (a command: start this asset)
  -> dialogue choice "I'll help" sets flag quest.sword.accepted
  -> QuestEvent -> the quest graph advances to "find the sword"
```

**Not every graph belongs to an object:** quests, world state and weather live on a world-level host (a global object or a world component), ticked or woken by the same loop.

**Order within a frame matters.** Engines split the frame into **phases**, so graphs see a consistent world:
1. input and events are processed (`EventSystem.processEvents()` already runs first)
2. **decide:** behaviour graphs (state machines, behaviour trees) read the blackboard and choose actions
3. **physics** moves things
4. **present:** animation and VFX graphs react to what happened
5. render

`World2DLayer.onUpdate` runs physics *before* the objects' `update`, and all components update in one pass, so behaviour-before-physics doesn't exist yet. It becomes a design point once graphs exist: e.g. an `update` phase and a `lateUpdate` phase for components.

---

## 8. Hosts: graphs beyond game objects

### A game object isn't necessarily a thing in the world

"Game object" is software language, not physical language. A `GameObject` is **a container with an identity**: it has a uid and a name, holds components, and the engine updates, saves and lists it. What it *is* comes entirely from its components:

| Components | What you'd call it | Visible? | Uses its position? |
|---|---|---|---|
| sprite + collider + state machine | a guard | yes | yes |
| sprite only | a rock | yes | yes |
| trigger collider + level wiring | a pressure plate zone | no (editor gizmo only) | yes |
| spawner | an enemy spawn point | no (editor icon only) | yes |
| sound source | a river's sound | no | yes |
| state machine + screen effects | the weather | no | no |
| state machine | the game's rules / score keeper | no | no |

Unity calls them GameObjects too ("Create Empty" gives one with only a transform, for managers); Unreal calls them Actors, and some built-in ones are pure data holders (`WorldSettings`, `GameState`); Godot calls them **Nodes**, which avoids the confusion. "Entity" (the common term in ECS engines) would describe it better.

### Graphs on other hosts

A graph type isn't tied to game objects. **What it runs on is decided by its host,** the thing that owns the graph instance and ticks it. A game object is just the most common host; the same graph *types* work at every level:

| Host | State machine | Behaviour tree | Dialogue | Other |
|---|---|---|---|---|
| **Game object** | guard: Idle -> Chase -> Attack | enemy AI | an NPC's conversation | its animation graph, VFX |
| **World / cell** | day -> dusk -> night; weather: clear -> rain -> storm | a *director* that spawns enemies based on tension (Left 4 Dead's "AI Director") | — | level wiring, quest graphs |
| **The game session** | game flow: MainMenu -> Loading -> Playing -> Paused -> GameOver | — | a narrator, radio messages | global quest progress |
| **UI** | screen flow: Inventory <-> Map <-> Crafting | — | the dialogue box *showing* a conversation | UI animations |

A state machine is a general tool for "something with modes": a guard, the weather, or the whole game. Big engines split it the same way:
- **Unreal:** Blueprints on Actors (objects), plus a **Level Blueprint** (world) and GameMode/GameState (the session).
- **Godot:** scripts on nodes, plus **autoloads**: global nodes that live for the whole game.
- **Unity:** components on GameObjects, plus "manager" objects kept alive between scenes.

### The context gets a host, not a game object

`GraphContext(gameObject, blackboard)` from section 6 becomes:
```java
interface GraphHost {
    Blackboard blackboard();             // every host has variables
    void subscribe(EventType e, ...);    // and can be woken by events
    GameObject asGameObject();           // null for world, session and UI hosts
}
```
- **Nodes declare what host they need:** "Play animation" needs a game object with an animator; "Change music" or "Set flag" works on any host.
- **The editor can warn** when a graph uses an object-only node but is placed on the world.

### Blackboards come in scopes

What a blackboard is, and where its variables are declared: [blackboards](blackboard.md).

| Scope | Example | Lives as long as | Saved in |
|---|---|---|---|
| `self.` | `self.hp`, `self.target` | the object | the object's cell (or not at all, see below) |
| `world.` | `world.timeOfDay`, `world.alarmRaised` | the loaded world | the world's global data |
| `game.` | `game.metBlacksmith`, `game.quest.sword.stage` | the whole playthrough | **the save game** |

Story flags like "met the blacksmith" must survive changing worlds, quitting and loading: they live in `game.`, not on the NPC.

### Object graphs and streaming

A guard's state machine runs while its cell is loaded. When the cell unloads, the guard either:
- **forgets:** it restarts at its entry state next time. Fine for most ambient NPCs and enemies.
- **or has its instance state saved with the cell:** current state, timers, `self.` variables. Needed for anything the player would notice: a door left open, an NPC mid-errand.

World and session hosts aren't in a cell, so they don't have this problem: another reason long-running logic (quests, day/night, the director) belongs on them, not on an object that might be unloaded.

### Where the hosts live in this engine

- **Game-object host:** a component, as in section 6.
- **World host:** a global object in the world (the [open world plan](../open-world-plan.md) already separates global objects from spatial, cell-bound ones), or a small world-level component; ticked by `World2DLayer`.
- **Session host:** something above any world, e.g. owned by `Application` or its own layer, so it keeps running across world changes.
- **UI host:** the editor layer and panels today; a game UI layer later.

---

## 9. What already exists

`editor/nodes/Graph.java` plus `editor/panels/NodeEditorPanel.java` is a **demo-level start** (about 300 lines):
- **It has:** nodes with *numbers* of input and output pins, wires, and the imgui node-editor extension to draw them.
- **It doesn't have:**
  - **pin types:** an "enemy" output shouldn't connect to a "number" input
  - **node kinds with settings:** "state Chase: speed 3"
  - **saving and loading**
  - **undo**
  - **a runtime that executes a graph**

---

## 10. Order of work

1. **One reusable graph foundation first:** typed pins, node kinds with properties, save and load through a codec, undo. Every graph type above needs exactly this.
   - Plus the runtime side: a graph instance with its context (owner, blackboard, exposed parameters), and update phases for components (section 7).
2. **Then graph types one at a time,** each with its own node kinds and runtime component:
   1. state machine (it also serves animation)
   2. dialogue
   3. level event wiring (needs persistent object ids first, section 6)
   4. behaviour trees (when AI needs more than a state machine)
   5. procedural placement (when building world generation)
3. **Visual scripting last, and probably not at all,** in favour of text scripting.
