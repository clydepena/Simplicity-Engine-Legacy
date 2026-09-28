# Sound engine: plan

Status: **planned, not started.** Related: [asset pipeline](asset-pipeline.md) (loaders, the pools, disposal).

How the engine plays sound: an audio system that owns OpenAL (as `Renderer` owns OpenGL), audio clips as assets, players that use them, and the designed sound assets and editors on top. Sound is the last asset type still loaded only by the legacy `util.AssetPool`; once it moves, that pool can be deleted.

This file is the overview. Each topic has its own file in `sound/`:

| File | What's in it |
|---|---|
| [How a sound system works](sound/how-it-works.md) | the idea: sounds are started, not redrawn; clip, source, listener; what game code looks like; compared with the renderer |
| [OpenAL: devices, distance and effects](sound/openal.md) | choosing the output device; distance, direction and Doppler; EFX filters and effects; where effect data lives |
| [Other audio libraries](sound/libraries.md) | Java Sound, FMOD, and the rest compared; why OpenAL Soft stays |
| [Where the code is today](sound/current-code.md) | `Window`'s OpenAL setup and `simplicity.Sound`, and their problems |
| [Runtime: AudioSystem, AudioClip, SoundSource](sound/runtime.md) | the code design of the system, the clip asset and its codec, and the player |
| [Assets and editors](sound/assets-and-editors.md) | sound events, the mixer, presets, music; the editors; the four levels of control, from code to node graphs |
| [Procedural sound](sound/procedural.md) | sound calculated by code: building blocks, techniques, and how the engine could do it (generated clips, or live streaming) |

Reading order for someone new to audio: how it works -> OpenAL -> runtime -> assets and editors -> procedural sound.

---

## Decisions

- **The library stays OpenAL Soft** (through LWJGL), with EFX for effects ([libraries](sound/libraries.md)).
- **An `AudioSystem` owns OpenAL, the way `Renderer` owns OpenGL.** `Application` creates it, calls `init()` after the window and `destroy()` before it, and gives it to layers the same way it gives them the renderer ([runtime](sound/runtime.md#audiosystem)).
- **The asset is only the audio data** (`AudioClip`: an OpenAL buffer). It's shared through the pools like every other asset.
- **Playing is separate** (`SoundSource`: an OpenAL source). One per thing that makes a sound; many can play the same clip at once.
- **No audio device is not an error.** The engine starts without sound, logs it once, and every audio call does nothing.
- **Effect settings are assets, effects are runtime objects.** A preset (e.g. `cave.reverb`) is plain data with a loader and a saver; the audio system creates and updates the OpenAL effects from it ([OpenAL](sound/openal.md#where-effect-data-lives-both-at-two-levels)).
- **Game code plays sound events, not files,** once events exist ([assets and editors](sound/assets-and-editors.md)).
- **Control is data first:** events, then parameters and curves; node graphs only if those run out.
- Naming follows Unity (`AudioClip` for data, `SoundSource` for the player); see the open decisions.

---

## Open decisions

1. **Names:** `AudioClip` + `SoundSource` (recommended), or `Sound` + `SoundPlayer`.
2. **Where `AudioSystem` is reachable from:** passed like the renderer (through `Application` and the render context), or a single static instance like `Tasks`. Passing it matches how the renderer is handled.

---

## Later

- **Music streaming:** long files decoded a few seconds at a time into a small ring of buffers, refilled in `AudioSystem.onUpdate()` (`stb_vorbis_open_memory` + `stb_vorbis_get_samples_short_interleaved`). Needed once music exists.
- **Positional sound:** listener at the camera (slightly above the map), sources at their objects, a distance model, Doppler. Mono clips only ([OpenAL](sound/openal.md#distance-and-direction-openal-calculates-them)).
- **Mixer groups and effect presets:** separate volumes for music, effects and UI; EFX effect chains per group; presets editable while the game runs ([assets and editors](sound/assets-and-editors.md)).
- **Procedural sound:** first generated clips from a recipe asset (sfxr-style `.sfx`), then live generation on top of music streaming ([procedural sound](sound/procedural.md)).
- **Occlusion:** muffling sounds behind walls: a ray through the tilemap, then a low-pass filter.
- **Device choice and changes:** an "Output device" setting, and headphones unplugged while running: OpenAL Soft reports it (`ALC_CONNECTED`) and can move to another output with `ALC_SOFT_reopen_device` without losing clips ([OpenAL](sound/openal.md#choosing-the-sound-hardware)).

---

## Order of work

1. **`AudioSystem`**: move the OpenAL setup out of `Window`, handle a missing device, call it from `Application.run()` in the right order.
2. **`AudioClip` + `AudioCodec`** (decode on a worker, upload on the main thread). Test with a short `.ogg` added to the resources.
3. **`SoundSource`**, with the rewind and cleanup bugs fixed.
4. **Remove `simplicity.Sound`** and the sound methods of the legacy `util.AssetPool` (nothing uses them).
5. Then the legacy pool can be deleted, once textures, shaders and sheets are switched over ([asset pipeline](asset-pipeline.md)).
6. One-shot sources and a sound component.
7. The designed-sound work: sound events, the mixer, emitters and zones, parameters and curves, in the order listed in [assets and editors](sound/assets-and-editors.md#order).
