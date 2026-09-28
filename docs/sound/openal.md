# OpenAL: devices, distance and effects

Part of the [sound engine plan](../sound-engine.md). Before: [how a sound system works](how-it-works.md). Related: [other audio libraries](libraries.md), [assets and editors](assets-and-editors.md).

Who decides what. LWJGL ships **OpenAL Soft**, which supports everything below. In each case the engine decides *what* should happen, and OpenAL does the actual sound processing.

---

## Choosing the sound hardware

The audio equivalent of choosing the window and GL context for the renderer, in two steps.

**Device = which output** (speakers, headphones, an HDMI monitor, a USB headset):
```java
// every output the system has, e.g. "Speakers (Realtek)", "Headphones (USB)"
String all = alcGetString(0, ALC_ALL_DEVICES_SPECIFIER);   // names separated by '\0'

long device = alcOpenDevice((String) null);                 // null = the system's default output
// or:  alcOpenDevice("Headphones (USB)");                  // a specific one, e.g. picked in a settings menu
```
`Window` currently asks for the default device's name and opens that. An "Output device" list in a settings menu is this enumeration.

**Context = how the device is used,** like a GL context. Attributes passed when it's created:
- **sample rate:** `ALC_FREQUENCY`, e.g. 48000
- **how many sounds can play at once:** `ALC_MONO_SOURCES`, `ALC_STEREO_SOURCES`
- **HRTF on/off:** `ALC_HRTF_SOFT`, which makes headphones sound 3D

**Switching device while running:** OpenAL Soft's `ALC_SOFT_reopen_device` extension moves an open device to another output **without losing loaded clips or playing sources**. It's the tool for unplugged headphones, or a different output picked in the settings.

---

## Distance and direction: OpenAL calculates them

The engine only says **where things are**; OpenAL works out loudness and left/right balance every time it mixes:
```java
alListener3f(AL_POSITION, camX, camY, 0);         // the ear
alSource3f(source, AL_POSITION, x, y, 0);         // each speaker
```

**How loudness falls off with distance:** the formula is chosen once, and tuned per source.

| Setting | Meaning |
|---|---|
| `alDistanceModel(...)` | the formula: `AL_INVERSE_DISTANCE_CLAMPED` (default, realistic), `AL_LINEAR_DISTANCE_CLAMPED` (fades to silence at a set distance, easiest to design with), `AL_EXPONENT_DISTANCE_CLAMPED` |
| `AL_REFERENCE_DISTANCE` | up to this distance, the sound plays at full volume |
| `AL_MAX_DISTANCE` | beyond this, it stops getting quieter (or is silent, with the linear model) |
| `AL_ROLLOFF_FACTOR` | how quickly it fades: 0 = never, higher = faster |

**Doppler effect:** with a velocity on sources and the listener (`AL_VELOCITY`), OpenAL raises the pitch of approaching sounds and lowers it for sounds moving away (a car passing). Tuned with `alDopplerFactor` and `alSpeedOfSound`.

**The engine's part:**
- **Every frame, pass positions and velocities** (the `onUpdate` job).
- **Units:** OpenAL has none, so pick a scale (e.g. 1 world unit = 1 metre) and tune the distances to it.
- **Top-down 2D:** put the listener slightly *above* the map (e.g. z = 5). Otherwise a sound just a bit to the right plays 100% in the right ear and switches sides abruptly as the player passes it.
- **Positional sounds must be mono.** OpenAL plays stereo clips as they are, without positioning them: footsteps should be mono files; music can be stereo.

**What OpenAL doesn't do: walls.** A sound behind a wall should be muffled, but OpenAL doesn't know the walls. The engine finds out itself (e.g. a ray from the listener to the source through the tilemap), then *applies* the muffling with a low-pass filter (below). Engines differ most in this kind of acoustic work.

---

## Effects beyond volume: EFX

Core OpenAL only has **volume** (`AL_GAIN`) and **pitch** (`AL_PITCH`, which also changes speed). Everything else comes from the **EFX extension**, which OpenAL Soft implements; LWJGL exposes it as `EXTEfx`. It has two kinds of processing.

**Filters: per source, on the sound itself**
- **low-pass:** muffled; behind walls, underwater
- **high-pass:** thin; a radio or phone
- **band-pass:** both at once

**Effects: shared "effect slots" that sources send part of their sound into**

| Effect | Typical use |
|---|---|
| Reverb / EAX reverb | caves, halls, rooms: the most used one |
| Echo | canyons |
| Distortion | radios, damage, broken speakers |
| Equalizer (4 bands) | shaping low, mid and high frequencies |
| Chorus, flanger | voices, magic |
| Compressor | evening out loud and quiet |
| Pitch shifter, ring modulator, autowah, frequency shifter, vocal morpher | creature voices and stylised effects |

Every parameter can change **while sounds play**: fading reverb up as the player walks into a cave, turning the low-pass up as they dive underwater.

**Who decides what:**
- **Game code decides *when*:** "the player is in a cave", "the player is underwater".
- **The audio system decides *how*:** it offers presets or buses (a "cave" reverb) and sends groups of sounds through them. This is where mixer groups (music / effects / UI) come in.
- **OpenAL Soft does the sound processing.**

**Beyond OpenAL:** FMOD and Wwise put this in a separate editor, where a sound designer builds effect chains and the game only sets parameters ("depth = 0.8"). That's the next step up if it's ever needed ([libraries](libraries.md)); for this engine, EFX covers equalizer, distortion and reverb.

---

## Where effect data lives: both, at two levels

The same split as shader vs material in rendering.

**Inside OpenAL, effects are objects created with commands,** like GPU objects: created, given values, deleted.
```java
int effect = alGenEffects();
alEffecti(effect, AL_EFFECT_TYPE, AL_EFFECT_REVERB);    // what kind
alEffectf(effect, AL_REVERB_DECAY_TIME, 2.5f);           // its settings
alEffectf(effect, AL_REVERB_DENSITY, 0.8f);

int slot = alGenAuxiliaryEffectSlots();                  // a place effects run in
alAuxiliaryEffectSloti(slot, AL_EFFECTSLOT_EFFECT, effect);

alSource3i(source, AL_AUXILIARY_SEND_FILTER, slot, 0, AL_FILTER_NULL);  // send this source into it
```
They only exist while the device is open: **runtime state**, owned by the audio system, like sources.

**The settings are an asset.** "Cave reverb: decay 2.5 s, density 0.8, gain 0.6" is a **preset** tuned once and reused everywhere, as a material holds settings for a shader.
- **The asset is plain data:** e.g. `cave.reverb` as JSON, with the effect type and its parameters. No OpenAL inside, so it loads entirely on a worker, and it still loads with no audio device.
  - Its codec has a loader **and** a saver, because presets are made and tweaked in the editor.
- **The audio system turns presets into live objects:** it creates the effect and slot, copies the preset's values in, and updates them when the preset changes. So a preset can be edited while the game runs and heard at once.
- **Game code only picks a preset and how strongly:** "cave reverb at 70%" as the player walks deeper.

How the big engines split it:
- **Unity:** the **AudioMixer** is an asset holding groups, their effects and settings (and "snapshots" to blend between); reverb zones are components.
- **Godot:** the **bus layout** is a resource file listing buses and their effects; each effect (`AudioEffectReverb`, …) is itself a resource with its settings.

**One limit:** a source can only send to a **small number of effect slots at once**, set when the context is created (`ALC_MAX_AUXILIARY_SENDS`). So effects are usually applied per *group* of sounds (a bus), not stacked per sound.

The presets, the mixer they live in, and their editors are in [assets and editors](assets-and-editors.md).
