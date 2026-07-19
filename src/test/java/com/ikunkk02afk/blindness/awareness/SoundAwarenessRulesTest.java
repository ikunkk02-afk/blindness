package com.ikunkk02afk.blindness.awareness;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SoundAwarenessRulesTest {
    @Test void radiusOneCoversExactlyThreeByThreeChunks() {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            assertTrue(SoundAwarenessRules.isChunkInRange(10, -4, 10 + dx, -4 + dz, 1));
        }
        assertFalse(SoundAwarenessRules.isChunkInRange(10, -4, 12, -4, 1));
        assertFalse(SoundAwarenessRules.isChunkInRange(10, -4, 10, -6, 1));
    }

    @Test void chunkBoundariesDoNotUseEuclideanDistance() {
        assertTrue(SoundAwarenessRules.isChunkInRange(0, 0, 1, 1, 1));
        assertFalse(SoundAwarenessRules.isChunkInRange(0, 0, 2, 0, 1));
        assertTrue(SoundAwarenessRules.isChunkInRange(0, 0, 2, -2, 2));
    }

    @Test void blockLimitsMatchConfiguredRadiusAndServerCap() {
        assertEquals(0, SoundAwarenessRules.blockLimit(0, 80));
        assertEquals(12, SoundAwarenessRules.blockLimit(1, 80));
        assertEquals(32, SoundAwarenessRules.blockLimit(2, 80));
        assertEquals(56, SoundAwarenessRules.blockLimit(3, 80));
        assertEquals(80, SoundAwarenessRules.blockLimit(4, 80));
        assertEquals(40, SoundAwarenessRules.blockLimit(4, 40));
    }
}
