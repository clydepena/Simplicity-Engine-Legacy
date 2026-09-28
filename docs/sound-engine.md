# Sound engine: plan

Status: **planned, not started.** Related: `asset-pipeline.md` (loaders, the pools, disposal).

How the engine plays sound: an audio system that owns OpenAL (as `Renderer` owns OpenGL), audio clips as assets, and players that use them. Sound is the last asset type still loaded only by the legacy `util.AssetPool`; once it moves, that pool can be deleted.

---

## 1. How a sound system works

It's set up like the renderer: `Application` creates it, starts and stops it, and layers can reach it. How it's *used* is the opposite.

### Rendering is redone every frame; audio is not

- **Rendering:** the screen is cleared and redrawn every frame. A sprite that isn't passed to the renderer in a frame isn't on screen, so every drawable is handed over again each frame.
- **Audio:** a sound is **started once** and keeps playing on its own. OpenAL mixes it on its own background thread (44,100 samples a second), whether the game loop is running or not. Game code doesn't pass sounds every frame; it gives **commands** (play, stop, change volume) and otherwise leaves them alone.

```
Renderer:   frame 1: draw(A, B, C)   frame 2: draw(A, B, C)   frame 3: draw(A, B)   <- C gone
Audio:      frame 1: play(explosion) frame 2: (nothing)       frame 3: (nothing)    <- still playing
```

### The three pieces

OpenAL, Unity and Godot all split sound the same way:

| Piece | Real-world idea | In OpenAL | Here |
|---|---|---|---|
| **Clip** | a recording | a *buffer* | `AudioClip` (the asset) |
| **Source** | a speaker placed somewhere, playing a recording | a *source* | `SoundSource` |
| **Listener** | the ear, usually at the camera | the *listener* | inside `AudioSystem` |

- **Many speakers can play the same recording at once:** 10 enemies, 1 "hit" clip, 10 sources. That's why the data and the player are separate classes.
- **Each speaker has its own settings:** volume, pitch, looping, position.
- **The listener decides how everything sounds:** a source far to its left sounds quieter and comes from the left.

Unity: an `AudioClip` asset, an `AudioSource` component per game object, an `AudioListener` on the camera. Godot: `AudioStream`, `AudioStreamPlayer`, and the camera as listener.

### What the audio system does each frame

Little, and only for what changes. There's no "draw" step for sound:

```java
audio.onUpdate(dt);
//  - move the listener to the camera
//  - move sources attached to moving objects (a car driving past)
//  - recycle one-shot sources that finished playing
//  - later: refill streamed music a few seconds ahead
```

### What game code looks like

**Fire and forget:** a UI click or an explosion, never controlled again:
```java
audio.playOnce(clickClip, 0.8f);   // borrows a free source, returns it when the sound ends
```

**A source that's kept:** anything to stop, loop or move later, e.g. an engine hum or footsteps. Usually a component on a game object:
```java
SoundSource engineHum = audio.createSource();
engineHum.setClip(handler.get("sounds/engine.ogg", AudioClip.class));
engineHum.setLoops(true);
engineHum.play();          // once, when the car starts
...
engineHum.stop();          // once, when the car stops
```

**Music:** usually its own call, because it's long and fades in and out:
```java
audio.playMusic(forestTheme, 2.0f);   // 2-second crossfade (later, with streaming)
```

### Compared with the renderer

| | Renderer | Audio system |
|---|---|---|
| Owned by | `Application` | `Application` |
| `init` / `destroy` | after the window / before it | after the window / before it |
| Per frame | every `Drawable` is passed again | nothing is passed; it only updates what moved |
| It's given | things to draw, every frame | commands: play / stop / change volume |
| Its "objects" | drawables, gone after the frame | sources, living until they're stopped |

---

## 2. Devices, distance and effects: who decides what

LWJGL ships **OpenAL Soft**, which supports everything below. In each case the engine decides *what* should happen, and OpenAL does the actual sound processing.

### Choosing the sound hardware

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

### Distance and direction: OpenAL calculates them

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

### Effects beyond volume: EFX

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

**Beyond OpenAL:** FMOD and Wwise put this in a separate editor, where a sound designer builds effect chains and the game only sets parameters ("depth = 0.8"). That's the next step up if it's ever needed; for this engine, EFX covers equalizer, distortion and reverb.

---

## 3. Where the code is today

