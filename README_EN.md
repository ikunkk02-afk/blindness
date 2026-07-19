[简体中文](README.md) | English

# Blindness

**Blindness** is an immersive Fabric mod for Minecraft. The world is pitch black.
Players rely on a guidance cane, sound echoes, block outlines, and vague danger
hints to navigate.

The world is not dimmed — it is fully dark. Perception comes through touch-like
probing and sound awareness.

> **Important**: This mod gamifies environmental perception methods that some
> individuals with severe visual impairments may use. It is not a representation
> of all blind or visually impaired people's real experiences, nor a complete
> simulation of medical or life conditions.

## Basic Info

| Item | Detail |
|------|--------|
| Minecraft | 1.21.1 |
| Mod Loader | Fabric |
| Java | 21 |
| Version | 1.0.0 |
| License | MIT |
| Installation | Client (required), Server (required) |
| GitHub | https://github.com/ikunkk02-afk/blindness |

The mod must be installed on both client and server. The server runs cane
contacts, sound awareness, cliff detection, and fall logic. The client alone
is not sufficient.

## Core Features

- **Completely dark world**: Pitch-black rendering when blindness is active. GUI, inventory, and menus display normally. The world stays black behind pause screens.
- **Guidance cane probing**: Tap or sweep blocks with the guidance cane to briefly reveal the hit block and its direct neighbors with accurate model outlines.
- **Accurate model outlines**: Grass, flowers, torches, fences, stairs, slabs — all show true baked-model outlines, not generic boxes.
- **Cliff and drop warnings**: Detect drops of 2+ blocks, lava, or void ahead when probing with the cane. Provides audio and text warnings.
- **Fall system**: Sprinting into obstacles, mobs, or unscanned elevation changes can cause a fall, followed by a brief movement lock and get-up animation.
- **Creature sound echoes**: When a creature actually makes a sound, an on-screen echo marker appears at the correct direction and height. The marker stays at the source position.
- **Vague hostile warnings**: Detection of nearby hostiles produces a non-specific warning with no name, count, or distance.
- **Configurable listening range**: Creature sound listening radius (0/1/2 chunk radius) and block outline reveal radius (0–4) are adjustable.
- **Information mod compatibility check**: Map, minimap, and block-info HUD mods are detected before entering a world to preserve the intended experience.
- **Mod Menu + in-game config**: Open the owo-lib config screen via Mod Menu or the `B` key to adjust visuals, audio, and accessibility options.

## Guidance Cane

### Recipe

|   |   |   |
|---|---|---|
| Iron Ingot | White Wool |   |
|   | Stick |   |
|   | Stick |   |

Yields 1 Guidance Cane.

### Usage

- **Tap (click and release quickly)**: Hits a block within 4 blocks. The hit block and its orthogonal neighbors briefly show baked-model outlines.
- **Sweep (hold right-click)**: Makes 4 directional contacts over one second (two left, two right), covering more area.

### Outline Behavior

- Outlines use the block's real rendered model (grass, fences, stairs etc. show true shape).
- Center block has thicker, brighter outlines with glow; adjacent blocks are slightly thinner and dimmer.
- Outlines persist ~5 seconds then fade; new contacts refresh the timer.
- Outlines respect occlusion — blocks behind walls are never revealed.
- Nothing is shown if the cane hits empty air.

## Sound Echo System

- **Only actual sounds create echoes**: A marker appears only when a creature plays a sound. Silent immobile creatures never produce persistent markers.
- **Echo position is fixed**: The marker records the sound position and height at emission time. If the creature moves, the old marker remains at its original position and fades.
- **No entity tracking**: Names, health, equipment, count, type, and exact distance are never shown.
- **Off-screen echoes**: Sounds behind the player appear as semi-circular markers at the screen edge.
- **Occluded sounds**: Wall-blocked echoes are more blurred and fragmented but do not reveal hidden blocks.
- **Body-anchored**: Markers sit at creature body height (footsteps ~35%, calls ~65% of height).
- **Default listening range**: 3×3 chunks (player chunk + 8 neighbors).
- **Configurable**: Chunk radius 0/1/2; block outline reveal radius 0–4.

## Cliff Warnings

- Requires active cane probing. Standing empty-handed gives no terrain awareness.
- Samples the forward path within 4 blocks using real collision surfaces.
- 1-block drops are safe; 2–3 blocks trigger a dual-ping warning; 4+ blocks, lava, or void trigger a triple severe alert.
- Depth, coordinates, and safe paths are never shown. The player is never moved or turned.
- Same edge and facing has a 2-second cooldown.

## Fall Mechanics

- Sprinting into obstacles, undergoing horizontal collision, or hitting unscanned elevation changes can cause a trip.
- Colliding with creatures has size-dependent risk (larger mobs = higher risk).
- Walking slowly, sneaking, or previously scanning the path greatly reduces trip risk.
- Tripping locks movement and actions for ~5 seconds, followed by automatic recovery.
- Tripping on grass, snow, or wool causes no damage; hard ground (stone, metal) may deal minor damage.
- Normal walking on flat ground never randomly trips. A protection cooldown follows each fall.

