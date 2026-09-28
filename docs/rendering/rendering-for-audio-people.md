# Rendering, explained through audio

A guide to how rendering works, for someone who knows audio. Related: [editor rendering](editor-rendering.md) (the engine's rendering code in detail), [sound engine](../sound-engine.md), and [procedural sound](../sound/procedural.md), which the core idea below builds on.

---

## The core idea: rendering is live procedural audio, for pictures

Music is usually played back from a recording. Rendering never is: **every frame is calculated from scratch,** like the live-generated wind in [procedural sound](../sound/procedural.md#level-b-generate-continuously-while-it-plays-harder). The game computes the next block of output just before the device needs it, forever.

| Audio | Rendering |
|---|---|
| a **sample**: one number, the speaker position | a **pixel**: one colour (4 numbers: red, green, blue, alpha) |
| sample rate, e.g. 48,000 per second | **resolution**, e.g. 1920×1080 pixels |
| a **buffer** of e.g. 1024 samples, filled just ahead of playback | a **frame**: a buffer of all the pixels (a *framebuffer*), filled just ahead of display |
| buffer refill rate: ~47 buffers/second | **frame rate**: 60 frames/second |
| stereo = 2 channels, interleaved (L R L R …) | RGBA = 4 channels, interleaved (R G B A R G B A …) |
| 16-bit samples | 8 bits per colour channel |
| the sound card pulls buffers at its own steady clock | the monitor shows frames at its own steady clock (60 Hz) |

---

## Timing: the same underrun problem

- **Buffer queueing = double buffering.** While the screen shows frame 1 (the *front* buffer), the game draws frame 2 into the *back* buffer. `swapBuffers()` is the audio "queue this buffer" call: the finished frame goes out, and the old one comes back to be drawn into.
- **VSync = locking to the device clock.** The swap waits for the monitor, as an audio thread waits for the card to ask for more.
- **A late frame = a buffer underrun.** In audio it's a click; in rendering it's a **stutter**: the same frame is shown twice.
- **Tearing = reading a buffer while it's being written.** The screen shows half of the old frame and half of the new one: the visual version of a glitch from a buffer changed mid-playback.

---

## Drawing = mixing

| Audio | Rendering |
|---|---|
| zero the mix bus before mixing | **clear** the framebuffer (`renderer.clear()`) |
| add a voice to the mix | **draw** a sprite into the framebuffer |
| wet/dry crossfade: `out = wet·mix + dry·(1−mix)` | **alpha blending**: `out = sprite·alpha + background·(1−alpha)`, e.g. semi-transparent smoke |
| summing signals, clipping above 1.0 | **additive blending**: fire and glows, "clipping" to white |
| a crossfade depends on the order it's applied in | alpha blending depends on **draw order**: things behind are drawn first. That's why the engine sorts sprites by Y, so the character in front is drawn last |

---

## Textures = recordings, sampling = resampling

- **A texture is a recording:** a stored grid of pixels (the PNG), like a stored list of samples (the OGG).
- **Texture coordinates = playback position.** A sprite says "play this texture from position 0.25 to 0.5": the four `texCoords` a `Sprite` stores, like a start and end offset in a sample.
- **Drawing a texture bigger or smaller = resampling:**
  - `GL_LINEAR` = **linear interpolation** between neighbouring samples: smooth, but blurry.
  - `GL_NEAREST` = **zero-order hold**, no interpolation: blocky. The engine's pixel art uses it, so pixels stay crisp squares.
- **Shrinking a texture a lot causes aliasing,** exactly like downsampling audio without a low-pass filter: shimmering, crawling patterns.
  - **Mipmaps** are the fix: pre-filtered, pre-downsampled copies at ½, ¼, ⅛ size. The same as band-limiting before decimation.
  - **Anti-aliasing (MSAA) = oversampling:** calculate more samples than are output, then filter down. Jagged edges ("jaggies") are aliasing on shape edges.

---

## Shaders = DSP plugins that run on every pixel at once

- **A shader is an effect plugin:** a small program applied to every sample. The difference is scale: a 1080p frame has about 2 million "samples", 60 times a second.
- **So there's a separate processor, the GPU:** a chip with thousands of small DSP cores, processing pixels in parallel.
- **The two kinds of shader:**
  - **Vertex shader = positioning:** it takes each sprite's corners and decides where on screen they land, like a panner deciding where a voice sits in the stereo field.
  - **Fragment shader = per-sample processing:** it runs once per pixel the sprite covers and computes its colour: reading the texture, tinting it, applying effects.
- **Uniforms = the plugin's knobs:** values set once per draw, the same for every pixel, e.g. the camera matrix or a tint colour.
- **Compiling a shader** is loading a plugin into the DSP: what `ShaderCodec.finish` does.

---

## The camera = the listener

- **The camera** decides what's seen, from where, as the listener decides what's heard. Moving the camera doesn't move the world: everything is transformed *relative to the camera*, exactly as OpenAL computes each source's position relative to the listener.
- **The projection** is the final mapping from world positions to the screen, like the panning law or HRTF mapping positions to two ears.
- **Zoom** is simply a scale in that mapping.

---

## Framebuffers = buses and sends

- **Rendering can go into an off-screen framebuffer** instead of the screen, as voices can be routed to an **aux bus** instead of the master.
- **Post-processing = master-bus effects:** render the whole world into a framebuffer, then run a full-screen shader over it: blur (a low-pass, in 2D), bloom, colour grading, a vignette.

---

## This engine's renderer, in audio terms

| Engine part | Audio equivalent |
|---|---|
| `Renderer` | the audio engine: owns the device (the GL context), knows how to mix |
| `Drawable` | something that can be mixed in: a voice |
| `SpriteBatcher` | **bouncing many voices into one buffer before sending**. Each draw call has a fixed overhead, like per-call overhead on an API, so hundreds of sprites are packed into one call |
| the layer stack passing a `RenderContext` and its framebuffer | an **insert chain on a bus**: the world layer writes into the framebuffer, and the editor layer takes that bus and adds its gizmos on top |
| `IdFramebuffer` (selection picking) | a **sidechain**: a hidden signal that's never heard, rendered only to analyse which object is under the mouse. Each object writes its id number instead of its colour |
| the selection outline shader | an effect on the sidechain: finds the edges of the selected object's area and draws a line there |
| `renderer.present()` + `swapBuffers()` | handing the finished buffer to the device |

---

## In one sentence

**Rendering is procedural audio in 2D, run 60 times a second on a massively parallel DSP chip**, with textures as recordings, shaders as plugins, blending as mixing, and the camera as the listener.
