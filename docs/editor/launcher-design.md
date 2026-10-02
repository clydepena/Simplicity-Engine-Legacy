# Launcher: design

Status: **being built** (`editor/panels/LauncherPanel.java`). Related: [open world plan](../open-world-plan.md) §7 (the project folder and `project.simplicity`), [asset pipeline](../asset-pipeline.md) (the project pool opened with the project).

What the project launcher shows and does, and how it lives in the editor's own window.

---

## 1. How it fits the editor

- **Same window, two modes.** The editor starts in **launcher mode** (a small centred window, about 900×550) and switches to **editor mode** (maximized, docked panels) once a project is opened. No second window, no second application.
- **One ImGui context.** The launcher is a mode of `SimplicityEditor`, not a second `ImGuiEditorLayer`: each `ImGuiEditorLayer` creates its own ImGui context and installs GLFW callbacks, and removing a layer only calls `onDetach()`, so swapping layers would leave a second context behind.
- **Launcher mode:** no dockspace (`shouldRenderDefaultDockspace()` returns false), no panels but the launcher, the world layer hidden and inactive, no world drawn as an image.
- **Opening a project:** `setProject(...)`, create the editor panels, show the world layer, apply the editor's minimum window size, maximize. Closing a project (later) is the reverse.
- **Window changes this needs:** no fixed 75%-of-screen minimum in `Window.init()` (applied when entering editor mode instead), and no unconditional `window.maximize()` in `Application.run()`. Size limits and attributes can be changed at any time (`glfwSetWindowSizeLimits`, `glfwSetWindowSize`, `glfwSetWindowAttrib`).

---

## 2. Main view

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  SIMPLICITY                                                     v0.1.0       │
│──────────────────────────────────────────────────────────────────────────────│
│                           │  Recent projects                [🔍 filter…    ] │
│   [ ＋  New Project    ]   │ ─────────────────────────────────────────────── │
│                           │  My Open World                          2 h ago  │
│   [ 📂  Open Project… ]   │  C:/Games/MyOpenWorld                            │
│                           │ ─────────────────────────────────────────────── │
│                           │  Test Project                          3 days ago│
│                           │  D:/tests/proj1                                  │
│                           │ ─────────────────────────────────────────────── │
│                           │  ⚠ Old Prototype                    (not found)  │
│                           │  E:/old/proto           [Locate…] [Remove]       │
│                           │                                                  │
│──────────────────────────────────────────────────────────────────────────────│
│  ⚠ Couldn't open D:/x/project.simplicity: made with a newer engine (format 3) │  <- only on an error
└──────────────────────────────────────────────────────────────────────────────┘
```

**Left: actions**
- **New Project:** switches the right side to the new-project form (section 3).
- **Open Project…:** a native file dialog (NFD, as in `IOHelper`) filtered to `project.simplicity`.

**Right: recent projects**, newest first:
- **Each row:** the project's **name** (from its project file) on the first line, its **folder** dimmer on the second; two projects can share a name.
- **Last opened** as a relative time ("2 h ago").
- **Double-click, or select and press Enter,** to open.
- **Right-click:** *Open*, *Show in Explorer*, *Remove from list* (only from the list; never deletes files).
- **Missing projects** (folder moved or deleted) are **greyed out with a warning**, offering *Locate…* (pick the new location) or *Remove*. Not dropped silently: the user may have just unplugged a drive.
- **A filter box** at the top, once the list grows.

**Bottom: an error bar,** shown only when something failed. Errors are shown *in* the launcher, not in a log the user can't see yet.

---

## 3. New-project view

Replaces the recent list, in the same window (not a popup on a popup):
```
│  New project                                                                 │
│                                                                              │
│  Name       [ My Open World                          ]                       │
│  Location   [ C:/Games                                ] [ Browse… ]          │
│                                                                              │
│  Will be created at:  C:/Games/My Open World/                                │
│                        └ project.simplicity, assets/, worlds/                │
│                                                                              │
│  ⚠ A non-empty folder with this name already exists.                         │
│                                                                              │
│                                              [ Cancel ]   [ Create ]         │
```
- **The final path is shown live,** so there are no surprises about where it goes.
- **Location remembers the last parent folder used,** so the second project is one click.
- **Validation while typing; Create stays disabled** until everything is valid:
  - the name is empty, or has characters Windows forbids (`\ / : * ? " < > |`)
  - the target folder exists and isn't empty
  - the location doesn't exist, or can't be written to
