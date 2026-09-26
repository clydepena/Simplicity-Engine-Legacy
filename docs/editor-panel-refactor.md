# Editor Panel Refactor

Plan for porting the legacy editor tools (the `editor` package) into `NewImGuiLayer`, and why the ImGui layer gets a lightweight panel list instead of its own layer stack.

## How the existing tools are built

All eleven panels extend `ImGuiInterface`, which has `imgui(float dt)`, `destroy()`, and `updateCalc()` (which caches position, size, docked, focused and hovered). The old `ImGuiLayer` creates each one and calls `imgui(dt)` every frame. That base class is fine. The problem is how the panels reach what they need: through `OldWindow` statics, `MouseListener` statics, and `EventSystem.notify`.

| Panel | Depends on | Refactor to |
|---|---|---|
| `SceneHierarchyWindow` | `OldWindow.getScene().getGameObjectList()` | `ctx.world().getGameObjectList()` |
| `MenuBar` | `OldWindow.getScene().getFilename()`, `OldWindow.getImGuiLayer().setUIColors(n)`, `EventSystem.notify(SaveLevel/SaveLevelAs/LoadLevel)` | `ctx.editor().setUIColors(n)`; Save/Load become editor commands (below) |
| `GameViewWindow` | `OldWindow` position and framebuffer, `getTargetAspectRatio`, `MouseListener` viewport setters, `EventSystem.notify(StartPlay/StopPlay)` | Becomes the see-through Viewport panel: no `ImGui.image`, reports its hovered state, Play/Stop as editor commands |
| `PropertiesWindow` | `MouseListener` + `PickingTexture` for click-to-select, `update(dt, Scene)` | Selection moves to shared editor state. Picking is left for later (it needs a picking pass) |
| `SpriteSelectorWindow` | `OldWindow.getImGuiLayer().getEditorGameObject()` | `ctx.editorObject()` |
| `FileExplorerWIndow`, `ProjectExplorerWindow`, `TextEditorWindow` | `IOHelper.*(OldWindow.get(), …)` file dialogs | `IOHelper` takes the new `Window`: `ctx.window()` |
| `LoggerWindow` | Fed by the old layer's `onNotify(EventLogged)` | The editor layer forwards `EventLogged` to it |
| `NodeEditorWindow`, `SImGui` | Nothing from the engine | No changes |

Most of the table comes down to **one change: give every panel a shared context object** instead of letting it reach for globals.

```java
public final class EditorContext {
    private final NewImGuiLayer editor;
    private final World2DLayer world;
    private final Window window;
    private GameObject selected;          // was PropertiesWindow.activeGameObject; gizmos read it too
    private GameObject editorObject;

    public World2DLayer world()       { return world; }
    public Window window()            { return window; }
    public NewImGuiLayer editor()     { return editor; }
    public GameObject selected()      { return selected; }
    public void select(GameObject go) { selected = go; }
    public GameObject editorObject()  { return editorObject; }

    // editor commands: panels request, the editor layer applies them in onUpdate
    public void requestPlay()             { editor.queue(EditorCommand.PLAY); }
    public void requestSave()             { editor.queue(EditorCommand.SAVE); }
    public void requestLoad(String path)  { editor.queue(EditorCommand.load(path)); }
}
```

- **Selection belongs in the context.** Right now `PropertiesWindow` owns the selected object, and `GizmoSystem` reaches through `OldWindow.getImGuiLayer().getPropertiesWindow()` to find it. With selection in the context, the hierarchy, properties panel, gizmos and viewport all read and write the same thing without knowing about each other.
- **Commands instead of `EventSystem.notify`.** Menu items and Play/Stop currently fire global events that `OldWindow` handled. They become requests that the editor layer queues and applies in its `onUpdate`, so the scene never changes in the middle of drawing the UI. The world stays unaware of the editor.

## Should the ImGui layer have its own layer architecture?

Not a full copy of `Application`'s. Give it a **lightweight panel list** that borrows only the parts that fit.

Why not a full layer stack:

- **Panels don't need ordering.** Application layers are ordered because they draw on top of each other and pass events down. Panels are placed by docking, and ImGui routes input to widgets by itself. A panel stack would have nothing to sort by.
- **There's no compositor chain.** All panels draw into the same ImGui frame between one `newFrame()` and one `render()`. There's no per-panel framebuffer to pass along.
- **Freeze and hide already exist for panels.** ImGui's `begin(name, ImBoolean open)` gives an open/close toggle, which is all "hidden" needs to mean for a panel. Very few panels would have logic to freeze.
- **Two layer systems with the same vocabulary would be confusing.** "Layer" should keep meaning one thing in this engine.

What's worth borrowing:

```java
public abstract class EditorPanel {                  // replaces ImGuiInterface
    protected EditorContext ctx;
    protected final ImBoolean open = new ImBoolean(true);

    public void onAttach(EditorContext ctx) { this.ctx = ctx; }   // gets the context instead of globals
    public void onUpdate(float dt) {}                             // optional non-UI logic
    public abstract void onImGui();                               // called between newFrame and render
    public void onNotify(Event e) {}                              // app events: logs, level loaded
    public void destroy() {}

    public boolean isOpen()        { return open.get(); }
    public void setOpen(boolean b) { open.set(b); }
}
```

```java
// NewImGuiLayer
private final List<EditorPanel> panels = new ArrayList<>();

public void onUpdate(float dt) {
    applyQueuedCommands();                               // save/load/play/select requests
    for (EditorPanel p : panels) p.onUpdate(dt);
}

public void onRender(RenderContext ctx) {
    imGuiGlfw.newFrame(); ImGui.newFrame();
    renderDockspace();                                   // menu bar lives here
    for (EditorPanel p : panels) if (p.isOpen()) p.onImGui();
    ImGui.render(); /* draw into the frame, platform windows */
}
```

This gives:

- **The same lifecycle ideas as layers**: attach with dependencies injected, update, draw, destroy. Only the parts panels actually use.
- **A "View" menu for free.** The menu bar lists `panels` and toggles `setOpen`, like Photoshop's Window menu.
- **Adding a panel is one line** (`panels.add(new SceneHierarchyWindow())`), and it can't reach `OldWindow`, because everything comes through `ctx`.

Panel additions don't need to be queued like layer commands. Panels are created once, at attach time, and only opened or closed afterwards.

## Porting order

`EditorPanel` + `EditorContext` first, then the panels with the fewest dependencies:

1. `LoggerWindow`, `NodeEditorWindow`
2. `SceneHierarchyWindow`, `MenuBar`
3. The file windows (needs the `IOHelper` signature change)
4. `SpriteSelectorWindow` and `PropertiesWindow` (need the editor object and selection)
5. `GameViewWindow` as the see-through viewport panel
