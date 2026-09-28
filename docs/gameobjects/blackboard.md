# Blackboards: shared variables for graphs

Status: **idea and plan, not started.** Part of [node graphs for game objects](node-graphs.md) (hosts and scopes are in its section 8). Related: [asset pipeline](../asset-pipeline.md) (the variables list is an asset with a loader and a saver).

What a blackboard is, why graphs need one, and where variables like `world.rainIntensity` come from.

---

## 1. What a blackboard is

A **shared table of named values** that different parts of the game read and write, **without knowing about each other**:
```
Weather blackboard (world scope)
  rainIntensity   Float     0.7
  weather         Weather   RAIN
  wetness         Float     0.4
```
The name comes from AI research's "blackboard architecture", and the picture is a classroom blackboard: anyone can write on it, anyone can read it, and the writer doesn't need to know who reads. The weather graph writes `rainIntensity`; the rain particles, the rain sound and the campfires read it. None of them has a reference to any other.

**Why graphs need one, rather than normal Java fields:**
- **Graphs are data, not code.** A node in `weather.fsm` can't write `particleEmitter.rate = 0.7`: the graph doesn't know about Java classes or fields. A blackboard gives graphs one generic way to share values: *by name*.
- **It decouples.** A new effect that reacts to rain only reads the value; the weather graph doesn't change.
- **The editor can show it.** Every variable is listed with its type, so node pins get dropdowns ("pick a variable") instead of hand-typed names, and the debugger can show live values while playing.

**In code, it's small:**
```java
public final class Blackboard {
    private final Map<String, Object> values = new HashMap<>();

    public <T> T get(Key<T> key)            { return key.type().cast(values.getOrDefault(key.name(), key.defaultValue())); }
    public <T> void set(Key<T> key, T value) { values.put(key.name(), value); }   // later: notify listeners of the change
}

// a typed key, so reading a Float can't return a String
record Key<T>(String name, Class<T> type, T defaultValue) {}
```
Unreal's behaviour trees use exactly this: each AI has a **Blackboard asset** that declares its keys and their types. Unity's Visual Scripting has the same idea under the name *Variables*.

---

## 2. Where the variables come from

Names like `world.rainIntensity` and `world.season` in these docs are **examples**: nothing defines them, and the engine wouldn't either.
- **The engine provides the mechanism:** blackboards, their scopes (`self.`, `world.`, `game.`), saving them, and editor support.
- **The game defines the variables:** which ones exist, their types, their defaults. A farming game has `world.season`, a horror game `world.dread`, a racing game neither.

They're the game's vocabulary, fully customizable.

### A declared list per scope, not free-form names

Each scope has a variables asset, edited as a table:
```
world.vars                                         (a data asset: loaded and saved, like .sheet)
  Name            Type      Default   Saved   Notes
  timeOfDay       Float     8.0       yes     hours, 0..24
  season          Season    SPRING    yes     enum: SPRING, SUMMER, AUTUMN, WINTER
  weather         Weather   CLEAR     yes
  rainIntensity   Float     0.0       no      eased by the weather graph
  alarmRaised     Bool      false     no
```
Why declare them instead of letting any graph write any name:
- **Typos:** with free-form names, writing `rainIntesity` silently creates a new variable and the rain never appears. With a declared list, the editor refuses unknown names.
- **Types:** a graph pin reading `season` knows it's a `Season`, so wires and dropdowns can be checked.
- **Defaults:** a new game starts from known values.
- **Saving:** some values belong in the save file (`season`), some are recalculated anyway (`rainIntensity`); the "Saved" column decides.
- **Renaming:** rename `rainIntensity` once in the table, and the editor updates every graph that uses it.

### Scopes, compared with Unity

