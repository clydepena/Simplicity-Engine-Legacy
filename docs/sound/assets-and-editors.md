# Sound assets and editors

Part of the [sound engine plan](../sound-engine.md). Builds on the [runtime](runtime.md) (clips and sources) and [OpenAL](openal.md) (distance, effects). Loaders and savers: [asset pipeline](../asset-pipeline.md).

Which audio assets exist, what their editors look like, and how sound behaviour is controlled, from code up to node graphs.

---

## The audio assets

| Asset | What it holds | Made in |
|---|---|---|
| **Audio clip** (`.ogg`, `.wav`) | the recording itself | external tools (Audacity, Reaper, a synth) |
| **Sound event** (e.g. `footstep_grass.sound`) | a *designed* sound: which clips, how to vary them, how it behaves in the world | the editor |
| **Mixer / bus layout** (`.mixer`) | the groups of sounds (master -> music, effects -> footsteps, UI), their volumes and effect chains, and snapshots | the editor |
| **Effect preset** (`cave.reverb`) | one effect's settings; can also live inside the mixer ([OpenAL](openal.md#where-effect-data-lives-both-at-two-levels)) | the editor |
| **Music** (`forest.music`) | tracks with loop points, and later layers that fade in and out and transitions between them | the editor |

Everything made in the editor is plain data (JSON) with a codec that loads **and** saves; only clips are load-only.

### The sound event is the most important one

Game code shouldn't play *files*; it plays *events*: `audio.play("footstep_grass")`. The event decides:
- **Which clip:** pick at random from 4 recorded variations, never the same one twice in a row, so footsteps don't sound robotic.
- **Random pitch and volume:** e.g. pitch 0.95–1.05, volume 0.8–1.0.
- **Which bus it plays on:** e.g. "Effects/Footsteps".
- **How it behaves in the world:** positional or not, the distance where it's at full volume and where it's silent.
- **Limits:** e.g. at most 3 playing at once, or at least 0.1 s between two plays, so 50 enemies hitting at once don't drown everything else.

It's what FMOD and Wwise call *events* / *sound objects*, and Unreal a *Sound Cue*. A sound designer tunes it without touching code, and game code never changes when the sound does.

### Snapshots

A named set of mixer settings, e.g. "underwater" (music quieter, low-pass on effects) or "paused" (everything muffled). Game code switches to one with a fade.

### Not assets: placed in the world

- **Sound emitters:** a campfire crackling, a river.
- **Ambience and reverb zones:** "inside this area: cave reverb plus a dripping-water loop".

For a 2D open world these zones do a lot: they're what make a forest sound different from a town. They're components saved with the world, referring to events and presets by path.

---

## What the editors look like

**Clip inspector:** mostly a viewer.
- the waveform, a play button, and its length, channels and sample rate
- loop points for music
- a warning when a clip meant for positional sound is stereo

**Sound event editor:** a form, not a graph.
```
footstep_grass.sound
  Clips        [grass_1.ogg] [grass_2.ogg] [grass_3.ogg] [grass_4.ogg]   (+)
  Pick         Random, no repeat
  Volume       0.80 ──────●── 1.00
  Pitch        0.95 ───●───── 1.05
  Bus          Effects/Footsteps  ▼
  3D           [x]  full volume within 2 m, silent beyond 25 m   (falloff curve preview)
  Limit        3 at once, 0.1 s apart
  [▶ Play]  [▶ Play at 10 m]
```
The preview buttons are what make it usable: the randomisation and the distance falloff can be heard without starting the game.

**Mixer editor:** looks like a mixing desk, or a DAW (Reaper, FL Studio).
- a vertical strip per bus, with a volume fader, mute, solo, and a live level meter
- the bus's effect slots below the fader, e.g. "Reverb: cave, 70%"
- snapshots listed on the side, to test with one click
- live while the game runs, so it can be balanced by ear

**In the world (the viewport):**
- **emitters** shown as icons with their distance circles, full volume inside, silent outside, so where a river is audible can be *seen*
- **reverb and ambience zones** as shapes, drawn like selections

**Debug overlay:** which sounds are playing right now, on which bus, how many voices are in use. Essential once "why is it so loud here?" comes up.

---

## How sound behaviour is controlled: four levels

**Level 1: code.** A component calls `play("jump")` when the player jumps. It always works, and every engine has it.

**Level 2: data (sound events).** The variation, randomness, distance and limits above. With no scripting, this covers most games' needs.

**Level 3: parameters and curves.** What FMOD and Wwise are known for (Wwise calls it RTPC, "real-time parameter control"). A sound event exposes a parameter, and curves map it to volume, pitch or layers:
```
car_engine.sound    parameter: rpm (0 … 1)
  idle.ogg   volume  ████▇▅▃▁____      fades out as rpm rises
  high.ogg   volume  ____▁▃▅▇████      fades in
  pitch      0.8 ──────────────▶ 1.6
```
Game code only does `engine.setParameter("rpm", 0.7)`. The editor for it is a **curve editor**. Other uses: rain intensity, a "danger" level blending in combat music layers, wind strength.

**Level 4: node graphs.** Unreal's **MetaSounds** (and before it, Sound Cues): nodes like *play clip -> random -> mix -> delay -> filter*, wired together, sometimes even generating sound from scratch (procedural engines, synthesizers). Very powerful, but:
- **It's expensive to build:** the graph editor, a runtime that executes it, and saving it.
- **Most games never need it:** levels 2 and 3 cover almost everything, and FMOD and Wwise are mostly *not* node-based.
- **Its real use is procedural audio:** sound generated or heavily reshaped while the game runs.

The editor already has a `NodeEditorPanel`, so a node graph isn't out of reach later. It still comes last, only if levels 2–3 run out. Running a graph is the same machinery as live procedural sound ([procedural sound](procedural.md#level-b-generate-continuously-while-it-plays-harder)).

### Triggers

Separate from these levels, many sounds are started by other systems rather than by code written for them:
- **animation events:** a marker on frame 3 of the walk cycle plays "footstep"
- **collisions:** a physics impact plays a sound scaled by its strength
- **zones:** entering an area starts its ambience

Their editors are the animation timeline (markers) and the zone inspector.

---

## Order

Picks up after the runtime work in the [plan](../sound-engine.md#order-of-work):

1. **Sound events:** the asset, its codec, and the form editor with preview. The biggest gain for the least work.
2. **Mixer:** buses and volumes, and a settings menu for players (music, effects, UI sliders); then effects (presets) and snapshots.
3. **Emitters and zones in the world,** drawn in the viewport. This matters for an open world.
4. **Parameters and curves** in sound events.
5. **Node graphs:** only if something really needs them.
