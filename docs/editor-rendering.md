# Editor rendering

## Sprites: `Drawable` and `SpriteBatcher`

`Renderer` holds GPU state and passes only: target framebuffer, shader, camera, blending, clearing, `begin()`/`end()`, `drawFullscreen` and `present`. It holds no content. What gets drawn is a `Drawable`, passed to `renderer.draw(drawable, extraTextureIds...)` inside a pass; the drawable issues its own draw calls with the pass's shader.

`SpriteBatcher` is the sprite `Drawable`. It owns a set of `RenderBatch`es: sprites with the same zIndex share batches (up to 1,000 sprites and 7 textures each), drawn in zIndex order, one draw call per batch.

- Whoever owns the content owns its batcher. `World2DLayer` owns its sprites (`world.sprites()`), adding and removing them as objects join and leave; attaching or detaching the layer doesn't touch them.
- `sync()` runs once per frame before drawing (the world calls it at the start of `onRender`, so it also runs while frozen). It moves sprites whose zIndex changed and frees empty batches.
- `draw()` only draws, after uploading sprites that changed, so any number of extra passes over the same batcher (like the selection passes below) can't restructure it.
- `Drawable` doesn't check that the shader matches the vertex layout. Code that depends on the sprite layout (entity id in attribute 4) takes a `SpriteBatcher`, not any `Drawable`.

## Editor camera

The viewport owns an `EditorCamera` (`editor/EditorCamera.java`), a plain editor class wrapping its own `Camera`, so editing never moves the game's camera.

- **Which camera draws:** `World2DLayer.setViewCamera(camera)` makes the world draw through another camera; `null` goes back to the game camera. `renderCamera()` returns whichever is in use, and the viewport picks and outlines through it. While editing, `ViewportPanel.onUpdate` sets the editor camera; while playing, it clears it.
- **Start:** when the world's game camera changes (a new scene), the editor camera copies its position and zoom.
- **Mouse to world:** `Camera.viewportToWorld(u, v)` turns a 0..1 point on the target (bottom-left origin) into a world position. The camera's origin is the view's bottom-left corner; the view spans `position .. position + projectionSize * zoom`, and a larger zoom shows more.

Input, in `ViewportPanel.handleCameraInput`, only while editing:

| Input | Effect |
|---|---|
| Middle-drag | Pans; the grabbed point stays under the mouse. Screen pixels become world units through the part of the frame the image shows. The invisible button over the image also responds to the middle button, so a pan keeps the mouse when it leaves the panel. Its flags are built as `(1 << ImGuiMouseButton.Left) | (1 << ImGuiMouseButton.Middle)`: imgui-java 1.86's `ImGuiButtonFlags.MouseButtonLeft` is `0` (Dear ImGui's is `1 << 0`), so using it leaves the left button out. |
| Scroll wheel (over the image) | Zooms toward the mouse: each notch multiplies the zoom by 1.1 (in) or divides by it (out), clamped to 0.05..20, then moves the camera so the world point under the mouse stays put. |
| Numpad `.` (viewport hovered or focused) | `resetView()`: eases the position back to (0, 0) with the legacy editor camera's growing lerp, finishing within about a third of a second. The zoom is kept. |

Camera input runs last in `onRender`, after the outline. The world and the outline are both drawn through the camera as it was at the start of the frame, and changes show from the next frame, so the outline never lags the sprites.

## Gizmos

`editor/Gizmo.java` is the transform gizmo, a plain editor class owned by `ViewportPanel`. It draws with the viewport window's ImGui draw list and hit-tests in screen pixels, both from the same size constants, so what is drawn is exactly what can be grabbed. It adds no world objects, sprites or GPU passes, and nothing of it is saved. It is shown on the last selected live object, and hidden while playing.

| Tool (key) | Handles | Drag writes |
|---|---|---|
| Translate (W) | X arrow, Y arrow (world axes), centre square | `transform.position`: along X, along Y, or freely |
| Rotate (E) | ring around the object, with an orientation line from the centre to a dot on the ring at the object's rotation (0° = right, counter-clockwise) | `transform.rotation`: the angle swept around the centre; Ctrl snaps to 15° steps. While dragging, a translucent wedge shows the sweep since the press (snapped too; past a full turn it starts over, showing only the part of the current turn), a faint line marks the start angle, and a label under the ring shows this drag's change and the total, e.g. `+30.0° (120.0°)`. |
| Scale (R) | X and Y handles with square ends, along the object's rotated axes; centre square | `transform.scale`: X, Y, or both. Dragging one axis length (80 px) further doubles it. Minimum 0.001. |

- **Colors:** X red, Y green, centre light grey, ring blue. A handle is brighter when hovered and darker while dragged; while one handle is dragged, the others fade.
- **Hit priority:** the centre square, then the X and Y handles; the rotate tool only has the ring.
- **Size:** constant in screen pixels at any zoom, clipped to the image.