| Unity Visual Scripting scope | Here | Lives as long as |
|---|---|---|
| Graph / Object | `self.` (per object; declared by the object's graph or a per-object variables list) | the object |
| Scene | `world.` | the loaded world |
| Application / Saved | `game.` | the playthrough, stored in the save file |

### Built-in, read-only values

The engine can add a few values only it knows, e.g. `world.time` (seconds since the world started) or `self.position`. Graphs read them like any other variable, but they're filled in by the engine, not declared by the game.

---

## 3. How graphs use the blackboard

The blackboard is one of four ways a graph connects to the game, next to events, exposed parameters and actions: see [graph channels](graph-channels.md), including when to use the blackboard and when an event.

**Reading and writing values:** `Get world.weather` and `Set self.alarmed = true` nodes. The variable is picked from a dropdown of the declared list, so a typo is impossible.

**Conditions:** where blackboards are used most.
- **State machine transitions:** `Patrol -> Chase` when `self.target != none`.
- **Behaviour tree checks:** a "Blackboard condition" node, e.g. "run this branch only while `self.target` is set". Unreal's behaviour trees add **observer aborts**: when the value changes, the tree re-decides *immediately* instead of finishing its current action. A guard chasing someone stops the moment `self.target` is cleared.

**Waking on change:** an event node **`On Variable Changed(world.weather)`**. The blackboard notifies listeners when a value changes, so event-driven graphs (dialogue, quests) react without checking every frame. That's the "notify listeners" comment in the `Blackboard` sketch.

**Services that keep it up to date:** something has to *write* most values, usually a component or a small repeating node, not the graph that reads it:
```
PerceptionComponent (every 0.2 s)   -> writes self.target = nearest visible player, or none
guard.bt                            -> reads self.target, decides chase / patrol
guard animation graph               -> reads self.speed, picks walk / idle
```
The perception code, the AI and the animation never reference each other: they only share `self.target` and `self.speed`.

---

## 4. Keeping it manageable

- **One writer per variable.** If two graphs both write `world.weather`, they fight and the value flickers. Each variable has one owner: the weather graph owns the weather keys; everyone else reads. The editor can show **"written by / read by"** per variable, which also answers "what breaks if I rename this?".
- **Local variables for scratch work.** A counter or timer used inside one graph doesn't belong on a shared blackboard: it's a **local** variable of that graph instance (Unity's "Graph" scope), invisible to everyone else. So there are four scopes: `local`, `self.`, `world.`, `game.`
- **Graphs declare what they need.** `guard.bt` says "I need `self.target: GameObject` and `self.alarmed: Bool`"; when the component starts, it makes sure the object's blackboard has those slots. Two graphs on one object asking for the same name *and* type share it; the same name with different types is an error the editor reports. That's how Unreal links a behaviour tree to its Blackboard asset.

---

## 5. A complete example: the alarm

```
lever (level wiring)      On Pulled   -> send event Alarm, set world.alarmRaised = true
guard.bt (every guard)    condition "world.alarmRaised" (observer abort) -> switch to "Search" branch
guard.bt                  Search branch reads self.lastKnownPlayerPos (written by perception)
alarm_bell (state mach.)  transition Idle -> Ringing when world.alarmRaised; plays the bell sound
game rules (session)      On Variable Changed(world.alarmRaised) -> game.timesCaught += 1 (saved)
```
One lever pull and five independent reactions, none referencing each other. Guards whose cell loads *later* still search, because they read the state.

---

## 6. In the graph editor

### The variables panel

Every graph editor gets a side panel listing everything the graph can see, grouped by scope:
```
┌ Variables ─────────────────┐ ┌ guard.bt ───────────────────────────────────┐
│ Local                      │ │                                             │
│   searchTimer    Float     │ │   ( self.target )──▶[ Is set? ]──▶ Chase     │
│ Self                       │ │                                             │
│   target         GameObject│ │   [ Set world.alarmRaised ← true ]           │
│   alarmed        Bool      │ │                                             │
│ World                      │ └─────────────────────────────────────────────┘
│   alarmRaised    Bool      │
│   weather        Weather   │
│ Game                       │
│   timesCaught    Int       │
│ Parameters  (per object)   │
│   patrolRoute    Path  👁  │
│ + New variable             │
└────────────────────────────┘
```
- **Drag a variable onto the canvas** -> a **Get** node: a small "pill" with one output pin carrying its value.
- **Drag with a modifier key** (e.g. Alt) -> a **Set** node, which writes a value when the flow reaches it.
- **"+ New variable"** adds to the declared list: local to this graph, or `self.`/`world.`/`game.` in the shared variables asset.
- **Parameters** (the eye icon) are the *exposed* ones, shown in the inspector of every object using the graph.

Other engines have exactly this panel:
- **Unity Visual Scripting** calls it the **Blackboard**, with tabs per scope.
- **Unity Shader Graph** also calls its panel the **Blackboard**; there it lists the *exposed properties* (the material parameters an artist sets per material): the same idea as "Parameters" here.
- **Unreal Blueprints** have the **My Blueprint** panel; dragging a variable onto the graph asks "Get or Set?", and right-clicking any pin gives **"Promote to variable"**, which creates a variable from a value already wired.

### How variables appear, by graph type

Graphs whose wires mean something other than "a value flows here" show a variable as a **field with a dropdown** on a node or transition instead of a Get/Set node:

| Graph type | How variables appear | Example |
|---|---|---|
| **Visual scripting, dataflow** (VFX, placement, materials) | **Get / Set nodes** wired into other nodes | `(world.rainIntensity) -> [× 200] -> Spawn rate` |
| **State machine** | a **condition on the transition**, edited in the transition's inspector | `Patrol -> Chase` when `self.target` **is set** |
| **Behaviour tree** | **condition and action nodes with a variable field** (Unreal's "blackboard key selector") | `[Blackboard: self.target is set]` above the Chase branch |
| **Dialogue / quests** | **condition and effect fields** on a line or choice | choice "Pay 10 gold" shown if `game.gold >= 10`, effect `game.gold -= 10` |

Plus one **event node** in every event-driven graph: **`On Variable Changed (world.weather)`**, which wakes the graph when that value changes.

All of these read the **same declared list**: the dropdown in a state machine's transition and the drag-out list in a VFX graph show identical variables. That's why declaring them once pays off.

### What it needs underneath

From the graph foundation (step 1 in the [node graphs order of work](node-graphs.md)):
- **Typed pins:** a `Bool` Get node only connects to a `Bool` input.
- **Node kinds with property fields:** the variable dropdown on a behaviour-tree node is just a property.
- **The declared variables lists:** the `.vars` assets (section 2), so the panel and dropdowns have something to list.
