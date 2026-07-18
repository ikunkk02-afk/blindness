package com.ikunkk02afk.blindness.fall;

public final class TripDamageRules {
    private TripDamageRules() {}

    public static float cap(float proposedDamage, double configuredMaximum) {
        return (float) Math.max(0.0, Math.min(proposedDamage, Math.clamp(configuredMaximum, 0.0, 4.0)));
    }
}
