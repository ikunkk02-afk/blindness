package com.ikunkk02afk.blindness.scan;

public final class ScanWaveMath {
    private ScanWaveMath() {}

    public static float radius(float maximumRadius, float progress) {
        return Math.max(0F, maximumRadius) * Math.min(1F, Math.max(0F, progress) / (0.5F / 1.5F));
    }

    public static float fade(float progress) {
        float p = Math.clamp(progress, 0F, 1F);
        float fadeStart = 0.8F / 1.5F;
        if (p <= fadeStart) return 1F;
        return Math.clamp(1F - (p - fadeStart) / (0.7F / 1.5F), 0F, 1F);
    }
}
