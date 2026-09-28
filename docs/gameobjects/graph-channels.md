# How graphs talk to the game: the four channels

Status: **idea and plan, not started.** Part of [node graphs for game objects](node-graphs.md). The blackboard channel in detail: [blackboards](blackboard.md).

A graph instance connects to the rest of the game through four separate channels. The blackboard is only one of them: it's shared state *around* the graph, not the graph's entry or exit.

---

## 1. The four channels

```
                 exposed parameters  (per object, set in the inspector: patrolRoute, speed)
                          │
 events ─────────▶ [ graph instance ] ─────────▶ actions   (play animation, move, start dialogue)
 (player interacted,      │   ▲                ─────────▶ events it sends (alarm raised)
  lever pulled)           ▼   │
                 blackboards: read and write   (self. / world. / game.)  + local variables
```

| Channel | What it is | Direction | Example |
|---|---|---|---|
| **Events** | *entry points*: "something happened" | in | `OnInteract`, `OnDamaged`, `OnEnter(zone)` wake the graph |
| **Exposed parameters** | *configuration*, fixed per object | in | this guard's `patrolRoute` |
| **Actions and sent events** | the graph's *effects* on the world | out | "play walk", "start `blacksmith.dialogue`", send `Alarm` |
| **Blackboard** | *shared state* that lasts over time | both | read `self.target`, write `world.alarmRaised` |

- **Entry points are events** (plus the state machine's entry state, or a behaviour tree's root, which run when the graph starts). They're nodes in the graph, like `On Interact`.
- **There's no "output" or return value** for state machines, behaviour trees or dialogue: they act through side effects (actions, sent events, blackboard writes).
- **The exception is dataflow graphs** (VFX, procedural placement, materials, sound graphs). They behave like functions and have explicit **Graph Input** and **Graph Output** nodes ("inputs: position, noise seed -> output: a list of trees to place"): the graph's signature, like a Java method's parameters and return type, separate from the blackboard. The same goes for a **subgraph**, a graph used inside another like a function call.

In Java terms: exposed parameters are **constructor arguments**, events are **method calls**, the blackboard is **shared fields** many objects can see, and a dataflow graph's input and output nodes are a **function's parameters and return value**.

---

## 2. The rule of thumb: state vs moments

Deciding between the blackboard and an event is the most important habit:
- **Something that's true for a while -> blackboard.** "It is raining", "the alarm is on", "my target is the player", "the player met the blacksmith".
- **Something that happens at an instant -> event.** "Thunder struck", "the lever was pulled", "the player pressed E".

Why it matters: a graph that starts *later* (a guard whose cell loads after the alarm went off) **can still read state, but it missed the event**. If "alarm" were only an event, that guard would never know; as `world.alarmRaised = true` it sees it at once. The reverse mistake, storing "thunder struck" as a variable, needs someone to reset it, or it fires forever.

Both are often used for one fact: the lever sends the `LeverPulled` **event**, and the wiring sets `world.gateOpen = true` as **state**.

---

## 3. How the channels look in the editor

Most graph editors follow the same visual conventions, popularised by Unreal. There are **two kinds of pin**:
- **Flow pins (▶):** "what happens next". They carry *when*, no value.
- **Data pins (●):** they carry a value; the pin's colour is its type (Bool red, Float green, Object blue…).

Nodes are **coloured by category**, so each channel is recognisable at a glance:
```
[● On Interact      ]▶───▶[ Branch ]▶─true──▶[ Door: Open    ]▶───▶[ Send Event: GateOpened ]
    instigator ●              ▲ condition        target ● ◀── ( self )
                       ( game.hasKey )
  └ event (red) ┘          flow control (grey)   └ action (blue) ┘    └ sent event (purple) ┘
                       └ variable (green pill) ┘
```

### Events: the entry points

**The node:** an **event node** has a coloured header, **no flow input** (nothing comes before it), one flow output, and a data pin for each piece of information the event carries.
```
[● On Damaged            ]▶
     amount   ● Float
     source   ● GameObject
```

**Where events come from:**

| Source | Examples |
|---|---|
| **The engine** | `On Start`, `On Tick(dt)`, `On Collision(other)`, `On Enter Zone(other)`, `On Input(action)` |
| **Components** | a `Health` component offers `On Died`; a `Door` offers `On Opened`; an `Interactable` offers `On Interact(instigator)` |
| **The game (custom events)** | `Alarm`, `GateOpened`, `BossDefeated(bossName)`: declared in an **events list**, like variables, with their data fields |

Custom events are declared, not typed freely, for the same reasons as variables: typos, types, and "where is this sent / where is it received?".

**Filters** sit on the node itself: `On Enter Zone` where other has tag **Player**, so the graph only wakes for what it cares about.

**Per graph type:**

| Graph type | How events appear |
|---|---|
| visual scripting, level wiring | event nodes as above, starting a flow |
| state machine | a **transition triggered by an event**: `Idle -> Alert` on `Alarm`; plus each state's **On Enter / On Exit** hooks, which act like events |
| behaviour tree | usually no event nodes: an event writes the blackboard, and the tree re-decides (observer aborts) |
| dialogue | a **Start** node; the dialogue is started by a command from another graph ("Start dialogue") |
| quests | objectives **listen** for events: "Collect 3 × `ItemPicked(herb)`", "Complete when `BossDefeated(troll)`" |