- **Create:** makes the folder, writes `project.simplicity`, creates `assets/` and `worlds/` (and an empty start world), adds the project to the recent list, and opens it.
- **Later:** a **template** choice (*Empty*, *Top-down 2D sample*) that copies a starter project instead of creating empty folders.

---

## 4. Empty state (first launch)

No empty list:
```
│                     Welcome to Simplicity                                    │
│                                                                              │
│                  [ ＋  Create your first project ]                            │
│                  [ 📂  Open an existing project… ]                            │
```

---

## 5. While a project opens

Opening can take a moment (engine assets, the start world, a first shader compile). Instead of a frozen window, show *"Opening My Open World…"* (a spinner or progress bar) for a frame or more, *then* switch to the editor and maximize. It's also the natural place for errors ("start world not found") before the editor appears.

---

## 6. Where the data lives

A recent-projects file in the user's folder, e.g. `%APPDATA%/Simplicity/recent.json` (per-user, never in a project):
```json
{
  "lastLocation": "C:/Games",
  "projects": [
    { "path": "C:/Games/MyOpenWorld/project.simplicity", "name": "My Open World", "lastOpened": "2026-10-01T09:14:00Z" },
    { "path": "D:/tests/proj1/project.simplicity",       "name": "Test Project",  "lastOpened": "2026-09-28T17:02:00Z" }
  ]
}
```
- **`name` is a copy,** so the list can show it without opening every project file; refreshed each time the project opens.
- **Missing check at startup:** `Files.exists(path)` per entry.

---

## 7. Building it in ImGui

### A window locked to the GLFW window, without docking

No dockspace is needed: a normal ImGui window placed at the main viewport's position and size, with every way of moving, resizing, docking or saving it turned off:
```java
ImGuiViewport vp = ImGui.getMainViewport();
ImGui.setNextWindowPos(vp.getWorkPosX(), vp.getWorkPosY());
ImGui.setNextWindowSize(vp.getWorkSizeX(), vp.getWorkSizeY());
ImGui.setNextWindowViewport(vp.getID());          // multi-viewports are on: keep it inside the main window

int flags = ImGuiWindowFlags.NoDecoration          // no title bar, resize grip, scrollbars, collapse
          | ImGuiWindowFlags.NoMove
          | ImGuiWindowFlags.NoDocking             // can't be docked, and nothing docks into it
          | ImGuiWindowFlags.NoSavedSettings       // nothing written to imgui.ini
          | ImGuiWindowFlags.NoBringToFrontOnFocus
          | ImGuiWindowFlags.NoNavFocus;

ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0.0f);     // square corners, flush with the window
ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0.0f);
ImGui.begin("##launcher", flags);                           // "##": an id with no visible title
ImGui.popStyleVar(2);
// ... launcher content ...
ImGui.end();
```
- **Main viewport, not window coordinates:** `ImGuiConfigFlags.ViewportsEnable` is on, so positions are screen coordinates; the main viewport's work area is exactly the GLFW window's inside (minus a menu bar, if any). Recomputed every frame, so it follows the window when it's resized or maximized.
- **`setNextWindowViewport`:** with multi-viewports, a window outside or moved off the main window would become its own OS window; this pins it to the main one.
- **No dockspace in launcher mode:** the editor layer already has the hook, `shouldRenderDefaultDockspace()`; returning `false` while no project is open means no dockspace host and no menu bar are drawn.

### The pieces

| Element | ImGui |
|---|---|
| the two columns | `beginChild` for the left, `sameLine`, `beginChild` for the right; or a 2-column table |
| a recent row | `selectable(label, selected, SpanAllColumns \| AllowDoubleClick)`, then `isMouseDoubleClicked(0)` |
| the dim path line | `pushStyleColor(Text, grey)` + `text(path)` |
| the right-click menu | `beginPopupContextItem()` |
| the title | the editor's bigger font, pushed with `pushFont` |
| the error bar | a child at the bottom with a red text colour, shown when `lastError != null` |

---

## 8. Order of work

1. **Recent list** (name, path, double-click to open) + **Open Project…** + **error bar**.
2. **New Project** form with the live path preview and basic validation.
3. **Missing-project handling** (grey, *Locate…* / *Remove*) and the **empty state**.
4. Later: filter box, right-click menu, "Opening…" screen, templates, and a warning when a project's `engineVersion` differs from the running engine.
