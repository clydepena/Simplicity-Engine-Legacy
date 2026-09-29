# Rendering assets and tools

Status: **overview and plan, not started.** Related: [extensibility](extensibility.md) (what limits the renderer today, render and shader graphs), [editor rendering](editor-rendering.md) (the current code), [rendering for audio people](rendering-for-audio-people.md) (the concepts), [asset pipeline](../asset-pipeline.md) (loaders and savers), [sound assets and editors](../sound/assets-and-editors.md) (the same layering for sound).

Which assets rendering uses and produces, what the editing tools are, and how rendering becomes editable and data-driven.

---

## 1. The one pattern behind all of it

The engine keeps the **code that knows *how* to draw**; everything that says ***what* to draw and with which settings** becomes data (components and assets); the editor only edits that data. Every editable rendering feature has three parts:
```
engine code        knows HOW:      "draw a sprite with a tint and a flip"          (Java + GLSL)
data               says WHAT:      sprite = grass_03, tint = white, flipX = true   (a component or an asset)
editor UI          edits the data: an inspector with a sprite picker, colour picker, checkbox
```
The engine already does this for sprites: `SpriteRenderer` is data (a sprite and a colour), and `SpriteBatcher` plus the shader are the "how". Making rendering "editor-worthy" means doing that for more and more features, and **generating the editor UI from the data's description**, so each feature doesn't need its own hand-made panel.

It's the same layering as sound (clip -> sound event -> mixer -> graph). In rendering: **component settings -> materials -> graphs**, with shader code as the "scripting" underneath.

---

## 2. Assets from outside tools (load-only)

| Asset | Made in | Used for |
|---|---|---|
| **Images** (`.png`) | Aseprite, Krita, Photoshop | sprites, tiles, UI, backgrounds |
| **Shaders** (`.glsl`) | a text editor | how things are drawn: the "scripts" of rendering |
| **Fonts** (`.ttf`) | font foundries | text; turned into a glyph image (atlas) when loaded |
| **Normal maps, masks** (`.png`) | image tools, or generated | lighting on 2D sprites, special effects |

`TextureCodec` and `ShaderCodec` already cover the first two.

---

## 3. Assets made with the rendering tools (load and save)

