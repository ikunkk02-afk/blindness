package com.ikunkk02afk.blindness.client.contact;

import net.minecraft.util.math.BlockPos;
import com.ikunkk02afk.blindness.contact.ContactRevealTimeline;
import com.ikunkk02afk.blindness.awareness.RevealSource;

public final class RevealedBlock {
    private final BlockPos pos;
    private RevealSource source;
    private int visibleFaces;
    private long startNanos;
    private long delayNanos;
    private long fadeInNanos;
    private long holdNanos;
    private long fadeOutNanos;
    private float intensity;

    RevealedBlock(BlockPos pos, RevealSource source, int visibleFaces, long startNanos,
                  long delayNanos, long fadeInNanos, long holdNanos, long fadeOutNanos, float intensity) {
        this.pos = pos.toImmutable();
        refresh(source, visibleFaces, startNanos, delayNanos, fadeInNanos, holdNanos, fadeOutNanos, intensity);
    }

    void refresh(RevealSource newSource, int newVisibleFaces, long now, long delay, long fadeIn,
                 long hold, long fadeOut, float newIntensity) {
        long oldEnd = endNanos();
        if (source == null || priority(newSource) > priority(source)) source = newSource;
        visibleFaces |= newVisibleFaces;
        startNanos = now;
        delayNanos = source == RevealSource.CANE_CENTER ? 0 : delay;
        fadeInNanos = fadeIn;
        long requestedEnd = now + delayNanos + fadeIn + hold + fadeOut;
        long finalEnd = Math.max(oldEnd, requestedEnd);
        holdNanos = Math.max(0, finalEnd - now - delayNanos - fadeIn - fadeOut)
                + (source == RevealSource.CANE_CENTER ? 100_000_000L : 0);
        fadeOutNanos = fadeOut;
        intensity = Math.max(intensity, source == RevealSource.CANE_CENTER ? 1.0F : newIntensity);
    }

    public BlockPos pos() { return pos; }
    public boolean isCenter() { return source == RevealSource.CANE_CENTER; }
    public RevealSource source() { return source; }
    public int priority() { return priority(source); }
    public int visibleFaces() { return visibleFaces; }
    public long endNanos() { return startNanos + delayNanos + fadeInNanos + holdNanos + fadeOutNanos; }

    public float alpha(long now) {
        long localAge = now - startNanos - delayNanos;
        return ContactRevealTimeline.alpha(localAge, fadeInNanos, holdNanos, fadeOutNanos, intensity);
    }

    private static int priority(RevealSource source) {
        return switch (source) {
            case CANE_CENTER -> 5;
            case CANE_ADJACENT -> 4;
            case ENTITY_DANGER -> 3;
            case ENTITY_AMBIENT -> 2;
            case ENTITY_FOOTSTEP -> 1;
        };
    }
}
