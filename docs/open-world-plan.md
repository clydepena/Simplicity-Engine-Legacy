# Open world, tilemaps and top-down rendering: plan

Status: **planned, not started.** Prerequisite: finish the basic editor parts first (see the last section), since most of this can only be checked visually.

The project's goal is 2D open worlds. This file records what has been decided, what is still open, and the order to build it in. Each step is meant to be a self-contained change that can be run and tested.

---

## 1. Object identity

In a streamed world, objects are saved and loaded all the time, so their identity has to survive that.

**Today:**
- `new GameObject(name)` takes the next number from a static counter.
- `GameObjectDeserializer` **ignores the saved uid**, so every load gives new uids (the level's 121–129 load as 0–9). Component uids *are* kept.
- `GameObject.copy()` gives a new uid twice (constructor + `generateUid()`), keeps the component uids identical to the original's, and re-fetches textures with `AssetPool.getTexture(filepath)` (a file path), although our sprites are loaded as resources.

**Planned:**
- **Persistent uid:** saved and restored, unique in the whole world. *Open:* a world-level counter stored in world metadata, or 64-bit random ids.
- **Runtime handle:** a small number assigned while an object is loaded and reused after it unloads. It is the entity id written into vertices, so the id buffer (floats, exact up to ~16.7 million), the outline lookup texture and the flag buffer stay small however big the world gets.
- **Two copy operations:** `duplicate()` gives a new identity (object and component uids), for Ctrl+D and placing; a play copy keeps them.
- **One fix-up step after deserializing** (textures, `StateMachine.refreshTextures()`), used by load, duplicate and play copy.

---

## 2. The world: cells, spatial and global objects

The world is **one document**: one continuous map, not one scene per cell. Cells are an internal storage and loading detail.

- A **cell** is a square **region of world space** (e.g. 16 × 16 units). It is the unit of loading, unloading and saving. It isn't made of anything; whatever lies inside the region belongs to it.
- **Spatial GameObjects** (NPCs, chests, props) belong to the cell their **position** is in. They load and unload with it, and move to another cell's list when they cross a cell line.
- **Global GameObjects** (world tilemaps, the player, managers) are **always loaded** and belong to no cell. A flag on the GameObject marks this (Unreal: *Is Spatially Loaded = false*).
- **No special world object:** world settings are plain data (below). World-wide *behavior* (weather, rules) would be a global GameObject with a component.
- Objects bigger than a cell can pop out when the cell holding their position unloads: keep the loading range above the largest object's reach, or mark them global.

**World settings** (`world.json`):

| Setting | Meaning | Suggested default | Rules |
|---|---|---|---|
| `cellSize` | streaming cell size, world units | 16 × 16 | fixed after creation (changing it means re-splitting every file) |
| `loadingRange` | cells kept loaded around each streaming source | 1 (a 3 × 3 block) | ≥ 1; the editor can use its own |
| `simulationRange` *(later)* | cells also updated, not just loaded and drawn | = `loadingRange` | ≤ `loadingRange` |
| `bounds` *(optional)* | limit the world to a range of cells | unbounded | |
| `pixelsPerUnit` | texture pixels per world unit (e.g. 32 means a 32 px image is 1 unit) | open | overridable per sprite sheet or tileset |
| sorting layers | see section 6 | Ground, World, Overhead | |
| uid counter | if counter-based ids are chosen | | |
| gravity, background color, default game camera, list of global objects | | | |

**Files:**
```
world/
  world.json         settings + global objects (including each tilemap object and its settings)
  cells/3_-2.json    that cell's spatial objects + each tilemap's chunk for the cell (keyed by the tilemap's uid)
```
Only cells with content have a file. Existing `saves/level*.json` can be converted by putting each object into the cell its position falls in.

**Streaming:**
- **Streaming sources** (the player; in the editor, the editor camera) keep the cells within `loadingRange` loaded.
- **Loading** a cell: read its file, create its objects (sprites to the world's sprite batchers, bodies to physics), and build each tilemap's chunk for the cell. **Unloading:** save if changed, then remove them.
- **Loading is not drawing.** Loading happens occasionally, when the set of nearby cells changes (disk, building buffers and shapes: expensive). Drawing happens every frame and only uses what is loaded. The loading range is bigger than the screen, so things load before they come into view. Drawing **culls** loaded chunks that are off screen.
- One `Physics2D` per world; cells add and remove their bodies.
- Positions are 32-bit floats. Very far from the origin (~100,000 units) movement gets jittery; the usual later fix is origin shifting.

**Editor:** the viewport shows one continuous world; cells load and unload around the editor camera. Cell lines are hidden by default, with an optional overlay. The hierarchy lists global objects (the tilemaps act as the layer list) and loaded spatial objects. Save writes `world.json` plus changed cells. Opening a level means opening another world folder.

**Play/Stop (approach #2, chosen):** Play runs a **separate world** that streams from the editor's in-memory cells where they're loaded and edited, and from disk elsewhere. Stop throws it away; the editor's world and everything pointing at it (selection, camera) is untouched. `World2DLayer.rebuildPhysics()` already rebuilds bodies from the current transforms on Play.

---

## 3. Units, sizes and the camera

- A **world unit** has no built-in length; it's a convention. Positions, `transform.scale`, physics, cell and tile sizes are all in world units.
- **Physics** treats 1 unit as 1 metre (Box2D; gravity `(0, -10)`). Box2D works best for moving objects about 0.1–10 units in size.
- **Screen size** comes from the camera: at zoom 1 it shows `projectionHeight` units (currently fixed at 3) top to bottom, so on a 1080 px window 1 unit = 360 px.
- **Texture pixels** only decide sharpness; an image is stretched over its object's world size. Loaded textures use `GL_NEAREST`, so pixel art stays sharp.
- **Suggested convention:** 1 unit = 1 tile = 1 metre.

**`pixelsPerUnit`** (world setting): only decides the **starting scale** of new sprites and a tileset's default tile size (`tilePixels / PPU`). *Chosen direction (a):* `transform.scale` stays the world size, so there is no renderer change and existing levels keep their meaning. (Option (b), Unity-style "image size × scale", would change what saved scales mean.)

**Camera modes** (a `Camera` setting, not a world setting): **fixed view height** (current), **fixed pixel scale** (a bigger window shows more of the world) and **pixel-perfect** (whole screen pixels per texture pixel, plus camera position snapping). This needs a `Camera.setViewportSize(width, height)` that the world calls instead of `setAspectRatio`. `projectionHeight` becomes a setting. Optional later: render the world at native resolution into a small framebuffer, then upscale by a whole number, as a step in the compositor chain.

**Example** (cell 16 × 16, tile 1 × 1, 32 × 32 px texture, 1080p, zoom 1, current 3-unit camera): 256 tiles per cell (512 bytes per tilemap per cell), a tile appears 360 px wide (11.25 screen pixels per texture pixel), and only 3 tiles are visible top to bottom, which is why the camera's view height needs to become a setting.

---

## 4. Tilemaps (Unity/Godot style, chosen)

A tilemap is an **ordinary GameObject with a `Tilemap` component**. Tiles are **data** held by that component, never one GameObject per tile (as with Unity's Tilemap, Godot's TileMap, Unreal's Landscape and foliage, and Minecraft's chunks). A developer can create **any number** of tilemaps. In practice each is a layer: ground, walls, decoration, overhead.

### World mode vs Local mode

| | **World mode (streamed)** | **Local mode (moves as an object)** |
|---|---|---|
| For | the world itself: ground, walls, terrain | airships, moving platforms, vehicles made of tiles |
| GameObject | global (always loaded) | spatial (loads with the cell its position is in) |
| Tile data | split and streamed **per world cell**, sparse (only where painted); no width/height | stored whole with the object, in its own grid; a max size is advisable (e.g. 64 × 64 tiles) |
| Grid origin | the world origin (0,0), so the painting grid is the world's regardless of the object's transform | the object's transform |
| Transform | **pinned**: position (0,0), rotation 0, scale 1; only draw order counts; editor fields read-only; runtime changes ignored with a warning; old files reset on load | normal: move, rotate, scale |
| Tile size | must **divide `cellSize` exactly** (16 → 1, 0.5, 0.25 ✓; 0.3 ✗), so every tile is inside one cell and a cell holds a whole number of tiles; width and height may differ | any |
| Physics | static merged rectangles per cell, from a `collision` setting; no `Rigidbody2D` needed | merged shapes in local space attached to its own body; none/Static = static, Kinematic = moved by code (platforms), Dynamic = simulated |
| Scale changes | n/a | rebuild the shapes: fine in the editor, warned about at runtime |
| Rendering | chunks in world space | a per-draw **model matrix**, so moving costs nothing |

- **Rules are enforced through a component validation hook** (e.g. `Component.validate(owner)`). A `Rigidbody2D` on a world-mode tilemap, or switching a tilemap with one to World mode, is **blocked in the editor** with a reason. In loaded files or runtime code it is **ignored with a one-time warning**, never a crash. The same hook can cover future rules.
- **A world tilemap has no size.** It's an endless sheet stored in **chunks**: one chunk = one tilemap's tiles inside one cell (e.g. 64 × 64 slots at tile size 0.25 in a 16-unit cell, about 8 KB at 2 bytes per tile id). Chunks only exist where something was painted.
- **Terms:** *cell* = a world region; *chunk* = one tilemap's tiles within one cell; *tile* = one square of a tilemap's grid.
- **Coordinate math:** `tx = floor(worldX / tileSize.x)`, cell `cx = floor(tx / tilesPerCell.x)`, index in chunk `tx - cx * tilesPerCell.x` (same for y).
- **World tilemap settings:** mode, tile size (must divide `cellSize`), tiles per cell (read-only), tileset(s), collision, sorting layer / order.
- **Grid shape:** square only at first (isometric/hex later).
- **Parallax** is a render setting ("scroll factor"), not a transform change, so it doesn't affect streaming.
- **Painting (editor):** brush, eraser and fill write into chunks of loaded cells and mark them changed. The grid shown is the active tilemap's, with cell lines optionally thicker. Clicking a tile selects its tilemap object (the chunk's vertices carry the tilemap's handle).
- **Runtime tile changes** (digging, building): update that chunk, re-upload it, and rebuild that cell's collision if on. Whether changes are saved is a game-save question.
- **Tiles with behavior** (doors, chests) are placed as ordinary spatial GameObjects instead.

---

## 5. Tilemap rendering

- **`TilemapChunk` implements `Drawable`:** built when its cell loads or its tiles change. It holds one quad per non-empty tile (corners = cell corner + (i, j) × tileSize; UVs from the tileset; texture slot; color; entity id = the tilemap's handle), uploaded once, and drawn with **one draw call**. Same index pattern as `RenderBatch`.
- **Same vertex layout as sprites**, so the default, picking, mask and flag shaders all work unchanged, and selection, outlines and rectangle picking work on tilemaps for free.
- One tileset image per tilemap is best (one texture per chunk draw); several can use texture slots, like `RenderBatch` (up to 7).
- **Seams:** pull tile UVs in by half a texel, or pad the tileset.
- **Animated tiles:** update their UVs every few frames (later: the shader picks the frame from a time value).
- **Memory:** a full 64 × 64 chunk is about 655 KB of vertices (16 × 16 is about 41 KB). A later optimization is "vertex pulling": store only tile ids as a small texture and let the shader compute each quad.

---

## 6. Top-down draw order: sorting layers and CPU Y-sorting (CPU sorting chosen)

Top-down games draw things lower on screen in front ("Y-sorting"). Sorting compares **bases** (feet), not centres.

**Sorting layers** (like Unity's): an ordered list, e.g. Ground (fixed) → World (**Y-sorted**) → Overhead (fixed). Every sprite and tilemap has a **layer** and an **order in layer** (tiebreak).

**New pieces:**

| Piece | Role |
|---|---|
| `SortingLayer` (settings data) | name, position in the list, mode: Fixed or Y-sorted |
| `TilemapChunk` (`Drawable`) | built-once chunks, for tilemaps on **Fixed** layers |
| `SortedBatcher` (`Drawable`) | for a **Y-sorted** layer, every frame: (1) collect what's on screen: the layer's sprites plus each tile of **Y-sorted tilemaps** in visible cells as its own item; (2) sort by sort Y, far first, ties by order in layer then a stable key like the uid (no flicker); (3) write the quads into one buffer refilled each frame; (4) draw in as few calls as possible, a new one only when the 7 texture slots or the buffer run out. The classic libGDX/MonoGame "sprite batch". Only on-screen items on Y-sorted layers are sorted (a few hundred to a few thousand: well under a millisecond). |
| `WorldRenderer` | draws the world layer by layer, in order: for Fixed layers, `SpriteBatcher` batches and tile chunks by order in layer; for Y-sorted layers, the layer's `SortedBatcher`. Justified now because drawing is several passes in a fixed order. |

**Changes to what exists:**

| Class | Change |
|---|---|
| `SpriteRenderer` | + sorting layer, order in layer, **sort point** (offset from the centre to the base; default open, e.g. the sprite's bottom edge) |
| `SpriteBatcher` | only sprites on **Fixed** layers; batches grouped by (layer, order) instead of zIndex; can draw one layer at a time; **fast removal** (each sprite remembers its batch and slot, and a gap is filled by moving the last sprite in, so only one sprite is dirtied). Unloading a cell removes hundreds of sprites; today every removal searches all batches and dirties every later sprite. |
| `RenderBatch` | the code writing a sprite's 4 vertices (`loadVertexProperties`) moves to a shared helper used by `SortedBatcher` and `TilemapChunk`; entity id = runtime handle |
| `sync()` | also moves sprites between `SpriteBatcher` and `SortedBatcher` when their layer or mode changes |
| `World2DLayer.onRender` | calls `WorldRenderer` |
| `SelectionRenderer` | the id, mask and flag passes must draw **the whole world in the same order as the real frame**, or "topmost" picks would be wrong under Y-sorting. It takes the world's full draw sequence (e.g. `WorldRenderer` as a `Drawable`) instead of one `SpriteBatcher`. All drawables share the sprite vertex layout, so its shaders are unchanged. |

---

## 7. Project folder

Like Godot (`project.godot`), Unreal (`.uproject`) and Unity (the project folder), the editor works inside a **project folder**. The project root is **the folder that contains the project file**.

```
MyGame/
  simplicity.project   project file (JSON): name, engine version, start world, editor startup world,
                       project-wide settings (sorting layers, pixelsPerUnit default, ...)
  assets/              images, sprite sheets, tilesets, prefabs
  worlds/overworld/    a world folder (world.json + cells/)
  .cache/              generated, not version-controlled
```

- **References are project-relative file paths** (e.g. `assets/tiles.png`). **Decided: no UIDs**, unlike Godot's UIDs and Unity's `.meta` GUIDs.
- **How assets are loaded, shared, moved and freed:** see `asset-pipeline.md`.
- **Moving the whole project folder is safe.** Only the recent-projects list in the user's preferences (absolute paths) goes stale.
- **The project file must stay at the root.** Moving it changes the root, so every path resolves to the wrong place. When opening, the editor checks that the expected folders (e.g. `assets/`, `worlds/`) sit next to the file, and says so clearly if not.
- **Moving or renaming assets inside the editor** goes through the project panel (the port of `ProjectExplorerWindow`), which rewrites references: in saved files (world and cell JSON, later prefabs: exact old path string replaced) and in memory (loaded objects' paths, `AssetPool`'s path-keyed cache). It can ask first ("12 files reference this asset; update them?"). Deleting warns if the file is still referenced.
- **Moving files outside the editor breaks references** (the accepted cost of path references). A missing file shows a placeholder (e.g. a magenta square) and is logged; optionally the panel lists missing assets.
- **Split:** the panel is UI only (browse, create folders, move/rename/delete, drag assets to the viewport or palette, open worlds). The file logic lives in a non-UI service, e.g. `ProjectFiles`: path resolution against the root, move/rename/delete with reference rewriting, and missing-file checks, so menus and later scripts can use it too.
- **Engine vs project files:** the engine's own files (shaders, editor icons, default `imgui.ini`) stay in the jar's resources. Game content (today's `images/`, sprite sheets and `saves/`) moves out into a project folder.
- **Per-user editor preferences** (theme, panel layout, recent projects) live in the user's folder, not the project.
- **Today** there's no project concept: `saves/level.json` resolves against the process working directory (`simplicity/` under Gradle), and file dialogs return absolute paths.

## 8. Open decisions

1. uid scheme: world-level counter or 64-bit random ids.
2. zIndex: replace it with **sorting layer + order in layer**, or keep `zIndex` as "which layer".
3. Where sorting layers are configured: per world, or project-wide (Unity: project-wide; the project file (section 7) is the natural place).
4. Default sort point: the sprite's bottom edge, with a per-sprite override?
5. Default `cellSize` (16?), default tile size, and `pixelsPerUnit`.
6. Camera: default mode, and whether `projectionHeight` becomes a world default.
7. Shared tile size: per tilemap (planned) or a shared grid setting like Unity's `Grid` parent.

---

## 9. Order of work

0. **Finish the basic editor parts first** (below), since tilemaps, streaming and Y-sorting are checked visually. The project folder (section 7, minimal version: project file + root + path resolution, test content moved into an example project) comes before Save/Load, so saved paths are project-relative from the start.
1. GameObject identity: persistent uid, runtime handle, `duplicate()` vs play copy, one fix-up step.
2. Extract the world's content into a container (objects, sprites, physics): the future cell.
3. Cell grid in the world, first with one cell holding everything (no behavior change), then a real `cellSize`.
4. Per-cell save format (world folder), and conversion of the existing levels.
5. Streaming sources and load/unload by range; editor camera as a source.
6. Play as a separate streaming world.
7. `SpriteBatcher` changes (fast removal, per-layer drawing, handles), sorting layers, `WorldRenderer`, `SortedBatcher`.
8. `Tilemap` component (World and Local modes), `TilemapChunk`, validation hook, painting tools.
9. Camera modes and `pixelsPerUnit`.

### Basic editor parts to finish first
Candidates (most are in `to-refactor/`):
- **Properties panel** (`PropertiesWindow`): edit the selected object's components.
- **Scene hierarchy** (`SceneHierarchyWindow`): list and select objects.
- **File menu: Save / Save As / Load** (commented out in `SimplicityEditor.onRenderMenuBar`).
- **Debug drawing** (`DebugDraw`): grid lines, collider outlines (collider offsets must be rotated by the object's rotation, matching `Physics2D.add`).
- **Sprite palette + placement** (`SpriteSelectorWindow`, the placement plan: a held `Placeable`, an ImGui ghost preview, a PLACE press owner).
- **Delete / duplicate** as editor commands, using `EditorSelection`.
- **Play/Stop restore** (approach #2) may wait for the world steps above; until then, Stop doesn't restore.