| Asset | What it holds | Status |
|---|---|---|
| **Sprite sheet / atlas** (`.sheet`) | how an image is cut into sprites; names, pivots | **exists** (grid only) |
| **Material** | a shader plus its parameter values and textures: "glowing water", "flash white when hit" | missing: *the* central rendering asset in most engines |
| **Animation** (`.anim`) | frames from a sheet with timings; event markers (footstep on frame 3) | missing |
| **Tileset** | tiles with their sprites, collision shapes, and auto-tiling rules | planned ([open world plan](../open-world-plan.md) §4) |
| **Tilemap** | the painted grid of a world or cell | planned |
| **Particle system** | emitter settings: rate, lifetime, size and colour over life | missing |
| **Post-process profile** | screen effects: tint, darkness, vignette, bloom, colour grading | missing (the weather's "Screen Effects") |
| **Colour grading table (LUT)** | a small image that remaps all colours: a "night" or "sepia" look | missing |
| **2D light settings** | light colour, radius, falloff, shadows | missing |
| **Render textures** | a render target *as an asset*: a minimap camera draws into it, the UI shows it | missing |
| **Render graph / pipeline** | which passes run, in what order | missing ([extensibility](extensibility.md)) |
| **Shader graph** | a material's shader built from nodes | missing (later) |
| **Sorting layers** | project setting: Ground, World, Overhead | planned ([open world plan](../open-world-plan.md) §6) |

**Generated (baked) assets:** packing many small images into one atlas automatically, or a font's glyph atlas. Created by a tool, not edited by hand, and re-generated when their inputs change.

---

## 4. The levels of editability

**Level 1: components with settings (most of rendering editing).** Fixed features exposed as fields:
- `SpriteRenderer`: sprite, colour, flip, sorting layer, order
- `Camera`: zoom, pixel-perfect snapping, background colour
- `Light2D`: colour, radius, intensity
- `ParticleEmitter`, `ScreenEffects`

The inspector shows the fields, and the engine reads them each frame. No graphs, no scripts. Most of what artists do in Unity or Godot's 2D rendering is here.

**Level 2: materials, the step that makes looks *reusable*.** A shader is a program with *parameters* (uniforms); a material is **one set of values** for them:
```
shader:   sprite_flash.glsl        uniforms: uFlashColor (vec4), uFlashAmount (float), uTexture
material: enemy_hit.mat            flashColor = white, flashAmount = 0.8
material: frozen.mat               flashColor = light blue, flashAmount = 0.3
```
- **The editor builds the material inspector automatically** by asking the compiled shader which uniforms it has (`glGetActiveUniform`): a colour picker for a `vec4`, a slider for a `float`, a texture slot for a `sampler2D`.
- **A new shader's materials are editable immediately.** That generated UI is the key to "data-driven": no hand-made panel per effect.
- **A sprite points at a material** instead of the renderer using one shader for everything (limit #2 in [extensibility](extensibility.md)).

**Level 3: graphs.**
- **Shader graph:** build the shader itself from nodes, generating GLSL, for artists who don't write shaders.
- **Render graph:** arrange the passes of the frame (world -> bloom -> vignette -> UI).
- **Particle / VFX graph:** particle behaviour as nodes, beyond what the settings allow.

**"Scripting" in rendering is shader code.** GLSL is to rendering what the scripting language is to gameplay. Editing a `.glsl` file and seeing it update at once (`reload` on the shader asset, which the pool already supports) is the rendering equivalent of hot-reloading a script. Some engines also let scripts add custom render passes (a hook in the render graph).

---

## 5. The tools

| Tool | Edits | What it looks like |
|---|---|---|
| **Sprite editor** | sheets and atlases | the image with a grid or boxes over it; drag to slice; each sprite's name and pivot; 9-slice borders for UI panels |
| **Animation editor** | animations | a row of frames, a timeline with durations, a preview playing; markers for events |
| **Tile palette + tileset editor** | tilesets, tilemaps | pick a tile, paint it in the viewport; rule tiles (auto-tiling: grass edges pick themselves) |
| **Material inspector** | materials | generated from the shader's parameters, with a live preview swatch |
| **Shader editing** | `.glsl` | a text editor with hot reload; errors shown with their line |
| **Particle editor** | particle systems | settings with **curves** (size over life) and **gradients** (colour over life), with a live preview |
| **Post-process inspector** | profiles | sliders with the viewport updating live; "volumes" in the world where they apply |
| **Light tools** | 2D lights | light gizmos with radius circles in the viewport, like sound emitters |
| **Render graph editor** | the pipeline | nodes = passes, wires = images |
| **Shader graph editor** | material shaders | nodes = maths, with a preview on every node |
| **Frame debugger** | nothing: it *shows* | every draw call of one frame, in order, with the image after each step and the textures and shader used (like RenderDoc, or Unity's Frame Debugger) |
| **Stats overlay** | nothing | draw calls, batches, sprites, overdraw heat map |

**Shared widgets used by many tools:** a **curve editor**, a **gradient editor**, a **colour picker**, **asset slots** with previews. Built once, they're reused by particles, post-processing, lights, animation and sound.

**The frame debugger is worth building early.** It's mostly a list of the renderer's passes and draw calls plus the framebuffer images, which `Renderer` already routes through `begin` / `draw` / `end`. It turns "why is this sprite invisible?" from guessing into looking.

---

## 6. What to build first for a 2D open world

1. **Materials** plus the generated material inspector (shader introspection): everything visual builds on them.
2. **Animations** and an animation editor: every character needs them.
3. **Tilesets and the tile palette:** the open world is mostly tilemaps.
4. **The frame debugger and stats overlay:** cheap, and they make everything after easier to get right.
5. **Screen effects and post-process profiles:** the weather, and the world's overall look.
6. **Particles:** rain, fire, dust, spells.
7. **2D lights:** day/night and dungeons.
8. **Render graph, then shader graph:** once the passes and materials above exist and need arranging.
