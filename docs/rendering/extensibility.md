# How customizable the renderer is

Status: **assessment and plan, not started.** Related: [editor rendering](editor-rendering.md) (how the current rendering code works), [rendering for audio people](rendering-for-audio-people.md) (the concepts), [asset pipeline](../asset-pipeline.md) (loaders and savers for new asset types).

How freely modules can change what the renderer outputs, what limits that today, and what it would take to build rendering from building blocks in a node editor.

Assessed from `Renderer`, `Drawable`, `SpriteBatcher`, `RenderBatch`, `Framebuffer`, `Shader`, `shaders/default.glsl`, and their users: `World2DLayer`, `SelectionRenderer`, `ImGuiEditorLayer`, `TestLayerTriangle`.

**Short answer:** flexible for code. The selection outline proves it: a two-pass effect built entirely *outside* the renderer. It can't yet be driven by data, which a node editor needs. Most of the gap is a handful of hard-coded assumptions, not the architecture.

---

## 1. What's already flexible

| Feature | Where it shows | What it allows |
|---|---|---|
| **`Drawable`**: "anything that issues its own draw calls" | `SpriteBatcher implements Drawable` | new kinds of geometry without changing `Renderer` |
| **Passes:** set target, shader, camera, blending, clearing, then `begin` / `draw` / `end` | `SelectionRenderer` runs 3 different passes | any number of passes, in any order, into any target |
| **Full-screen passes:** `drawFullscreen(textures...)` | the outline shader reads the mask and draws the outline | post-processing: blur, colour grading, outlines, vignettes |
| **Render targets with formats:** `Framebuffer(w, h, format...)` | the id buffer stores integers, not colours | "hidden" data passes, like the picking sidechain |
| **Extra textures per draw:** `draw(drawable, extraTextureIds...)` | the mask pass reads the "which ids are selected" texture | a shader combining several inputs |
| **The layer chain can replace the image** | `ImGuiEditorLayer` swaps the framebuffer it passes on | inserting whole-screen processing between layers |
| **Each world owns its batcher** | `World2DLayer` owns a `SpriteBatcher` | several independent sets of sprites (worlds, previews, minimaps) |

So a new module that changes the output is possible today (a bloom layer, a water-reflection pass, a minimap render), as long as it's a `Drawable` or a layer written in Java, plus a shader.

---

## 2. What limits freedom today

1. **The vertex layout is fixed, and nothing checks it.**
   - Every sprite vertex is `pos(2) color(4) uv(2) texId(1) entityId(1)`, hard-coded in `RenderBatch`.
   - Any shader used on sprites must declare exactly those inputs at those locations; a mismatch just renders garbage (the `Drawable` doc says "nothing checks this").
   - Per-sprite extras (a normal map, a glow strength, a dissolve amount) would need a second batcher.
2. **One shader per pass, and no materials.**
   - The shader is chosen per *pass*, so every sprite in a world draws with the same shader.
   - "This one sprite glows" or "this enemy flashes white when hit" can't be expressed, except with a separate batcher and pass.
   - Shader parameters are uploaded by hand from Java code, by name.
3. **Blending is only on/off,** with one fixed formula (premultiplied alpha, set once in `init()`). No additive mode (fire, light), multiply (shadows) or screen; no scissor or stencil. All small additions, but today a pass can't choose them.
4. **Shaders must use the names `uProjection` and `uView`:** `begin()` uploads the camera by those exact names.
5. **A render target has exactly one colour image, plus a fixed depth buffer.**
   - No rendering colour + normals + glow in one pass (multiple render targets).
   - No pool of temporary targets: a chain of post effects would create and destroy framebuffers by hand (as `ImGuiEditorLayer` does for its UI frame).
