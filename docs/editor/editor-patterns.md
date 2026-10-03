# Editor patterns

The patterns that show up in almost every editor (Unity, Godot, Photoshop, VS Code, Blender), what each one
solves, and where Simplicity already uses it. The table at the end is the short version.

---

## 1. The Command pattern

**Idea:** turn an action into an **object**. Instead of code that *does* something ("set position to 5"), build
an object that *describes* it and knows how to apply and reverse it.

```java
interface EditorCommand { String name(); void undo(); void redo(); }
```

Because an edit is an object, it can be stored (undo history), replayed (redo), named ("Undo Move 3 objects"),
and later grouped or sent elsewhere (networked co-editing, macros).

In Simplicity: `editor/undo/` — `EditorCommand`, `UndoHistory`, `TransformCommand`, `FieldCommand`,
`AddObjectsCommand`, `DeleteObjectsCommand`.

### Two ways to write a command

| Kind | How undo works | Example | Good / bad |
|---|---|---|---|
| **Store the values** (state-based) | keeps the before and after values and writes the old one back | `TransformCommand`, `FieldCommand` | exact, never drifts, works for any field; costs memory for big data |
| **Store the operation** (operation-based) | keeps "what was done" and runs the inverse | "move by +5", undone as "move by -5" | tiny; but needs an inverse for every operation, and floating-point errors pile up |

Editors mostly store **values** for properties and transforms. Operations are used where the data is huge:
painting on a 4K texture stores only the changed area, not the whole image.

### Variations

- **Execute vs record.** Some commands perform the edit (`execute`: delete). Others are recorded *after* the
  edit was applied live (`record`: a gizmo drag, an inspector drag), so the user sees the change while dragging
  but gets one undo step.
- **Merging (coalescing).** Typing "hello" shouldn't take 5 undos. A command can absorb the next similar one
  (`mergeWith`), usually within a time window or until a "seal" (an undo, a save, a different field).
- **Macro / composite command.** One command made of several: "Paste 10 objects" is 10 adds, undone as one.
  It's the Composite pattern applied to commands: `CompositeCommand(List<EditorCommand>)`, which undoes its
  children in **reverse order**.
- **Transactions / edit sessions.** "Begin edit... many changes... end edit" becomes one command. The inspector's
  edit session is a small transaction. Bigger editors expose `history.beginGroup("Align objects")` ...
  `endGroup()` so a tool can make many changes that undo together.
- **Pointing at objects by id instead of reference.** The biggest practical problem: a command holds the objects
  it changed. If those objects are recreated (load, Play/Stop, network sync), the references go stale. Big
  editors store **stable ids** and look the object up on undo. This is why Simplicity clears the history on Stop
  until game object uids survive serialization.