**Which input owns a press:** a left press on a hovered handle starts a gizmo drag, which owns the press until release (`handleGizmoPress` runs before `handleSelectionInput`, and selection only starts when no gizmo drag did). So dragging a gizmo never selects anything. W, E and R switch tools while the viewport is hovered or focused and nothing is being dragged.

**Drag math, anchored at the press:** `begin()` stores every selected object's transform, plus the mouse's world point (translate), screen point (scale) and angle around the centre (rotate). Each frame computes the total change since the press and applies it to every target's start values, so there is no drift. Several selected objects get the same offset, angle or scale factor; each rotates and scales around its own centre.
- Rotate: the angle is counter-clockwise on screen, matching `Transform.rotation` in the y-up world. It is unwrapped across ±180°, so full turns keep counting.
- Escape cancels a drag and restores the start values. It is checked before the release: with keyboard navigation on, Escape also makes ImGui release the active item in the same frame.

**Frame order:** the gizmo is drawn with this frame's transforms, and the drag is applied last in `onRender`, like camera input. The world, the outline and the gizmo all show the change from the next frame. There, `world.onEditorUpdate` runs `SpriteRenderer.editorUpdate`, which sees the transform change and marks the sprite dirty. That relies on `Transform.copy` copying every field `equals` compares, including rotation and zIndex.

**World ↔ screen:** `ViewportPanel.gizmoView` maps through `world.renderCamera()` (`viewportToWorld` / `worldToViewport`) and the part of the frame the image shows (`uvMin`/`uvMax`).

Gizmo edits change the scene's own transforms immediately. There's no undo yet, and physics bodies aren't moved (see the Play/Stop scene restore work).

## Selection: picking and outlines

Selecting objects in the viewport and outlining them is handled by `editor/SelectionRenderer.java`, which `ViewportPanel` owns. It is **not** a second renderer: like `World2DLayer`, it is a client of the default `Renderer`. It has no batches, sprite list or draw loop of its own. It only owns its offscreen buffers and shaders, and drives the existing `Renderer` API with different settings. The world knows nothing about selection.

`SelectionRenderer` works purely in uids, and every call takes the renderer, the target framebuffer and the camera:

```java
int          pick(renderer, target, camera, x, y)                        // uid, or -1
Set<Integer> pickRect(renderer, target, camera, x0, y0, x1, y1,
                      includeHidden, fullyInside, maxUid)
void         drawOutline(renderer, target, camera, Collection<Integer> uids)
```

Pixel coordinates are in the target's pixels, bottom-left origin, and its buffers follow the target's size. It never sees a `GameObject` or a world: the caller maps uids to objects and does the `NonPickable` and `isDead` checks. Every call also takes the `SpriteBatcher` to work on (the viewport passes `world.sprites()`), so it works for any view of any sprite set: a second viewport with its own camera and target, or another world's sprites. Only sprites can be picked, since the id comes from each sprite's vertices.

### The vertex data already carries entity ids

Every vertex `RenderBatch` builds has this layout:

```
pos(2) | color(4) | texCoords(2) | texId(1) | entityId(1)   ← entityId = uid + 1
```

`default.glsl` ignores `entityId`, while the editor shaders use it. So the same batches can be drawn three ways just by swapping the shader:

| Shader | Draws into | Output per pixel |
|---|---|---|
| `shaders/default.glsl` (world) | world frame | the sprite's color |
| `shaders/editor/pickingShader.glsl` | `IdFramebuffer` (float RGB) | the topmost sprite's entity id, or 0 |
| `shaders/editor/selectionMask.glsl` | mask `Framebuffer` | 1 if the sprite is selected, else nothing |

All three drop nearly transparent pixels the same way: the picking and mask shaders discard `alpha < 0.5`, and the default shader's fully transparent pixels draw nothing. Clicks and outlines therefore follow the sprite's visible shape, not its rectangle.

### What it uses from `Renderer`

Almost everything is existing API:

- `setFramebuffer`, `setShader`, `setCamera`, `setBlending`, `setClearing`, `setClearColor`
- `begin()`, `draw(drawable)`, `end()`, where the drawable is the world's `SpriteBatcher`

Additions made for it:

- `Renderer.drawFullscreen(int... textureIds)`: draws one triangle covering the target, built from `gl_VertexID`, with an empty VAO. Used by the outline pass.
- `Renderer.draw(drawable, int... extraTextureIds)`: optional extra textures, bound to units `Renderer.FIRST_EXTRA_TEXTURE_UNIT` (8) and up for the whole draw. Units 0..7 belong to the sprite batches' textures.
- `Renderer` getters (`getShader`, `getCamera`, `isBlending`, `isClearing`, `getClearColor`), so passes can save and restore the renderer's state.
- A `Framebuffer`/`Texture` constructor that takes a format and filter. `IdFramebuffer` is a `Framebuffer` subclass that uses it for float storage (`GL_RGB32F`, nearest) and adds `readPixel` and `readRect`.
- `IdSetTexture`: a set of entity ids stored as a lookup texture (see the outline section).
- `IdFlagBuffer`: per-entity-id flags in a shader storage buffer, for rectangle picking modes that must see covered sprites (see rectangle picking).

The only raw GL outside `Renderer` is in the `renderer` package classes built for this: `glReadPixels` in `IdFramebuffer`, the texture upload in `IdSetTexture`, and the storage buffer in `IdFlagBuffer`.

### Picking (only on a click)

1. `ViewportPanel` converts the mouse position to a frame pixel using `uvMin`/`uvMax`, then flips Y, because OpenGL's origin is bottom-left.
2. `SelectionRenderer.pick()` renders every sprite with the picking shader, through the given camera, into an `IdFramebuffer` the size of the target (created on first use, replaced when the size changes). It clears to 0 and turns blending off, so ids are never averaged together. Batches are drawn in zIndex order, so the topmost sprite writes last and wins.
3. `IdFramebuffer.readPixel(x, y)` reads that one pixel. The value minus 1 is the uid that `pick()` returns; the viewport turns it into the object with `world.getGameObject(uid)`.

### Selecting in the viewport: click and drag-select

`ViewportPanel.handleSelectionInput` turns mouse input into picks. This only happens while editing. While playing, the viewport is the game view: clicks and drags don't select (they still reach the world), no outline is drawn, and the selection is kept for when you stop.

1. An `ImGui.invisibleButton` covers the image, so the viewport owns the mouse while a press is held, even if the mouse leaves the panel.
2. A press on the image records the press position.
3. Moving past ImGui's drag threshold (`io.MouseDragThreshold`, 6px by default) turns it into a drag. The rectangle is drawn with the window's ImGui draw list, clipped to the image, so the world's frame is untouched. Its end corner is clamped to the image.
4. On release, a drag calls `pickInScreenRect`; a click calls `pickAtScreenPoint` at the press position. A click on a `NonPickable` object leaves the selection alone.
5. Escape cancels a press or drag.

The pick is applied with `EditorSelection.apply(mode, objects)`. The mode comes from the modifiers at release, checked in this order:

| Modifier | Mode | Effect | Rectangle color |
|---|---|---|---|
| Ctrl | `TOGGLE` | flips each picked object | yellow |
| Alt | `SUBTRACT` | removes the picked objects | red |
| Shift | `ADD` | adds the picked objects | orange |
| none | `REPLACE` | the pick becomes the selection; an empty pick clears it | orange |

Because an empty pick only changes a `REPLACE` selection, a click on empty space clears the selection, while a modified click on empty space leaves it as is.

Selection happens on release, which relies on ImGui seeing quick clicks. imgui-java 1.86's GLFW backend sets `MouseDown` in `newFrame()` from its own "just pressed" latch or the live GLFW button state, so `ImGuiEditorLayer.onNotify` forwards button events to `imGuiGlfw.mouseButtonCallback`, which sets that latch. A press and release that arrive before the same frame then still register as a click.

### Rectangle picking

`SelectionRenderer.pickRect(renderer, target, camera, x0, y0, x1, y1, includeHidden, fullyInside, maxUid)` returns the uids in the rectangle between two target pixels (inclusive, bottom-left origin, corners in any order). Two flags choose the rule:

- `includeHidden`: false = only objects with a visible pixel in the rectangle; true = also objects fully covered by others.
- `fullyInside`: false = any pixel inside is enough (partial, the default); true = the object's whole shape must be inside. The whole shape includes covered parts, so a sprite whose hidden half sticks out doesn't count.

For every mode, only pixels that pass the alpha cutout count, so the transparent corners of a sprite's quad don't select it.

| `includeHidden` | `fullyInside` | Selected when | Passes |
|---|---|---|---|
| false | false (default) | a visible pixel is inside | id buffer only |
| true | false | any pixel is inside, covered or not | flag pass |
| true | true | the whole shape is inside | flag pass |
| false | true | the whole shape is inside and a visible pixel is inside | id buffer + flag pass |

**Id buffer:** the same one `pick()` uses. `IdFramebuffer.readRect` reads the rectangle's id channel (`GL_RED`) in one `glReadPixels` and collects the distinct uids. The rectangle is clamped to the buffer; one fully outside finds nothing.

