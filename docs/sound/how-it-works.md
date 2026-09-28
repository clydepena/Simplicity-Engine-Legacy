# How a sound system works

Part of the [sound engine plan](../sound-engine.md). Next: [OpenAL: devices, distance and effects](openal.md).

It's set up like the renderer: `Application` creates it, starts and stops it, and layers can reach it. How it's *used* is the opposite.

---

## Rendering is redone every frame; audio is not

- **Rendering:** the screen is cleared and redrawn every frame. A sprite that isn't passed to the renderer in a frame isn't on screen, so every drawable is handed over again each frame.
- **Audio:** a sound is **started once** and keeps playing on its own. OpenAL mixes it on its own background thread (44,100 samples a second), whether the game loop is running or not. Game code doesn't pass sounds every frame; it gives **commands** (play, stop, change volume) and otherwise leaves them alone.

```
Renderer:   frame 1: draw(A, B, C)   frame 2: draw(A, B, C)   frame 3: draw(A, B)   <- C gone
Audio:      frame 1: play(explosion) frame 2: (nothing)       frame 3: (nothing)    <- still playing
```

## The three pieces

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

## What the audio system does each frame

Little, and only for what changes. There's no "draw" step for sound:

```java
audio.onUpdate(dt);
//  - move the listener to the camera
//  - move sources attached to moving objects (a car driving past)
//  - recycle one-shot sources that finished playing
//  - later: refill streamed music a few seconds ahead
```

## What game code looks like

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

Once sound events exist ([assets and editors](assets-and-editors.md)), game code plays *events* rather than clips: `audio.play("footstep_grass")`.

## Compared with the renderer

| | Renderer | Audio system |
|---|---|---|
| Owned by | `Application` | `Application` |
| `init` / `destroy` | after the window / before it | after the window / before it |
| Per frame | every `Drawable` is passed again | nothing is passed; it only updates what moved |
| It's given | things to draw, every frame | commands: play / stop / change volume |
| Its "objects" | drawables, gone after the frame | sources, living until they're stopped |
