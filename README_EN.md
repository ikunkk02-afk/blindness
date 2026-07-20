[简体中文](README.md) | English

# Blindness (失明症)

**Blindness** is a Fabric mod for Minecraft 1.21.1 that simulates severe visual impairment. Your vision is heavily restricted — navigate using a guidance cane, sound echoes, block outlines, and accessibility markers.

> This is a gameplay mod designed to provide a challenging but playable blindness experience. It is not a medical simulation.

---

## Features

### Visual Restriction

- Near-total black screen; only faint nearby light remains
- Veil post-processing for effects and block outlines
- HUD-based fallback black screen when Veil is unavailable
- Compatible with Sodium and Iris

### Guidance Cane

- New players receive one guidance cane automatically on first join (once per player)
- **Right-click**: probe forward, revealing the hit block and its six direct neighbors as outlines
- **Hold right-click**: continuous probing of a few blocks
- Outlines fade in, hold briefly, then fade out
- Different feedback for regular blocks vs. ores

### Ore Detection

- Ores detected by the cane display a distinct orange outline
- HUD shows ore name, count, direction, and distance
- Directional sound cues for detected ores
- Supported vanilla ores: coal, iron, copper, gold, redstone, lapis, diamond, emerald, nether quartz, ancient debris
- Compatible with modded ores that use Fabric's common ore tags
- **Not X-ray**: only ores within actual cane probe range are revealed

### Sound Awareness

- Creature sounds produce "sound echo" markers on the HUD
- Directional origin indicators distinguish neutral from hostile creatures
- Vague warning when hostile entities are nearby
- Wall occlusion blurs sounds behind blocks

### Ender Eye Accessibility

- While flying: on-screen tracking marker (green ring + distance)
- While off-screen: edge arrow pointing toward the eye
- Real-time distance display with height indicators
- Dropped eyes: brief item tracking (~9 seconds)
- Shattered eyes: clear notification with sound
- **Only tracks eyes thrown by you** — no stronghold coordinates revealed

### Other Mechanics

- Running too fast or hitting walls may cause tripping
- Cliff edge warnings (text + narration + camera feedback)
- First-person fall camera tilt
- Subtitle direction blurring
- Optional blocking of minimap and info-HUD mods

---

## Controls

| Action | Default | Description |
|---|---|---|
| Probe with cane | Right-click | Detect blocks ahead |
| Continuous probe | Right-click (hold) | Sweep multiple blocks |
| Open settings | `B` | Adjust client config |

**Ender Eye tracking** activates automatically upon throwing. No extra input needed.

**Configuration**: press `B` in-game or edit `config/blindness-client.properties`.

---

## Requirements

### Required

- Minecraft 1.21.1
- Fabric Loader (≥ 0.19.3)
- Fabric API (≥ 0.116.14)
- Cardinal Components API (≥ 6.1.3)
- owo-lib (≥ 0.12.15.4+1.21)

### Recommended

- Veil (≥ 4.3.0) — better visuals and outline rendering
- Player Animator (≥ 2.0.4) — cane animations
- Mod Menu (≥ 11.0.4) — in-game settings menu

### Optional

- Sodium — performance, tested compatible
- Iris — shaders, tested compatible

---

## Installation

1. Install Minecraft 1.21.1 with Fabric Loader
2. Place all required dependencies into `.minecraft/mods/`
3. Download `blindness-1.1.0.jar` and place it into `.minecraft/mods/`
4. Launch the game

> Use the remapped release JAR (e.g. `blindness-1.1.0.jar`). Do not install `-dev.jar` or `-sources.jar`.

---

## Configuration

Press `B` in-game or edit `config/blindness-client.properties`.

### Visual

| Option | Default | Description |
|---|---|---|
| enableVisualPostProcessing | true | Veil post-processing (off = HUD fallback) |
| useDetailedModelOutlines | true | Detailed model outlines |
| contactHoldTime | 5.00 s | Outline hold duration |
| contactFadeOutTime | 0.80 s | Outline fade-out duration |
| keepHeldCaneVisible | true | Keep cane visible when held |
| blackScreenBehindMenus | true | Black screen behind menus |