6. **Texture units are hard-coded:** 0–7 for sprite batches, extras from 8.
7. **A pass is not an object.** It's a sequence of setter calls on the renderer's global state, then `begin()`: it can't be stored, listed, saved or rearranged. **For a node editor this is the main blocker:** there's nothing for a node to *be*.
8. **The frame's structure is code:** which passes run, in which order, into which targets, is written inside each layer's `onRender`.
9. **Shaders can't describe themselves:** there's no way to ask a `Shader` "what parameters do you have?", which an editor needs to show sliders automatically. OpenGL can answer it (`glGetActiveUniform`); nothing asks yet.
10. **Small leftovers:** the default shader still comes from the legacy `util.AssetPool`, and debug line drawing (`DebugDraw`) isn't ported yet.

### Scorecard

| Area | Today |
|---|---|
| New geometry types (in Java) | good: implement `Drawable` |
| Multi-pass effects (in Java) | good: proven by the outline |
| Post-processing (in Java) | good: `drawFullscreen` + framebuffers |
| Per-object look (materials) | missing |
| Blend modes / render states | minimal: on/off only |
| Custom sprite data | blocked by the fixed layout |
| Describing a frame as data | missing: passes are setter calls, not objects |
| Editor-driven (no Java code) | not possible yet |

---

## 3. Could it become node-based? Two different "render node editors"

**A. Render graph / compositor: nodes are passes and images.** Like Blender's compositor, or Unity's and Godot's render-pipeline graphs.
```
[World sprites] ──▶ [Bright parts] ──▶ [Blur ×2] ──┐
        │                                          ├──▶ [Add] ──▶ [Vignette] ──▶ [Screen]
        └──────────────────────────────────────────┘
```
Each node is a pass ("draw these drawables into a target", or "run this full-screen shader on these images"), and the wires are images (render targets). The natural next step for this engine: it's what `SelectionRenderer` already does by hand.

**B. Shader graph / material editor: nodes are maths inside one shader.** Like Unity's Shader Graph or Unreal's material editor.
```
[Texture sample] ──▶ [× Colour] ──▶ [+ Rim glow] ──▶ [Output colour]
          [Time] ──▶ [Sine] ──────────┘
```
The graph is turned into GLSL source, compiled with `ShaderCodec`, and hot-reloaded through the pool. More work (code generation, type-checking the wires), and it only pays off once materials exist.

### What A needs, mapped onto the limits in section 2

1. **`RenderPass` as an object (fixes 7):** target, shader or material, camera, blend mode, clear, and what to draw; `Renderer` gains `execute(RenderPass)`. The current setters can stay, implemented on top of it.
2. **Blend modes as an enum (fixes 3):** alpha, additive, multiply, opaque.
3. **Materials (fixes 2 and 9):** a shader plus parameter values, as an asset with a loader and a saver. With shader introspection, the editor shows each parameter automatically as a slider or colour picker.
4. **A pool of render targets (fixes 5):** "a screen-sized RGBA target" requested by description and reused between frames, not created by hand.
5. **Named drawable sets:** "world sprites", "UI", "selection", so a node can say *what* to draw by name.
6. **The graph itself:** nodes, wires, a topological sort, and an executor that runs the passes each frame. Saved as an asset (JSON) with a codec, as `.sheet` files are.
7. **The editor:** `NodeEditorPanel` already exists, built on imgui's node-editor extension with a `Graph` model, so the UI side has a head start.

---

## 4. Order of work

| Step | Gives | Needs a node editor? |
|---|---|---|
| 1. Blend modes + `RenderPass` object | per-pass freedom, passes as data | no |
| 2. Materials (+ shader introspection) | per-object looks, auto-generated parameter UIs | no |
| 3. Render-target pool + named drawable sets | cheap post-effect chains | no |
| 4. Render graph built in code, executed each frame | the frame's structure in one place | no |
| 5. Graph saved as an asset + node editor | rendering changed without Java code | yes |
| 6. Shader graph (GLSL generation) | new effects without writing GLSL | yes |
| (separately) a second vertex layout / batcher for extra sprite data | normal maps, dissolve, per-sprite parameters | no |

Steps 1–3 improve the renderer even without any editor, and steps 4–6 build on them.
