# Procedural sound

Part of the [sound engine plan](../sound-engine.md). Builds on the [runtime](runtime.md) (clips, sources) and the [asset pipeline](../asset-pipeline.md) (codecs). Related: node graphs in [assets and editors](assets-and-editors.md#how-sound-behaviour-is-controlled-four-levels).

Sound calculated by code instead of played from a recording: what it is, the common techniques, and how this engine could do it.

Status: **hypothetical.** Nothing here is planned yet; it records how it would fit.

---

## What sound is to a computer

A long list of numbers: the position of the speaker cone, measured tens of thousands of times a second (e.g. 48,000 numbers per second, each between −1 and +1). A recording (`.ogg`) stores those numbers; procedural audio **computes** them with formulas:

```java
// one second of a 440 Hz tone (the note A): the simplest procedural sound
short[] samples = new short[48000];
for (int i = 0; i < samples.length; i++) {
    double t = i / 48000.0;                                   // time in seconds
    samples[i] = (short) (Math.sin(2 * Math.PI * 440 * t) * 32767 * 0.5);
}
```

Everything else is combinations of a few building blocks.

---

## The building blocks

| Block | What it does | Example |
|---|---|---|
| **Oscillator** | a repeating wave: sine (pure), square (buzzy, retro), saw (bright), triangle | a laser beep, an engine's hum |
| **Noise** | random numbers: a hiss | wind, rain, explosions, footsteps on gravel |
| **Envelope** (ADSR) | shapes loudness over time: **a**ttack, **d**ecay, **s**ustain, **r**elease | a "pew" that starts sharp and fades |
| **Filter** | removes high or low frequencies | noise + a moving filter = wind gusting |
| **Modulation** | one wave slowly changes another's pitch or volume | vibrato, sirens, a wobbling alarm |

---

## Common techniques

- **Retro effect generators (sfxr, bfxr, jsfxr):** an oscillator plus an envelope plus a pitch slide, with ~20 sliders and a "randomise" button. Jumps, coins, explosions and power-ups in countless indie games are made this way. The easiest place to start.
- **Noise-based nature sounds:**
  - **Wind:** noise through a filter whose cutoff drifts slowly; the gusts come for free.
  - **Rain:** thousands of tiny random "ticks" on top of soft noise, with the density set by a rain-intensity value.
  - **Fire:** noise plus random crackling pops.
- **Physical modelling:** simulate the object.
  - **A plucked string (Karplus–Strong):** a burst of noise fed through a short delay loop; 10 lines of code, surprisingly realistic.
  - **Impacts:** a struck object "rings" with a few decaying tones whose pitch depends on its size and material. The same code gives a wooden crate, a metal barrel and a glass bottle.
- **Granular synthesis:** a recording chopped into tiny pieces (10–50 ms "grains") played overlapping, shuffled or stretched. Car engines, crowds and endless water use it: a short recording becomes an endless, never-repeating sound.
- **Parameter-driven synthesis:** a car engine built from its RPM, its tone rising with speed, with no recording at all.
- **Generative music:** code choosing notes by rules, e.g. calmer when exploring, busier in combat.

---

## How this engine could do it: two levels

### Level A: generate a clip once, then play it normally (easy)

Calculate all the samples up front, upload them as an ordinary OpenAL buffer, and from then on it's a normal `AudioClip`. This fits the existing asset pipeline:

- **An asset file holds the *recipe*, not the sound:** e.g. `jump.sfx` as JSON with the waveform, envelope and pitch slide, the way sfxr stores its presets.
- **The codec's `decode`, on a worker,** runs the synthesizer and produces the samples: the same shape as `.ogg` decoding ([runtime](runtime.md#the-asset-audioclip-and-audiocodec)).
- **`finish`, on the main thread,** uploads them; the result is a normal clip.
- **Variation:** a sound event can regenerate it with slightly randomised settings, so no two jumps sound identical.
- **The editor is sfxr's:** sliders, a waveform preview, "randomise" and "mutate" buttons, with a codec that loads *and* saves the recipe.

Roughly a few hundred lines, with no new audio machinery.

### Level B: generate continuously while it plays (harder)

For sounds that react live, like wind following the weather or an engine following the RPM, the samples must be computed **just ahead of playback, forever**. OpenAL supports this with **buffer queueing**, the same mechanism music streaming uses:

```
source's queue:  [buffer 1: playing] [buffer 2: ready] [buffer 3: ready] [buffer 4: ready]
                  ~20 ms of audio each
when buffer 1 finishes -> take it off (alSourceUnqueueBuffers),
                          fill it with the next 20 ms of newly calculated sound,
                          put it back at the end (alSourceQueueBuffers)
```

The hard part is **timing**: if a buffer isn't refilled in time, the source runs dry and there's a click or a gap. So:

- **Not on the game loop:** a slow frame (loading, a GC pause) would starve the audio. It needs **its own dedicated thread**, not the `Tasks` pool, which runs work "whenever", while this needs steady timing. The OpenAL context works from any thread, so that's allowed.
- **No allocation in the sound loop:** allocating objects feeds the garbage collector, whose pauses cause gaps. Arrays are preallocated and reused.
- **Parameters cross threads safely:** the game writes "rpm = 0.7" into a `volatile` field, and the audio thread reads it and **smooths** the change over a few milliseconds. Jumping abruptly makes an audible "zipper" crackle.
- **Latency is a trade-off:** smaller or fewer buffers react faster but underrun more easily. 4 × 1024 samples at 48 kHz is about 85 ms: fine for wind and engines, too slow for a musical instrument.
- **Alternative:** OpenAL Soft's *callback buffer* extension (`AL_SOFT_callback_buffer`), where OpenAL itself asks the code for samples from its own mixing thread. Lower latency, but the code then runs inside OpenAL's time-critical thread, so the no-allocation rules become strict.

**Level B is also what a node-graph runtime is** (level 4 in [assets and editors](assets-and-editors.md#how-sound-behaviour-is-controlled-four-levels)): each node computes or reshapes a block of samples, and the graph runs on this streaming loop. Procedural audio and node graphs are the same investment.

---

## Recommendation

- **Level A is a fun, cheap feature** that fits the existing loader design with no new machinery. A good fit for a 2D game, retro effects especially.
- **Level B waits for music streaming,** which needs the same buffer queueing plus an audio thread. Once streaming exists, procedural wind or rain is a small step on top.
