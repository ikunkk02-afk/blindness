package com.ikunkk02afk.blindness.fall;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TripDamageRulesTest {
    @Test void normalTripsCannotExceedConfiguredMaximum() {
        assertEquals(2F, TripDamageRules.cap(8F, 2.0));
        assertEquals(0F, TripDamageRules.cap(-1F, 2.0));
        assertEquals(4F, TripDamageRules.cap(8F, 99.0));
    }
}
