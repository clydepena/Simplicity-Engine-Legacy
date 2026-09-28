# Asset handling pipeline: plan

Status: **planned, not started.** Related: `open-world-plan.md` (section 7, project folder).

How the engine finds, loads, shares, moves and frees the project's assets (images, sprite sheets, worlds, and later prefabs, sounds, fonts). This is the textbook game-engine resource manager (see "Patterns"), keyed by file path.

---

## 1. Decisions

- **References are project-relative file paths**, e.g. `assets/tiles.png`. **No UIDs** (unlike Godot's UIDs and Unity's `.meta` GUIDs).
- **The project root is the folder that contains the project file.** Moving the whole project is safe.
- **One shared handle per asset.** Fields hold the handle itself; there is no separate per-field wrapper.
- **Moves inside the editor keep references working.** Moves outside the editor are allowed to break them (a placeholder is shown and the problem logged).
- **Engine files** (shaders, editor icons, default `imgui.ini`) stay in the jar, in their own small cache. **Project assets** go through the asset database.

---

## 2. Asset types and their references

Assets form a **dependency graph** (a DAG), not a tree: one asset can be used by many others. **Leaves** are raw files that reference nothing; **branches** are files that hold paths to other files. Every arrow below is a field holding an `AssetHandle`, and every box is an asset type with a loader.

```
Project file (.simplicity)
 └─► World (.smp)                       settings + global objects
      ├─► Cell files (cells/x_y.json)   spatial objects + tilemap chunks
      │    └─► GameObject ─► Components
      └─► GameObject (global) ─► Components

Components that reference assets:
  SpriteRenderer ─► Spritesheet (.sheet) ─► Texture (.png)      leaf
  Tilemap ───────► Spritesheet (later: Tileset ─► Spritesheet)
  StateMachine ──► animation states ─► sprites ─► ...
  (later) AudioSource ─► Sound (.wav)                            leaf
  (later) FontRenderer ─► Font (.ttf)                            leaf

Prefab (.prefab) ─► GameObjects ─► Components ─► ...
Prefab ─► Prefab            nested prefabs: the one place cycles must be prevented
```

The graph is used for: delete warnings and rewriting references on a move (**up**: who references this?), and export and memory audits (**down**: what does this pull in?). Tools for drawing it: draw.io / diagrams.net (VS Code extension, saved as `.drawio.svg`), Excalidraw, tldraw; one shape for branches, another for leaves, arrows meaning "references".

### Asset files hold data; classes hold it in memory

A sprite sheet as an asset file instead of code (today sheets are defined in code: `new Spritesheet(texture, 32, 32, 48, 0)` in `LevelEditorSceneInitializer`):

`assets/sprites/tiles.sheet`
```json
{ "texture": "assets/sprites/tiles.png", "spriteWidth": 32, "spriteHeight": 32, "count": 48, "spacing": 0 }
```
```java
public class Spritesheet {
    AssetHandle<Texture> texture;        // the one outgoing edge
    int spriteWidth, spriteHeight, count, spacing;
    transient List<Sprite> sprites;      // computed on load, never saved
}
```
- **An asset doesn't store its own path;** the database knows where it came from (its key). Only references to *other* assets are paths.
- **No leading slash**, and **one extension per type** (`.sheet`, `.prefab`, `.smp`), so the loader and the project panel's icon are picked by extension.
- **Sprites would reference the sheet plus an index** (`"sheet": "assets/sprites/tiles.sheet", "index": 12`), instead of today's per-sprite texture path and UVs repeated in every saved object. Moving `tiles.png` then rewrites one `.sheet` file instead of every cell.

A tilemap component, for example:
```java
public class Tilemap extends Component {
    Mode mode = Mode.WORLD;              // WORLD (streamed, pinned) or LOCAL
    Vector2f tileSize = new Vector2f(0.25f, 0.25f);
    AssetHandle<Spritesheet> tiles;      // later possibly a Tileset (per-tile collision, animation, terrain rules)
    boolean collision;
    TileChunk localTiles;                // LOCAL mode only; WORLD mode tile data lives in the cell files
}
```

---

## 3. `AssetHandle<T>`: one per asset, shared by every reference

```java
public final class AssetHandle<T> {
    private String path;          // current project-relative path; changed only by AssetDatabase.move()
    private final Class<T> type;  // Texture, Spritesheet, ...
    private T loaded;             // null until loaded
    private State state;          // NOT_LOADED, LOADED, MISSING (later: LOADING)
    private int refCount;         // how many loaded things use it

    public String path() { ... }
    public T get()       { ... }  // loads through the database on first use; MISSING -> placeholder

    // package-private: only AssetDatabase changes these
    void setPath(String p) { ... }
    void setLoaded(T t)    { ... }
    void acquire()         { ... }
    void release()         { ... }
}
```

