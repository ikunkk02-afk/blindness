package com.ikunkk02afk.blindness.scan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScanWaveMathTest {
    @Test void reachesMaximumAtHalfSecondAndFadesByOnePointFive() {
        assertEquals(0F, ScanWaveMath.radius(5F, 0F));
        assertEquals(5F, ScanWaveMath.radius(5F, 0.5F / 1.5F), 0.001F);
        assertEquals(1F, ScanWaveMath.fade(0.8F / 1.5F), 0.001F);
        assertEquals(0F, ScanWaveMath.fade(1F), 0.001F);
    }
}
