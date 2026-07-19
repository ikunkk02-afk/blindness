package com.ikunkk02afk.blindness.client.contact;

import net.minecraft.util.math.BlockPos;
import com.ikunkk02afk.blindness.contact.ContactRevealTimeline;

public final class RevealedBlock {
    private final BlockPos pos;
    private boolean center;
    private int visibleFaces;
    private long startNanos;
    private long delayNanos;
    private long fadeInNanos;
    private long holdNanos;
    private long fadeOutNanos;
    private float intensity;

    RevealedBlock(BlockPos pos, boolean center, int visibleFaces, long startNanos,
                  long delayNanos, long fadeInNanos, long holdNanos, long fadeOutNanos, float intensity) {
        this.pos = pos.toImmutable();
        refresh(center, visibleFaces, startNanos, delayNanos, fadeInNanos, holdNanos, fadeOutNanos, intensity);
    }

    void refresh(boolean newCenter, int newVisibleFaces, long now, long delay, long fadeIn,
                 long hold, long fadeOut, float newIntensity) {
        center |= newCenter;
        visibleFaces |= newVisibleFaces;
        startNanos = now;
        delayNanos = newCenter ? 0 : delay;
        fadeInNanos = fadeIn;
        holdNanos = hold + (center ? 100_000_000L : 0);
        fadeOutNanos = fadeOut;
        intensity = center ? 1.0F : newIntensity;
    }

    public BlockPos pos() { return pos; }
    public boolean isCenter() { return center; }
    public int visibleFaces() { return visibleFaces; }
    public long endNanos() { return startNanos + delayNanos + fadeInNanos + holdNanos + fadeOutNanos; }

    public float alpha(long now) {
        long localAge = now - startNanos - delayNanos;
        return ContactRevealTimeline.alpha(localAge, fadeInNanos, holdNanos, fadeOutNanos, intensity);
    }
}