**Why one shared handle:** if each field held its own path string, 50 sprites using `tiles.png` would hold 50 copies, and a move would have to find and update all of them. With one shared handle, a move updates **one** `path`, and every field sees it.

**Why no separate `AssetRef` wrapper around it:** a per-field wrapper pays off where creating and destroying it counts references automatically (C++, Rust/Bevy), or for per-reference kinds (strong vs weak). Java has no destructors, so counting is explicit anyway (owners acquire and release), and package-private setters already hide the internals. Godot is similar: a field holds the shared resource itself. A wrapper can come back if per-reference kinds are ever needed.

**Saving and loading:** a handle is written as just its path (`"texture": "assets/tiles.png"`), through one custom Gson adapter. Reading a path asks the database for that path's handle, so every reference to the same path shares it automatically. The file format doesn't change.

---

## 4. `AssetDatabase`: the cache

```java
public class AssetDatabase {
    private final Path projectRoot;
    private final Map<String, AssetLoader<?, ?>> loaders = new HashMap<>();   // key: extension
    private final Map<String, AssetHandle<?>> assets = new HashMap<>();       // key: normalized relative path
    private final List<AssetHandle<?>> pendingUnload = new ArrayList<>();
    private final Map<Class<?>, Object> placeholders = new HashMap<>();
}
```

| Field | Holds | Why |
|---|---|---|
| `projectRoot` | the project folder's absolute location, e.g. `C:/Games/MyGame/` | everything else uses short relative paths; they're joined with the root only when a file is actually read. Moving the project changes only this. |
| `loaders` | one loader per file extension (`.png`, `.jpg` → `TextureLoader`; `.sheet` → `SpritesheetLoader`; later `.prefab`, ...) | each file type needs its own code; several extensions can share one loader (stb_image decodes PNG, JPG, BMP, TGA). A new type means registering one more loader. |
| `assets` | one handle per asset referred to, keyed by normalized relative path | **the cache itself:** one handle per file, so each asset exists once in memory and a move updates one place. A handle can exist while its asset isn't loaded. |
| `pendingUnload` | handles whose count dropped to 0, not freed yet | avoids thrashing (a player stepping back and forth over a cell border reloading the same texture); taken off the list if used again before it's freed |
| `placeholders` | one fallback per type, e.g. a magenta checkerboard `Texture` | returned by `get()` for a `MISSING` file: the problem is visible on screen and logged, nothing crashes |

**Normalize path keys.** Windows file names aren't case-sensitive, and paths can be written several ways. Without normalizing, `assets/Tiles.png`, `assets\tiles.png` and `./assets/tiles.png` become three entries for one file. Before a path is used as a key: forward slashes; no leading `./` or `/`; `..` resolved; one consistent case rule (e.g. the case the file system reports).

**Methods:**

| Method | Does | Uses |
|---|---|---|
| `handle(path, type)` | normalizes the path; returns the existing handle or creates one (not loaded). Deserialization calls it for every path it reads. | `assets` |
| `get(handle)` | if not loaded: picks the loader by extension, reads `projectRoot` + path, stores the result. If the file is missing: state `MISSING`, returns the placeholder. | `loaders`, `projectRoot`, `placeholders` |
| `move(oldPath, newPath)` | called by the project panel after a move: re-keys the handle and sets its path | `assets` |
| `acquire(handle)` | a loaded object starts using the asset: `refCount++`, taken off `pendingUnload` | `pendingUnload` |
| `release(handle)` | a loaded object stops using it: `refCount--`; at 0 it goes on `pendingUnload` | `pendingUnload` |
| `update()` | once per frame on the render thread: frees handles that have waited long enough (dispose GPU memory, `loaded = null`, state `NOT_LOADED`) | `pendingUnload` |

**One texture's life:**
1. A cell file says `"assets/tiles.png"` → `handle(...)` creates the handle, not loaded.
2. An object in that cell loads → `acquire` (count 1) → the first `get()` runs the `.png` loader; the texture is on the GPU.
3. Two more objects use it → count 3; still one texture in memory.
4. The cell unloads → three `release`s → count 0 → onto `pendingUnload`.
5. Seconds later, nothing wanted it → `update()` frees it. The handle stays in `assets` and reloads if anything asks again.

---

## 5. Loaders and loading stages