- **One history per document, or one global history.** Photoshop has one per image; Unity has one across scenes
  and assets. Per-document is simpler, and is what Simplicity has (the world's history).
- **Save point.** The history remembers its depth at the last save, so "unsaved" means "not at the save point".
  Undoing back to it clears the `*`.
- **Lifetime of things only a command holds.** A deleted object can't be freed while a command can still bring
  it back. Commands get a `discard()` when they leave the history for good, and free it then.

### Commands as *actions* (the other meaning)

Editors also use "command" for **things you can trigger**: Save, Duplicate, Toggle Grid. One action object has a
name, a shortcut, an icon, an enabled check and what it does. The menu, the toolbar, the shortcut and the command
palette (Ctrl+Shift+P in VS Code) all read from the same action list. This **action registry** is related to undo
commands but separate from them: an action often *creates* an undo command.

```java
record EditorAction(String name, String shortcut, BooleanSupplier enabled, Runnable run) {}
```

Today Simplicity writes each shortcut and menu item by hand in `SimplicityEditor`; an action registry would
remove that duplication.

---

## 2. Data and documents

- **Dirty flag:** "changed since saved". It also appears inside engines, like `SpriteRenderer.isDirty` for
  "draw me again".
- **Unit of Work:** collect the changed documents and save them together: `UnsavedChanges` with Save All.
- **DTO / file model:** a plain class that matches the file (`WorldFile`, `ProjectFile`), kept separate from the
  live runtime objects so the format can change independently.
- **Versioned formats + migration:** `formatVersion`, plus code that upgrades old files on load (old world files
  that are a bare `[]` array).
- **Atomic save:** write a `.tmp` file, then rename it over the real one. A crash never leaves half a file.
- **Memento:** capture an object's state to restore it later without exposing its internals. Play mode's snapshot
  (`PlaySession`) is a memento of the whole world. Some editors implement *all* undo as mementos (a snapshot before
  and after each edit): simple, but memory-heavy.

## 3. Assets

- **Handles (Proxy):** `Asset<T>` stands in for the real texture. Code holds the handle, while the pool loads,
  reloads, swaps in a placeholder or moves the path underneath.
- **Registry / Strategy by type:** loaders and savers are chosen by file extension (`saverFor("sheet")`). New
  asset types plug in without touching the pool.
- **Asset database + GUIDs:** Unity and Godot give every asset a permanent id stored in a `.meta` / `.import` file,
  so renaming or moving a file doesn't break references. It's the step beyond referring to assets by path.
- **Hot reload:** watch the files and reload changed assets live. It relies on handles, so existing references
  see the new data.

## 4. Selection and tools

- **Selection model:** one shared selection object (`EditorSelection`) that every panel reads and changes. The
  hierarchy, viewport and inspector stay in sync because they share it, not because they talk to each other.
- **State pattern for tools:** the active tool (Translate / Rotate / Scale, later Paint / Erase / Select) decides
  how mouse input behaves. Each tool is an object with `onPress / onDrag / onRelease / draw`, and switching tools
  swaps the object. The `Gizmo` with its `Tool` enum is a small version of this; as tools grow, each becomes its
  own class.
- **Gizmos and handles:** draw interactive handles over the scene, hit-test them first, and let a grabbed handle
  "own" the drag (the viewport does this: a gizmo press never reaches selection).

## 5. UI architecture

- **Immediate-mode UI (ImGui):** the UI is drawn again every frame from the current data, so there's no separate
  UI state to keep in sync. That's why the inspector can read fields directly each frame. The challenge is knowing
  when an edit *ends*, which the inspector's edit session solves.
- **MVC / MVVM (retained-mode editors):** in Qt / WPF / web editors the widgets persist, so data and UI are kept in
  sync with bindings or observers. ImGui mostly avoids this.
- **Panels / docking:** each panel is a self-contained view (`SimplicityPanel`) with a shared context. A new tool
  window is a new class, with no changes elsewhere.
- **Reflection-driven inspector (property descriptors):** fields are found automatically and drawn by type, with
  **custom drawers** registered for special types (Unity's `PropertyDrawer`, Godot's `_get_property_list`).
  Simplicity has the automatic part (`InspectorPanel` with `SImGui`'s property grid); a drawer registry
  (`Map<Class<?>, Drawer>`) is the next step, e.g. for `Asset<Texture>` (show a thumbnail, pick another).
- **Validation hook:** after an outside edit, the object can react: `Component.onFieldsChanged()`, called
  `OnValidate` in Unity.

## 6. Communication

- **Observer / events:** systems announce things ("selection changed", "asset reloaded", "world saved") and others
  listen, without depending on each other. Simplicity has an `EventSystem`; editor-level events (e.g. "selection
  changed", so the hierarchy can scroll to the object) would use it.
- **Context / service locator:** one object passed around that holds the shared services
  (`SimplicityEditorContext`: world, selection, history, pools). Simple, and works well up to a fairly large
  editor.
- **Mediator / facade:** one front door for a subsystem. `editorContext.execute / undo / deleteSelected` hide the
  history and enforce "no editing while playing".

## 7. Scene structure

- **Composite:** objects contain children that are objects, which forms the hierarchy tree. Not built yet (the
  world is a flat list), but most editors have it.
- **Prototype / prefabs:** make new objects by cloning a template (`GameObject.copy()` is the clone). Prefabs add
  "instances remember their template and only store what they override", one of the harder systems in editors.
- **Visitor:** walk the scene tree and do something to every node (save, find all lights, collect render items)
  without each object knowing about every operation.

## 8. Play mode and runtime

- **Snapshot and restore:** Unity-style Play, which Simplicity has (`PlaySession`), or **run the game in a separate
  process**, Godot-style: full isolation and the real startup path, but it needs the game runtime entry point.
- **Editor code depends on the game, never the reverse.** That's why ImGui was removed from components. Unity
  enforces this with `Editor/` folders that aren't included in builds.

---

## Where Simplicity stands

| Pattern | Status |
|---|---|
| Command + history, merging, save point, edit sessions | done |
| Composite (macro) commands | missing; needed for multi-step tools |
| Commands referring to objects by stable id | missing; blocked on the uid design |
| Action registry (menus, shortcuts, palette from one list) | missing; would clean up `SimplicityEditor` |
| Dirty flag, Unit of Work, DTO, atomic save, versioning | done |
| Asset handles, codec registry | done |
| Asset GUIDs / `.meta` files | missing |
| Shared selection model | done |
| Tools as State-pattern classes | partial (the gizmo's `Tool` enum) |
| Reflection inspector | done; custom drawer registry missing |
| Hierarchy (Composite), prefabs | missing |
| Play snapshot (Memento) | done |

The most useful next pieces are **composite commands** and an **action registry**: both are small and make every
future tool simpler. **Stable ids** unblock undo across Play, the hierarchy and prefabs.