### Ore Detection

| Option | Default | Description |
|---|---|---|
| enableOreHud | true | Show ore HUD |
| enableOreSound | true | Directional ore sound |
| oreOutlineDurationTicks | 60 | Ore outline duration (ticks) |
| maxRenderedOres | 16 | Max simultaneous ore entries |

### Ender Eye Tracking

| Option | Default | Description |
|---|---|---|
| enableEnderEyeTrackingMarker | true | Master toggle |
| enableEnderEyeWorldMarker | true | On-screen world marker |
| enableEnderEyeEdgeArrow | true | Off-screen edge arrow |
| showEnderEyeDistance | true | Show distance |
| enableEnderEyeTrackingSound | true | Flight tracking sound |
| enderEyeTrackingSoundIntervalTicks | 25 | Sound interval (10~100 ticks) |
| droppedEnderEyeMarkerDurationTicks | 180 | Drop marker duration (40~600 ticks) |
| enableEnderEyeResultHint | true | Result notifications |

### Sound Awareness

| Option | Default | Description |
|---|---|---|
| entitySoundEchoEnabled | true | Enable sound echoes |
| listeningChunkRadius | 1 | Listening chunk radius (0~2) |
| showOffscreenSoundEchoes | true | Show off-screen echoes |

### Accessibility

| Option | Default | Description |
|---|---|---|
| cameraShakeStrength | 0.45 | Camera shake intensity |
| showTutorial | true | Show new-player tutorial |
| cliffWarningText | true | Cliff warning text |
| hostileWarningText | true | Hostile warning text |

---

## Compatibility

### Tested With

- Sodium
- Iris (with and without shaders)
- Veil
- Singleplayer
- Fabric multiplayer servers

### Known Incompatibility

- **DashLoader**: unsupported. Its shader cache restoration conflicts with Veil initialization timing. Do not install alongside this mod.

For compatibility issues, provide `latest.log`, crash report, full mod list, and reproduction steps.

---

## Multiplayer

- **Both client and server** must install this mod
- Starter cane is tracked per-player independently
- Ore detection is server-confirmed (anti-cheat)
- Ender Eye markers only show **your own** thrown eyes
- Server admins can use `/blindness enable/disable` per player

---

## Commands

| Command | Permission | Description |
|---|---|---|
| `/blindness enable` | Self | Enable blindness |
| `/blindness disable` | Self | Disable blindness |
| `/blindness status` | Self | Show status |
| `/blindness reset` | OP or Creative | Reset player data |

---

## What's New in 1.1.0

### Added

- Auto-grant starter cane on first join
- Ore detection system (outlines, HUD, directional sound)
- Ender Eye shatter notification
- Ender Eye drop notification
- On-screen Ender Eye tracking marker (green ring + distance)
- Off-screen directional edge arrow
- Dropped Ender Eye item tracking
- Tracking sound (configurable interval)

### Fixed

- Mixin injection failure causing startup crash
- Unclear Ender Eye result feedback

### Compatibility

- Rendering compat with Sodium, Iris, and Veil
- DashLoader remains unsupported

---

## Issues

Report issues at [GitHub Issues](https://github.com/ikunkk02-afk/blindness/issues) with:

```
Minecraft version:
Fabric Loader version:
Mod version:
Dependency versions:
Sodium/Iris installed:
Description:
Steps to reproduce:
latest.log:
```

---

## License

MIT License. See `LICENSE` file in the repository.

---

## Credits

- [Fabric](https://fabricmc.net/) and [Fabric API](https://github.com/FabricMC/fabric)
- [Veil](https://github.com/FoundryMC/Veil) — rendering framework
- [Cardinal Components API](https://github.com/Ladysnake/Cardinal-Components-API)
- [owo-lib](https://github.com/wisp-forest/owo-lib)
- [Player Animator](https://github.com/KosmX/playerAnimator)
- Players and viewers who tested and provided feedback