| Stage | `tiles.png` | `tiles.sheet` | Thread |
|---|---|---|---|
| 1. Resolve the path | `assets/tiles.png` → `C:/Games/MyGame/assets/tiles.png` | same | any |
| 2. Read the bytes | raw file bytes | JSON text | any (slow: disk) |
| 3. Decode / parse | stb_image: bytes → pixels + size | JSON → fields | any (CPU) |
| 4. Resolve dependencies | none (leaf) | get the texture's handle | any |
| 5. Finish / upload | `glTexImage2D`: pixels → GPU texture | cut the texture into `Sprite`s | **render thread only** |
| 6. Register | store in the handle, state `LOADED` | same | database |

- **Reading bytes (stage 2) stays out of the loaders,** in a small file-source layer. Loaders ask for "the bytes of `assets/tiles.png`" and don't care whether they come from a loose file (editor, running from the project), the jar (engine files) or later a packed archive in an exported game (like Unreal's `.pak` or Godot's `.pck`).
- **Decode and finish are separate loader steps,** so decoding can move to a background thread later without a hitch, as in libGDX's `loadAsync()` / `loadSync()`:
  ```java
  interface AssetLoader<T, D> {                 // D = decoded, not-yet-uploaded data
      List<String> extensions();                // e.g. [".png", ".jpg", ".jpeg", ".bmp", ".tga"]
      D decode(byte[] bytes, AssetDatabase db); // stages 3–4: any thread
      T finish(D decoded);                      // stage 5: render thread
  }
  ```
- The database does stages 1, 2 (through the file source) and 6, and calls the loader for 3–5.
- **Start simple:** everything can run on the render thread in order. Keeping reading outside the loader and `finish` separate means background loading can be added later without rewriting loaders.
- Loaders can also be keyed by type (`Texture.class`) for code that asks for "a `Texture` at this path". Extension-keyed is enough to start.

---

## 6. Moving and renaming assets

The old path can be in three places:

