# Sound: where the code is today

Part of the [sound engine plan](../sound-engine.md). What replaces it: [runtime: AudioSystem, AudioClip, SoundSource](runtime.md).

---

- **OpenAL is set up inside `Window`.** `Window.init()` opens the default audio device, creates a context and makes it current; `Window.destroy()` destroys them. Audio has nothing to do with the window: it's there only because both happen at startup.
- **`simplicity.Sound` is two things in one class:**
  - the **audio data**: an OpenAL buffer, decoded from an `.ogg` file with `stb_vorbis_decode_filename`
  - a **player**: an OpenAL source, with `loops`, a fixed volume (0.3), `play()`, `stop()`, `isPlaying()`
- **Loaded only by the legacy pool:** `AssetPool.addSound(path, loops)` / `getSound(path)` / `getAllSounds()`.
- **Nothing uses it.** There are no calls outside the legacy pool, and no `.ogg` or `.wav` files in the resources.

## Problems

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
