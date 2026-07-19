package com.ikunkk02afk.blindness.awareness;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the null-safety fixes in EntitySoundRevealService.
 * <p>
 * The original crash was caused by unregistered SoundEvents (e.g. from resource packs,
 * mod-added sounds, or unregistered vanilla sounds like axolotl) where
 * {@code Registries.SOUND_EVENT.getId(sound)} returned {@code null}, and
 * {@code classify()} directly called {@code id.getPath()} without a null check.
 * <p>
 * In the unit-test environment Minecraft registries are frozen and
 * {@code SoundEvent.of()} rejects unregistered identifiers, so the
 * null-id code path is verified through design review rather than
 * direct registry manipulation. The {@code classify(null)} test covers
 * the explicit null-parameter guard.
 */
class EntitySoundRevealServiceNullSafetyTest {

    // ── classify() null-safety ──────────────────────────────────────────

    @Test
    void classifyNullSoundReturnsNull() {
        assertNull(EntitySoundRevealService.classify(null));
    }

    // ── classifyPath() null-safety ───────────────────────────────────────

    @Test
    void classifyPathNullReturnsNull() {
        assertNull(EntitySoundRevealService.classifyPath(null));
    }

    @Test
    void classifyPathEmptyStringReturnsNull() {
        assertNull(EntitySoundRevealService.classifyPath(""));
    }

    @Test
    void classifyPathSingleWordReturnsNull() {
        assertNull(EntitySoundRevealService.classifyPath("axolotl"));
    }

    @Test
    void classifyPathNonEntitySoundReturnsNull() {
        assertNull(EntitySoundRevealService.classifyPath("block.grass.step"));
        assertNull(EntitySoundRevealService.classifyPath("weather.rain.above"));
        assertNull(EntitySoundRevealService.classifyPath("ambient.cave"));
    }

    @Test
    void unregisteredStyleSoundPathsReturnNullSafely() {
        // Sound paths that look like entity sounds but don't match any
        // known action keyword — these would crash before the fix if their
        // SoundEvent had no registry ID. Now they safely return null.
        assertNull(EntitySoundRevealService.classifyPath("entity.axolotl.idle_air"));
        assertNull(EntitySoundRevealService.classifyPath("entity.cod.flop"));
        assertNull(EntitySoundRevealService.classifyPath("entity.salmon.flop"));
    }

    // ── Normal entity sounds still work after the fix ────────────────────

    @Test
    void axolotlKnownSoundsAreClassifiedCorrectly() {
        // Axolotl was the original crash trigger — verify known sounds work
        assertEquals(EntitySoundCategory.DEATH, EntitySoundRevealService.classifyPath("entity.axolotl.death"));
        assertEquals(EntitySoundCategory.HURT, EntitySoundRevealService.classifyPath("entity.axolotl.hurt"));
    }

    @Test
    void aquaticEntitySoundsAreClassifiedCorrectly() {
        assertEquals(EntitySoundCategory.HURT, EntitySoundRevealService.classifyPath("entity.cod.hurt"));
        assertEquals(EntitySoundCategory.DEATH, EntitySoundRevealService.classifyPath("entity.cod.death"));
        assertEquals(EntitySoundCategory.HURT, EntitySoundRevealService.classifyPath("entity.salmon.hurt"));
        assertEquals(EntitySoundCategory.DEATH, EntitySoundRevealService.classifyPath("entity.salmon.death"));
    }

    @Test
    void standardEntitySoundsPreserveClassification() {
        // Death / hurt
        assertEquals(EntitySoundCategory.DEATH, EntitySoundRevealService.classifyPath("entity.zombie.death"));
        assertEquals(EntitySoundCategory.HURT, EntitySoundRevealService.classifyPath("entity.skeleton.hurt"));
        // Footsteps
        assertEquals(EntitySoundCategory.FOOTSTEP, EntitySoundRevealService.classifyPath("entity.zombie.step"));
        assertEquals(EntitySoundCategory.FOOTSTEP, EntitySoundRevealService.classifyPath("entity.cow.step"));
        // Movement (swim, splash, flap, fly, jump, land)
        assertEquals(EntitySoundCategory.MOVEMENT, EntitySoundRevealService.classifyPath("entity.bat.fly"));
        assertEquals(EntitySoundCategory.MOVEMENT, EntitySoundRevealService.classifyPath("entity.rabbit.jump"));
        assertEquals(EntitySoundCategory.MOVEMENT, EntitySoundRevealService.classifyPath("entity.axolotl.swim"));
        // Ambient
        assertEquals(EntitySoundCategory.AMBIENT, EntitySoundRevealService.classifyPath("entity.cow.ambient"));
        assertEquals(EntitySoundCategory.AMBIENT, EntitySoundRevealService.classifyPath("entity.warden.roar"));
        assertEquals(EntitySoundCategory.AMBIENT, EntitySoundRevealService.classifyPath("entity.wolf.growl"));
        // Attack
        assertEquals(EntitySoundCategory.ATTACK, EntitySoundRevealService.classifyPath("entity.creeper.primed"));
        assertEquals(EntitySoundCategory.ATTACK, EntitySoundRevealService.classifyPath("entity.warden.attack"));
    }

    // ── handleSound() null-safety ────────────────────────────────────────

    @Test
    void handleSoundWithNullParametersDoesNotCrash() {
        // All-null: must return silently without throwing
        assertDoesNotThrow(() ->
                EntitySoundRevealService.handleSound(null, null, null, 1.0F, 1.0F));
    }
}