1. **The cache:** re-key the handle from the old path to the new one. The loaded asset stays in memory; there's no reload.
2. **Loaded objects and assets:** nothing to find. They hold the shared handle, whose path is updated once. Whatever references the moved file is marked as having unsaved changes, so its next save writes the new path.
3. **Files that aren't loaded** (other worlds, unloaded cells, closed prefabs):
   - **Rewrite them now:** search the project's files for the exact old path string and replace it. Simple; fine for small projects.
   - **Or redirect** (Unreal's redirectors): record `old path → new path` in a small project table; loading resolves old paths through it; a "fix up redirects" action, or ordinary saving, rewrites files over time. Scales to open worlds with thousands of cell files.

**A move, in order:** move the file on disk → update the handle and its cache key → mark loaded referencers changed → rewrite unloaded files (or add a redirect) → refresh the panels.

**UI:** the project panel (port of `ProjectExplorerWindow`) is UI only. It can ask first ("12 files reference this asset; update them?") and warns when deleting a referenced file. The file logic lives in a non-UI service (e.g. `ProjectFiles`), so menus and scripts can use it too.

**Moves outside the editor break references** (the accepted cost of path references): the handle's state becomes `MISSING`, the placeholder is shown, and the problem is logged. Optionally the project panel lists missing assets.

**Later:** a reference index (`Map<path, Set<referencing files>>`, built by one scan when the project opens and updated on save) makes "used by" instant, and can power a small "References" view in the project panel (like Godot's Owners dialog) and the export step.

---

## 7. Unloading (freeing memory)

**Why:** GPU and RAM are finite; streaming needs far areas' assets freed; leaving a world; long editor sessions; replacing an asset on hot reload.

| Policy | Meaning |
|---|---|
| **Reference count hits zero** | nobody loaded uses it → it can be freed |
| **Delay / memory budget (LRU)** | zero-count assets wait, and are freed after a while or when memory passes a budget, least recently used first |
| **Scope-based** | everything belonging to a world is freed when it's closed |
| **Explicit** | game code asks for it |
| **Pinned** | never freed (engine assets, UI, player) |

**Planned:** reference count + delay/budget (`pendingUnload`). Owners acquire and release **in one place**: an object acquires its assets when it loads and releases them when it unloads or is destroyed. That avoids leaks from forgotten releases and double releases.

**Two kinds of memory in Java:**
- **Java objects** (the `Texture` object, arrays) are freed by the **garbage collector** once nothing references them. You can't free them yourself; "destroying" them means dropping every reference (out of the map, `loaded = null`).
- **Native and GPU resources** (OpenGL textures and buffers, LWJGL `MemoryUtil` memory, OpenAL buffers) are **invisible to the garbage collector**. If a `Texture` is collected without `glDeleteTextures` having been called, GPU memory leaks for the rest of the run. They need an explicit `dispose()`, like `Framebuffer.destroy()` and `RenderBatch.destroy()`.
  - **OpenGL calls must run on the render thread** (the one owning the GL context), so freeing is queued to it.
  - **Don't free GL resources from finalizers or `Cleaner`:** those run on other threads, at unpredictable times, and finalizers are deprecated. A `Cleaner` is only useful as a safety net that logs "texture leaked: never disposed".

```java
void unload(AssetHandle<?> h) {                       // on the render thread
    if (h.loaded instanceof Disposable d) d.dispose(); // free native/GPU memory
    h.setLoaded(null);                                 // drop the Java reference so the GC can collect it
    h.setState(State.NOT_LOADED);                      // the handle stays: path known, reloads on next get()
}
```
A small `Disposable` interface (libGDX has one) marks asset types that hold native resources: `Texture`, later sounds and fonts.

---

## 8. Patterns

| Part | Pattern | Where it's described |
|---|---|---|
| One handle per path, shared | Identity Map; interning / Flyweight | Fowler, *Patterns of Enterprise Application Architecture*; GoF |
| Handle that loads on first use | Handle + Virtual Proxy (lazy loading) | Gregory, *Game Engine Architecture* (resource management); GoF Proxy |
| Loaders by extension | Strategy / plugin registry | GoF |
| `refCount` | Reference counting | standard |
| The whole thing | Resource / asset manager | *Game Engine Architecture* |

Similar systems: Godot's resource cache (one `Resource` per path, reference-counted; editor moves update the path), libGDX `AssetManager` (path keys, loaders per type, reference counting, `unload`), Bevy `AssetServer` and `Handle<T>`, Unreal's soft object paths, asset registry and redirectors, Unity's `AssetDatabase` and Addressables handles, Ogre's `ResourceManager`. The main difference: most key by a permanent id; we key by path, so a move re-keys the handle (like libGDX).

Pitfalls: forgotten releases leak and double releases free assets still in use (keep acquire/release in one place); reference cycles never reach zero (only nested prefabs can form them, and those must be kept cycle-free); background loading needs a `LOADING` state that other requests wait on; handles of unloaded assets stay in the map (a few fields each, fine).

---

## 9. Where the code is today

- **`AssetPool`:** static maps per type (`shaders`, `shadersRes`, `textures`, sprite sheets), some keyed by absolute file path, some by resource path; two ways to load (`getTexture` from files, `getTextureFromRes` from the jar); **nothing is ever unloaded**; no project concept.
- **Loaded textures have no way to free their GL texture;** only `Framebuffer.destroy()` deletes the textures it owns.
- **Deserialized `Texture`s are placeholders holding a `filepath`,** swapped for the cached one after loading by `refreshTextures()` (`LevelEditorSceneInitializer`) and in `GameObject.copy()`. `copy()` uses `AssetPool.getTexture(filepath)` (file path), although our sprites are loaded as resources. The handle design replaces this swap.
- **`Texture.initFromRes` does every stage in one method:** read (`IOHelper.ResToByteBuffer`), decode (`stbi_load_from_memory`), upload (`glTexImage2D`). `Shader` reads text, then compiles. This code becomes the texture and shader loaders' `decode` and `finish`.
- **Sprite sheets are defined in code** (`AssetPool.addSpritesheetToRes(...)`), and **saved sprites repeat the texture path and UVs** in every object.
- **Game content is bundled in the engine's resources** (`src/main/resources/images`, sprite sheets); it moves into a project folder.

---

## 10. Order of work

1. Project folder, minimal: project file, root, path resolution; move the test content into an example project (see `open-world-plan.md` section 7).
2. File source (bytes by path: project files and jar), path normalization.
3. `AssetHandle`, `AssetDatabase` (handles, loaders, placeholders), a Gson adapter writing handles as paths; `TextureLoader` from `Texture.initFromRes`. Replace `refreshTextures()`.
4. Sprite sheet asset files (`.sheet`) and sprites referencing sheet + index; conversion of existing levels.
5. `Disposable`, reference counting and `pendingUnload` (needed once cells stream).
6. Project panel with safe moves (rewrite unloaded files), delete warnings, missing-asset list.
7. Later: background loading (`decode` on a worker thread), reference index and "References" view, redirect table, hot reload, export (see `open-world-plan.md`: include everything first, reference walk later).
