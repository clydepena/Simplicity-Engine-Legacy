# Other audio libraries for Java

Part of the [sound engine plan](../sound-engine.md). Related: [OpenAL: devices, distance and effects](openal.md).

Why the engine stays with OpenAL Soft, and what else exists.

---

| Library | What it is | 3D / effects | Notes |
|---|---|---|---|
| **LWJGL OpenAL (OpenAL Soft)** | what the engine uses | positions, distance, Doppler, HRTF; EFX effects | already a dependency; the usual choice for LWJGL engines |
| **Java Sound** (`javax.sound.sampled`, built into Java) | `Clip`, `SourceDataLine`, volume/balance controls | no 3D, no effects | no extra libraries; reads WAV/AIFF/AU only (Ogg needs a plugin); higher latency; fine for tools, weak for games |
| **FMOD** (bindings in LWJGL 3.3+: `lwjgl-fmod`) | professional audio engine, with FMOD Studio as the sound designer's editor | everything, designed in the editor | LWJGL only has the bindings; FMOD's libraries come from FMOD under their licence (free below a budget limit for indies; check the current terms) |
| **libGDX audio**, **jMonkeyEngine audio** | the audio parts of those frameworks | OpenAL underneath | come with the whole framework: not a real alternative for this engine |
| **Paul Lamb's SoundSystem**, **TinySound** | older small wrappers over OpenAL / Java Sound | basic | early Minecraft used SoundSystem; not maintained |
| **Minim, Beads** | libraries for music and interactive sound (Processing heritage) | synthesis, effects | for audio-heavy creative projects, not game engines |
| **miniaudio, SoLoud, Steam Audio, Wwise** | C/C++ libraries | varies (Steam Audio: realistic 3D acoustics) | **no official Java bindings:** the binding would have to be written (JNI, or LWJGL-style with the Foreign Function API) |

## Decision: stay with OpenAL Soft

- It's already a dependency, and LWJGL keeps it current.
- It covers what a 2D open world needs: positional sound, distance falloff, reverb and filters (EFX), and HRTF for headphones.
- **FMOD** becomes worth it if a dedicated sound designer joins: its editor is the real reason to use it. The asset and editor design in [assets and editors](assets-and-editors.md) borrows its ideas (events, buses, parameters), so switching later wouldn't change how game code plays sound.
- **Java Sound** is only a fallback for zero native dependencies.
