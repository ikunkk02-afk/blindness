package com.ikunkk02afk.blindness.awareness;

import org.junit.jupiter.api.Test;

import static com.ikunkk02afk.blindness.awareness.SoundEchoProjectionRules.ProjectionState.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SoundEchoProjectionRulesTest {
    @Test void negativeOrZeroWIsNeverProjectedOnScreen() {
        assertEquals(BEHIND_CAMERA, SoundEchoProjectionRules.classify(0.2F, 0.1F, 0F, -1F));
        assertEquals(BEHIND_CAMERA, SoundEchoProjectionRules.classify(0.2F, 0.1F, 0F, 0F));
    }

    @Test void validClipCoordinatesMustAlsoPassDepthAndFiniteChecks() {
        assertEquals(ON_SCREEN, SoundEchoProjectionRules.classify(0.2F, -0.3F, 0.5F, 1F));
        assertEquals(OFF_SCREEN, SoundEchoProjectionRules.classify(1.2F, 0F, 0.5F, 1F));
        assertEquals(OFF_SCREEN, SoundEchoProjectionRules.classify(0F, 0F, 1.2F, 1F));
        assertEquals(REJECTED, SoundEchoProjectionRules.classify(Float.NaN, 0F, 0F, 1F));
    }
}