**Flag pass:** `shaders/editor/selectionFlags.glsl` draws every sprite (there's no depth test, so covered sprites run too) into an `IdFlagBuffer`, a shader storage buffer with one `uint` of flags per entity id, set with `atomicOr`:

- bit 1: a pixel of the object passing the alpha cutout lies inside the rectangle
- bit 2: a pixel lies outside the rectangle, or a corner of the object's quad is off the frame

The second part of bit 2 is in the vertex shader. The GPU never rasterizes pixels off the frame, so without it a sprite sticking out of the frame's edge could look fully inside a rectangle touching that edge. It uses the quad's corners, so it is slightly conservative.

The buffer is sized from the `maxUid` the caller passes (the viewport uses the largest uid in the world) and cleared before each pick. `read()` issues `glMemoryBarrier(GL_BUFFER_UPDATE_BARRIER_BIT)` before reading, since shader writes aren't otherwise visible to buffer reads. The pass writes no useful color; it targets the outline's mask framebuffer, which `drawOutline` clears before using.

In the viewport, `rectIncludeHidden` and `rectFullyInside` (both false) hold the modes, and `pickInScreenRect(renderContext, a, b)` converts two screen points to frame pixels, calls `pickRect` with them, and turns the uids back into objects in one pass over the world, leaving out `NonPickable` ones. Drag-select calls it on release. For the outline, `selectedUids()` collects the uids of the live selected objects each frame.

### Outline (every frame while something is selected)

1. **Mask pass:** every sprite is drawn with the mask shader, which checks its entity id against the selection. Only selected sprites write 1. Because this pass ignores what's on top, the whole silhouette is kept even when another sprite covers it. It is always one pass, however many objects are selected.

   The selection reaches the shader as an `IdSetTexture`, a lookup texture where texel `id` is 1 if that entity id is selected. `renderer.draw(sprites, outlinedIds.getTexId())` binds it to unit 8 as `uSelected`. Details:

   - Ids are laid out in rows of `IdSetTexture.ROW_WIDTH` (1024), so id `n` lives at `(n % 1024, n / 1024)`. The shader reads the width from `textureSize`, and ids past the last row count as not selected.
   - The texture grows (doubling its rows) to fit the largest selected id.
   - `set(ids, count)` compares with what is already uploaded and only re-uploads when the selection changed, so a steady selection costs no uploads.
2. **Edge pass:** `drawFullscreen` runs `shaders/editor/selectionOutline.glsl` over the world frame. For each pixel outside the mask, it checks the neighbors within the outline thickness (2px by default). If any are inside the mask, it writes the outline color (orange by default), premultiplied to match the renderer's `GL_ONE, GL_ONE_MINUS_SRC_ALPHA` blending. Everything else is discarded, so the world image is untouched.

The color and thickness can be changed with `setOutlineColor` and `setOutlineThickness`.

### Why it doesn't conflict with the world's rendering

1. **Order:** layers render bottom to top. The world finishes its whole `begin`/`draw`/`end` before the editor layer's `onRender` starts, and the viewport's GL work happens inside that. ImGui only samples the world frame later, in `renderDrawData`, so it already sees the outline.
2. **Separate targets:** the picking and mask passes draw into the editor's own framebuffers; only the edge pass touches the world frame. `begin()` binds the target and sets `glViewport` to its size, and the buffers are kept at the world frame's size, so pixel coordinates line up.
3. **State is restored:** `SavedState` records the framebuffer, shader, camera, blending, clearing and clear color before each pass and restores them after. The next layer and the next frame see the renderer as the world left it. This matters because `Application` uses the renderer's clear color on the main frame each frame.
4. **Pass guards still apply:** each pass is its own `begin()`/`end()`. `checkNotInPass`/`checkInPass` make accidental nesting or mid-pass state changes throw instead of corrupting a draw.
5. **Drawing the sprites again is safe:** the world calls `sprites.sync()` once per frame before its pass (moving zIndex changes, dropping empty batches), and its pass uploads changed sprites and clears their dirty flags. The extra `draw(sprites)` calls only draw; `SpriteBatcher.draw` never adds or removes sprites.

### Costs

- **Picking:** `glReadPixels` makes the CPU wait for the GPU, which is why the id buffer is only rendered on the click frame, never every frame.
- **Rectangle picking:** the same wait, plus reading the rectangle's pixels: about 8MB for a full 1920×1080 frame, since only the id channel is read. The non-default modes add one more draw of all sprites and a small buffer read. Fine once per drag, not every frame.
- **Outline:** one extra draw of all sprites plus one full-screen pass, which checks up to 13 neighbors per pixel at 2px thickness. The mask pass costs the same for 1 or 10,000 selected objects. That's trivial for a 2D scene, and nothing runs when nothing is selected. The known improvement, if the full-screen pass ever shows up in profiling, is limiting it to the selection's bounds with a scissor rectangle.