### Exposed parameters: per-object configuration

**In the graph editor:** declared in the variables panel under **Parameters** (the eye icon), each with a type, a default, a tooltip, and optional limits (e.g. `speed: 0.5 … 5`). In the graph they're used through a **Get node** marked with the eye. They're **read-only**: a graph can't change its own configuration. For a changing value, copy the parameter into a `self.` variable on start.

**In the inspector**, where parameters really live: every object using the graph shows them under its component, with the right widget for the type.
```
▼ State Machine          [guard.bt    ] [Open]
    speed           [2.5    ]  ●                    <- the dot: changed from the graph's default
    patrolRoute     [Waypoints: 4]  [🎯 pick]        <- an object reference: pick it in the viewport
    aggressive      [x]
                                        [Reset to defaults]
```
- **Values changed from the default are marked** (bold, or a dot), as Unity marks prefab overrides, and can be reset one by one.
- **Object references** (a patrol route, the door a lever opens) get an **eyedropper**: click it, then click the object in the viewport. They're saved as the object's **persistent id**, which needs the persistent-uid work from the [open world plan](../open-world-plan.md).
- **Other engines:** Unreal calls these *Instance Editable* variables (plus *Expose on Spawn* to set them when code creates the object); Unity calls them serialized fields; Unity Shader Graph's exposed properties become the material inspector.

### Actions: the graph's effects

**The node:** an **action node** has a flow input and output (it happens *in* the flow) and data inputs for its arguments. By default it acts on **self**; an optional **target** pin makes it act on another object.
```
▶[ Play Animation   ]▶          ▶[ Door: Open      ]▶          ▶[ Wait        ]▶ 🕑
     clip  ● walk                    target ● (door)                seconds ● 2.0
```
**Latent actions** take time (`Wait`, `Move To`, `Play Animation until finished`). They're marked (a clock icon), and the flow continues only when they finish. In behaviour trees this is the **Running** status: an action reports *running* until it succeeds or fails.

**Where actions come from:** not a hand-written node for everything. **Components expose their own actions and events**, and the editor generates the nodes. The natural Java way is annotations, scanned at startup:
```java
public class Door extends Component {
    @GraphAction("Open")  public void open()  { ... }       // becomes the node [Door: Open]
    @GraphAction("Close") public void close() { ... }
    @GraphEvent("Opened") public final EventSlot opened = new EventSlot();   // becomes [● On Opened]
}
```
A new component's actions and events appear in every graph editor's node menu automatically. This is how Unreal's `UFUNCTION(BlueprintCallable)` works, and it's the answer to visual scripting's biggest cost: nobody maintains a node per feature by hand.

**Per graph type:**

| Graph type | How actions appear |
|---|---|
| visual scripting, level wiring | action nodes in the flow, as above |
| state machine | a **list of actions on each state**: *On Enter*: play "alert" sound; *On Update*: face target; *On Exit*: stop sound. Edited in the state's inspector (Unity's `StateMachineBehaviour`, PlayMaker) |
| behaviour tree | the **leaves** of the tree are actions (`Move To target`, `Attack`), returning running / success / failure |
| dialogue | **effects** on a line or choice: "give item", "start quest", "set `game.metBlacksmith`" |

### Sent events: the graph talks to others

**The node:** `Send Event` is an action whose argument is a **declared custom event**, with its data fields as pins, and a **"to"** option saying who receives it.
```
▶[ Send Event: Alarm    ]▶
     to      [ everyone in radius ▼ ]   radius ● 20
     source  ● (self)
```

| "To" | Who receives it | Use |
|---|---|---|
| **self** | graphs on this object | one graph telling another on the same guard |
| **a target** | one object, from a pin or parameter | the lever telling *its* door |
| **in radius** | objects near a point | noise that nearby guards hear |
| **world** | everything listening in the world | "boss defeated" |

**The receiving side** is an **event node for that custom event**, `[● On Alarm]`, in any graph. The editor's **"find references"** on an event lists every place it's sent and received.

### Level wiring: the same channels, shown on objects

For level design, wiring *objects* together is easier than writing a graph. The Source engine's Hammer editor does it with an **Outputs list in the inspector**:
```
Inspector: lever_01
▼ Outputs
   When        Target       Action     Parameter   Delay
   On Pulled   gate_01      Open       —           0.0 s
   On Pulled   alarm_bell   Ring       —           0.5 s
   [+ Add output]
```
Each row connects **one object's event** to **another object's action**: the same events and actions as the graph nodes, generated from the same annotations. The same wiring can also be shown as a node graph (objects as nodes) for an overview of a whole room; both views edit the same data.

---

## 4. Summary

| Channel | Node | Looks like | Defined by |
|---|---|---|---|
| **Events** (in) | event node | coloured header, no flow input, data pins for its fields | the engine, components (`@GraphEvent`), declared custom events |
| **Parameters** (in) | Get node with the eye | a pill; values set in each object's inspector | the graph's Parameters list |
| **Blackboard** (both) | Get / Set nodes, or dropdown fields | green pills; dropdowns on transitions and conditions | the declared `.vars` lists ([blackboards](blackboard.md)) |
| **Actions** (out) | action node | flow in and out, argument pins, optional target, clock icon if latent | the engine, components (`@GraphAction`) |
| **Sent events** (out) | Send Event node | an action with a "to" option | declared custom events |
