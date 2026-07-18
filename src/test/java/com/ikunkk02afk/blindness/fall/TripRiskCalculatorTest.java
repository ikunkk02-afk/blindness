package com.ikunkk02afk.blindness.fall;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TripRiskCalculatorTest {
    @Test void flatGroundNeverTrips() {
        assertEquals(0.0, TripRiskCalculator.probability(new TripRiskCalculator.Inputs(0, 0.3, true, 1, 50, false, false, 1)));
    }

    @Test void scannedPathCutsRiskToQuarter() {
        var unsafe = new TripRiskCalculator.Inputs(0.35, 0.30, true, 1, 100, false, false, 1);
        var safe = new TripRiskCalculator.Inputs(0.35, 0.30, true, 1, 100, true, false, 1);
        assertEquals(TripRiskCalculator.probability(unsafe) * 0.25, TripRiskCalculator.probability(safe), 1.0e-9);
    }

    @Test void sneakingMakesRiskNearZeroAndCapIsEnforced() {
        double crouched = TripRiskCalculator.probability(new TripRiskCalculator.Inputs(0.20, 0.30, true, 1.0, 100, false, true, 1));
        double uncrouched = TripRiskCalculator.probability(new TripRiskCalculator.Inputs(0.20, 0.30, true, 1.0, 100, false, false, 1));
        assertTrue(crouched <= uncrouched * 0.051);
        double capped = TripRiskCalculator.probability(new TripRiskCalculator.Inputs(0.55, 0.30, true, 1.4, 0, false, false, 3));
        assertEquals(0.70, capped);
    }
}
