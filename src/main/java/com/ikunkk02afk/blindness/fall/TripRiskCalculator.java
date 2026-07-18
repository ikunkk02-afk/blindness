package com.ikunkk02afk.blindness.fall;

public final class TripRiskCalculator {
    private TripRiskCalculator() {}

    public record Inputs(double baseHazard, double horizontalSpeed, boolean sprinting, double materialFactor,
                         double balance, boolean scanned, boolean sneaking, double serverScale) {}

    public static double probability(Inputs input) {
        if (input.baseHazard() <= 0.0 || input.horizontalSpeed() <= 0.06) return 0.0;
        double speedFactor = clamp((input.horizontalSpeed() - 0.06) / 0.24, 0.0, 1.0);
        double sprintFactor = input.sprinting() ? 1.35 : 0.75;
        double balanceFactor = 1.0 + (1.0 - clamp(input.balance() / 100.0, 0.0, 1.0)) * 0.5;
        double scannedFactor = input.scanned() ? 0.25 : 1.0;
        double crouchFactor = input.sneaking() ? 0.05 : 1.0;
        return clamp(input.baseHazard() * speedFactor * sprintFactor * input.materialFactor()
                * balanceFactor * scannedFactor * crouchFactor * input.serverScale(), 0.0, 0.70);
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
