package com.ikunkk02afk.blindness.contact;

public final class ContactRevealTimeline {
    private ContactRevealTimeline() {}

    public static float alpha(long localAge, long fadeIn, long hold, long fadeOut, float intensity) {
        if (localAge < 0) return 0F;
        if (localAge < fadeIn) return intensity * (float) localAge / fadeIn;
        if (localAge < fadeIn + hold) return intensity;
        long fadeAge = localAge - fadeIn - hold;
        if (fadeAge >= fadeOut) return 0F;
        float progress = (float) fadeAge / fadeOut;
        float smooth = progress * progress * (3F - 2F * progress);
        return intensity * (1F - smooth);
    }
}
