# Changelog

## [1.0.0] - 2026-07-19

### Added
- Complete visual darkness with pitch-black world rendering via Veil post-processing pipeline
- Guidance cane item with tap (short click) and sweep (hold) probing modes
- Accurate baked-model contact outlines with glow, occlusion, and depth masking
- Cliff and drop detection system: four-block forward sampling, dual-ping for drops, triple severe alert for lava/void
- Trip-and-fall mechanics: hazard detection (obstacles, mobs, elevation changes), balance meter, movement lock, get-up animation
- Creature sound echo system: on-screen markers at correct body-anchored height, screen-edge markers for off-screen sounds
- Entity sound classification across eight categories (footstep, movement, ambient, hurt, attack, death)
- Vague hostile awareness: periodic spatial query, non-specific text warnings, directional subtitle blurring
- Configurable listening chunk radius (0/1/2) and block outline reveal radius (0–4), synced and persisted server-side
- Information mod compatibility check: pre-world-entry detection of map/minimap/block-info-HUD mods with readable conflict screen
- Hard-allowed recipe viewer list (JEI, REI, EMI) bypasses blocking
- owo-lib config UI with Mod Menu integration and in-game keybind (B)
- Full Chinese (zh_cn.json) and English (en_us.json) localization
- Server-authoritative gameplay logic (cane contacts, sound awareness, cliffs, falls) with client rendering only
- Unit test suite covering sound classification, trip damage, contact math, payload bounds, chunk range, and resource integrity

### Changed
- Replaced initial prototype wave/scan system with contact-based cane probing using real collision rays
- Replaced generic box outlines with baked-model outlines using block model vertex data and alpha-cutout textures
- Upgraded cliff warnings from simple drop detection to configurable server-side sampling with cooldown and severity tiers
- Enhanced sound echo display with body-anchored positioning, categorized heights, and occlusion-aware rendering

### Fixed
- Pause menu no longer reveals the world behind it
- First-person outlines now render with the same depth-correct mask as third-person
- Outline rendering properly uses block model data instead of degrading to full-cube bounding boxes
- Sound echoes correctly anchor to creature body height rather than foot-level or sky position
- Off-screen sound echoes project to screen edges using camera and projection matrices
- Entity sound classification now safely handles unregistered SoundEvents (fixes axolotl crash)