- **OpenAL is set up inside `Window`.** `Window.init()` opens the default audio device, creates a context and makes it current; `Window.destroy()` destroys them. Audio has nothing to do with the window: it's there only because both happen at startup.
- **`simplicity.Sound` is two things in one class:**
  - the **audio data**: an OpenAL buffer, decoded from an `.ogg` file with `stb_vorbis_decode_filename`
  - a **player**: an OpenAL source, with `loops`, a fixed volume (0.3), `play()`, `stop()`, `isPlaying()`
- **Loaded only by the legacy pool:** `AssetPool.addSound(path, loops)` / `getSound(path)` / `getAllSounds()`.
- **Nothing uses it.** There are no calls outside the legacy pool, and no `.ogg` or `.wav` files in the resources.

### Problems

| Problem | Effect |
|---|---|
| Audio setup lives in `Window` | Audio can't be started, stopped or replaced on its own; a game without a window (tests, a server) can't have audio, and a window can't exist without audio. |
| No audio device is not handled | `alcOpenDevice` returns 0 on a machine with no sound output (or with it disabled). The code doesn't check it and goes on to create a context and capabilities for that null device, which can stop startup with an exception. (Not tested yet: needs a machine without an output device.) |
| Shutdown order is unmanaged | Buffers and sources must be deleted before the context is destroyed. `Window.destroy()` destroys the context without knowing whether any sound still exists. |
| Data and player in one class | One source per file: two things can't play the same sound at once (the second call restarts the first). `loops` is fixed when the file is loaded, though it's a property of how a sound is played, not of the file. |
| `alSourcei(source, AL_POSITION, 0)` used to rewind | `AL_POSITION` is the source's 3D position in the world, a 3-float vector, not the playback position. The call is invalid and rewinds nothing. Rewinding is `alSourceRewind(source)` or `alSourcef(source, AL_SEC_OFFSET, 0)`. |
| `destroy()` is never called | Every sound leaks its buffer and its source. |
| More than 2 channels | `format` stays -1, `alBufferData` fails with an OpenAL error nobody checks, and the sound is silently empty. |
| Decoding reads the file itself (`stb_vorbis_decode_filename`) | Can't read from the jar or a packed archive, and can't split into a worker step, unlike the other loaders. |

---

## 4. Decisions

- **An `AudioSystem` owns OpenAL, the way `Renderer` owns OpenGL.** `Application` creates it, calls `init()` after the window and `destroy()` before it, and gives it to layers the same way it gives them the renderer.
- **The asset is only the audio data** (`AudioClip`: an OpenAL buffer). It's shared through the pools like every other asset.
- **Playing is separate** (`SoundSource`: an OpenAL source). One per thing that makes a sound; many can play the same clip at once.
- **No audio device is not an error.** The engine starts without sound, logs it once, and every audio call does nothing.
- Naming follows Unity (`AudioClip` for data, `SoundSource` for the player). Open: keep the name `Sound` for the data and call the player `SoundPlayer` instead (see section 9).

---

## 5. `AudioSystem`

```java
public final class AudioSystem {
    public void init();                 // open the default device, create + make current the context; no device -> disabled
    public void destroy();              // delete remaining sources, then context, then device (in that order)
    public boolean isEnabled();         // false when there's no device: everything else does nothing

    public void onUpdate(float dt);     // per frame: finished one-shot sources are recycled; later: music streaming

    public void setMasterVolume(float volume);   // the listener's gain (AL_GAIN on the listener)
    public void setListenerPosition(float x, float y);   // later, for positional sound (usually the camera)

    SoundSource createSource();         // tracked, so destroy() can delete sources that were never disposed
}
```

- **Lifetime mirrors the renderer:** `Application.run()` calls `window.init()`, `renderer.init()`, `audio.init()`; on exit `audio.destroy()` runs before `window.destroy()`. All OpenAL calls stay on the main thread, like OpenGL ones.
- **`Window` loses all OpenAL code:** the device and context fields, the imports, and the lines in `init()` and `destroy()`.
- **Where it lives:** its own package, `audio` (like `renderer`), next to `AudioClip`, `SoundSource` and the codec's data type.
- **Pools and shutdown:** the asset pools are closed before `audio.destroy()`, so clips are disposed while the context still exists. The same applies to textures and the GL context today.

---

## 6. The asset: `AudioClip` and `AudioCodec`

