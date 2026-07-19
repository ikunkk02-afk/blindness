package com.ikunkk02afk.blindness.fall;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FallControlLockTest {
    @Test void fallingLocksPlayerForExactlyFiveSeconds() {
        assertEquals(100, FallStateManager.controlLockTicks());
    }
}
