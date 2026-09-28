# Sound runtime: AudioSystem, AudioClip, SoundSource

Part of the [sound engine plan](../sound-engine.md). Replaces [the current code](current-code.md). Concepts: [how a sound system works](how-it-works.md). Loaders and pools: [asset pipeline](../asset-pipeline.md).

The code-level design of the three pieces: the system that owns OpenAL, the clip asset and its codec, and the player.

---

## `AudioSystem`

Owns OpenAL, the way `Renderer` owns OpenGL.

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
- **No device:** `init()` logs it once and leaves the system disabled; every call does nothing. Device choice and switching are in [OpenAL](openal.md#choosing-the-sound-hardware).

---

## The asset: `AudioClip` and `AudioCodec`

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

- **Why decoding happens up front:** a clip is fully decoded into memory. That's right for short sounds (steps, hits, UI). Long music would take a lot of memory this way; it gets streaming later (see the [plan](../sound-engine.md#later)).
- **Not saved:** audio files come from other tools, so `AudioCodec` has no `AssetSaver`.
- **`.wav`:** stb has no wav reader. A small parser for uncompressed PCM wav can be added as a second codec later.

---

## Playing: `SoundSource`

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
- **Later, sound events sit on top:** a source plays whatever clip an event picks, with the event's randomised volume and pitch ([assets and editors](assets-and-editors.md)).
