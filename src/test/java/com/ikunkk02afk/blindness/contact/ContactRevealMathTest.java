package com.ikunkk02afk.blindness.contact;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContactRevealMathTest {
    @Test void localOffsetsContainOnlyCenterAndSixOrthogonalNeighbors() {
        assertEquals(7, ContactRevealMath.LOCAL_OFFSETS.size());
        assertEquals(7, ContactRevealMath.LOCAL_OFFSETS.stream().distinct().count());
        assertTrue(ContactRevealMath.LOCAL_OFFSETS.stream()
                .allMatch(pos -> ContactRevealMath.isDirectContactOffset(pos.getX(), pos.getY(), pos.getZ())));
        assertFalse(ContactRevealMath.isDirectContactOffset(1, 1, 0));
        assertFalse(ContactRevealMath.isDirectContactOffset(2, 0, 0));
    }

    @Test void timelineDelaysAppearsHoldsAndSmoothlyFades() {
        long fadeIn = 80;
        long hold = 1_200;
        long fadeOut = 700;
        assertEquals(0F, ContactRevealTimeline.alpha(-1, fadeIn, hold, fadeOut, 1F));
        assertEquals(0.5F, ContactRevealTimeline.alpha(40, fadeIn, hold, fadeOut, 1F), 0.001F);
        assertEquals(1F, ContactRevealTimeline.alpha(500, fadeIn, hold, fadeOut, 1F));
        assertEquals(0.5F, ContactRevealTimeline.alpha(1_630, fadeIn, hold, fadeOut, 1F), 0.001F);
        assertEquals(0F, ContactRevealTimeline.alpha(1_980, fadeIn, hold, fadeOut, 1F));
    }
}
