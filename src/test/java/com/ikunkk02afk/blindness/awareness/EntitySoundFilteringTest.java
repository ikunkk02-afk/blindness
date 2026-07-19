package com.ikunkk02afk.blindness.awareness;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntitySoundFilteringTest {
    @Test void acceptsStructuredEntitySoundActions() {
        assertEquals(EntitySoundCategory.AMBIENT, EntitySoundRevealService.classifyPath("entity.cow.ambient"));
        assertEquals(EntitySoundCategory.FOOTSTEP, EntitySoundRevealService.classifyPath("entity.zombie.step"));
        assertEquals(EntitySoundCategory.ATTACK, EntitySoundRevealService.classifyPath("entity.creeper.primed"));
        assertEquals(EntitySoundCategory.MOVEMENT, EntitySoundRevealService.classifyPath("entity.bat.fly"));
    }

    @Test void rejectsPlayerIndependentAndMisleadingWorldSounds() {
        assertNull(EntitySoundRevealService.classifyPath("block.grass.step"));
        assertNull(EntitySoundRevealService.classifyPath("weather.rain.above"));
        assertNull(EntitySoundRevealService.classifyPath("ambient.cave"));
        assertNull(EntitySoundRevealService.classifyPath("music.overworld.day"));
        assertNull(EntitySoundRevealService.classifyPath("block.note_block.basedrum"));
    }

    @Test void bodyAnchorsStaySeparateFromGroundOrigins() {
        assertEquals(0.35, EntitySoundRevealService.echoHeightFraction(EntitySoundCategory.FOOTSTEP));
        assertEquals(0.65, EntitySoundRevealService.echoHeightFraction(EntitySoundCategory.AMBIENT));
        assertEquals(0.65, EntitySoundRevealService.echoHeightFraction(EntitySoundCategory.HURT));
    }
}