```java
public final class AudioClip implements Disposable {
    int bufferId;
    public float getLengthSeconds();
    public int getChannels();
    public int getSampleRate();
    public void dispose();              // alDeleteBuffers; safe to call twice
}
```

`AudioCodec implements AssetLoader<AudioClip, PcmData>`, registered for `ogg`:

| Step | Thread | Work |
|---|---|---|
| `decode(bytes)` | worker | copy the bytes into a **direct** buffer (a heap buffer crashes the JVM in native code, as found with textures), `stb_vorbis_decode_memory` -> 16-bit samples, channel count, sample rate. Throws on bad data or more than 2 channels. |
| `PcmData` | | the samples are stb's memory: `implements Disposable`, freed with `LibCStdlib.free`, so a discarded load doesn't leak |
| `finish(pcm, handler)` | main | `alGenBuffers` + `alBufferData` (mono or stereo, 16-bit), then free the samples. With audio disabled, returns an empty clip that plays nothing. |

- **Why decoding happens up front:** a clip is fully decoded into memory. That's right for short sounds (steps, hits, UI). Long music would take a lot of memory this way; see *Later* below.
- **Not saved:** audio files come from other tools, so `AudioCodec` has no `AssetSaver`.
- **`.wav`:** stb has no wav reader. A small parser for uncompressed PCM wav can be added as a second codec later.

---

## 7. Playing: `SoundSource`

```java
public final class SoundSource implements Disposable {
    public void setClip(Asset<AudioClip> clip);   // a handle: follows reload and move
    public void play();                 // from the start if stopped; resumes if paused
    public void pause();
    public void stop();                 // stops and rewinds (alSourceRewind)
    public boolean isPlaying();         // asks OpenAL (AL_SOURCE_STATE), no cached flag that can go stale
    public void setLoops(boolean loops);
    public void setVolume(float volume);
    public void setPitch(float pitch);
    public void dispose();              // alDeleteSources
}
```

- **The source holds the clip's `Asset<AudioClip>`, not the clip.** If the clip isn't loaded yet (or is `MISSING`), `play()` does nothing instead of failing. When the clip is reloaded, the source must be stopped and re-attached, since OpenAL won't delete a buffer that a source still uses.
- **One-shot sounds** (an explosion that nobody keeps a player for): `AudioSystem.playOnce(clip, volume)` takes a source from a small pool of free sources and returns it when the sound ends (in `onUpdate`).
- **In the game:** a component (e.g. `AudioSourceComponent`) wraps a `SoundSource` for game objects, with the clip saved as its path like any `Asset<T>` field.

---

## 8. Later

- **Music streaming:** long files decoded a few seconds at a time into a small ring of buffers, refilled in `AudioSystem.onUpdate()` (`stb_vorbis_open_memory` + `stb_vorbis_get_samples_short_interleaved`). Needed once music exists.
- **Positional sound:** listener at the camera (slightly above the map), sources at their objects, a distance model, Doppler. Mono clips only. Details in section 2.
- **Mixer groups:** separate volumes for music, effects and UI; later with EFX effect chains per group (section 2).
- **Occlusion:** muffling sounds behind walls: a ray through the tilemap, then a low-pass filter.
- **Device choice and changes:** an "Output device" setting (device enumeration), and headphones unplugged while running: OpenAL Soft reports it (`ALC_CONNECTED`) and can move to another output with `ALC_SOFT_reopen_device` without losing clips (section 2).

---

## 9. Open decisions

1. **Names:** `AudioClip` + `SoundSource` (recommended), or `Sound` + `SoundPlayer`.
2. **Where `AudioSystem` is reachable from:** passed like the renderer (through `Application` and the render context), or a single static instance like `Tasks`. Passing it matches how the renderer is handled.

---

## 10. Order of work

1. **`AudioSystem`**: move the OpenAL setup out of `Window`, handle a missing device, call it from `Application.run()` in the right order.
2. **`AudioClip` + `AudioCodec`** (decode on a worker, upload on the main thread). Test with a short `.ogg` added to the resources.
3. **`SoundSource`**, with the rewind and cleanup bugs fixed.
4. **Remove `simplicity.Sound`** and the sound methods of the legacy `util.AssetPool` (nothing uses them).
5. Then the legacy pool can be deleted, once textures, shaders and sheets are switched over (see `asset-pipeline.md`).
6. Later: one-shot sources, a sound component, music streaming, positional sound.