## Installation

1. Install Minecraft 1.21.1.
2. Install compatible [Fabric Loader](https://fabricmc.net/) (≥ 0.19.3).
3. Ensure Java 21 is used.
4. Install all required dependencies (see table below).
5. Place this mod and all dependencies in the `mods` folder.
6. Launch the game.

## Dependencies

| Dependency | Purpose | Required |
|------------|---------|----------|
| [Fabric API](https://modrinth.com/mod/fabric-api) | Base API | Required |
| [Veil](https://modrinth.com/mod/veil) | Rendering pipeline (blackout + depth mask) | Required |
| [Cardinal Components API](https://modrinth.com/mod/cardinal-components-api) | Player data persistence (CCA) | Required |
| [owo-lib](https://modrinth.com/mod/owo-lib) | Config UI | Required |
| [Player Animator](https://modrinth.com/mod/playeranimator) | Cane & fall animations | Required |
| [Mod Menu](https://modrinth.com/mod/modmenu) | Config entry point | Recommended (client only) |

See `fabric.mod.json` `depends` and `recommends` for exact version requirements.

## Controls

| Action | Key |
|--------|-----|
| Use guidance cane | Right Click |
| Open settings | `B` |

Keys can be rebound in Minecraft Controls → Blindness category.

## Commands

| Command | Description |
|---------|-------------|
| `/blindness enable` | Enable blindness experience |
| `/blindness disable` | Restore normal vision and subtitles |
| `/blindness status` | Show status (enabled/disabled, visual mode, cane proficiency, total falls) |
| `/blindness reset` | Reset persistent and transient data (requires OP level 2 or creative mode) |

Available in single-player and multiplayer. Use `disable` for accessibility or debugging.

## Configuration

Open via **Mod Menu → Blindness → Config** or press `B`.

Key settings:

- **Visual**: Outline thickness, brightness, glow, fade timing; menu blackout toggle; held cane visibility.
- **Accessibility**: Camera shake strength; disable first-person fall tilt; show tutorial.
- **Sound Awareness**: Creature echo toggle; listening chunk radius (0/1/2); sound block outline radius (0–4); off-screen echoes; echo size/brightness/duration; occlusion blur; max simultaneous echoes.
- **Compatibility** (requires restart): Map/minimap blocking; block-info HUD blocking; world-visibility blocking; custom add/ignore Mod IDs.

Listening radius and block outline range sync to the server and persist in player data.

## Incompatible or Restricted Mods

The following mod types are detected before entering a world. A conflict screen appears; the conflicting mods must be removed or disabled to proceed. No files are deleted, the game does not crash, and other mod configs are not touched.

### Map / Minimap

`xaerominimap`, `xaeroworldmap`, `journeymap`, `voxelmap`, `ftbchunks`, `antiqueatlas`, `antique_atlas`, `antique_atlas_4`, `map_atlases`, `mapatlases`

### Block-Info HUD

`jade`, `wthit`, `hwyla`, `waila`, `theoneprobe`

### World Visibility (configurable)

Custom IDs can be added in config.

> This is an experience-integrity check, not security-grade anti-cheat. Client reports can theoretically be modified or faked.

## Allowed Query Mods

The following are never blocked: JEI, REI, EMI, and other recipe/usage-query mods. Hard-allowed IDs: `jei`, `roughlyenoughitems`, `roughlyenoughitems-api`, `emi`.

## Compatibility

| Environment | Status |
|-------------|--------|
| Vanilla Fabric | ✅ Verified |
| Single-player | ✅ Verified |
| Multiplayer (client + server) | ✅ Verified |
| Dedicated server | ✅ Verified (Mod Menu not required) |
| Sodium | Not fully verified |
| Iris | Not fully verified |

## Known Issues

No confirmed critical issues at this time. If you encounter problems, please file an Issue with your `latest.log`.

## Feedback

Please submit to [GitHub Issues](https://github.com/ikunkk02-afk/blindness/issues) with:

- Minecraft version & Fabric Loader version
- Mod version & dependency versions
- `latest.log` and crash report (if any)
- Reproduction steps
- Installed mods list

## Building

```bash
./gradlew build
```

Windows:

```powershell
.\gradlew.bat build
```

Output in `build/libs/`.

## License

MIT. See [LICENSE](LICENSE).

## Acknowledgements

Built with:

- [Fabric API](https://github.com/FabricMC/fabric)
- [Veil](https://github.com/FoundryMC/Veil)
- [Cardinal Components API](https://github.com/Ladysnake/Cardinal-Components-API)
- [owo-lib](https://github.com/wisp-forest/owo-lib)
- [Player Animator](https://github.com/KosmX/playerAnimator)
- [Mod Menu](https://github.com/TerraformersMC/ModMenu)
