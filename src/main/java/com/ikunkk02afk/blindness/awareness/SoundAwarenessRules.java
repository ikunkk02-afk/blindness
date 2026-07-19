package com.ikunkk02afk.blindness.awareness;

public final class SoundAwarenessRules {
    private static final int[] BLOCK_LIMITS = {0, 12, 32, 56, 80};

    private SoundAwarenessRules() {}

    public static boolean isChunkInRange(int playerChunkX, int playerChunkZ,
                                         int sourceChunkX, int sourceChunkZ, int radius) {
        int safeRadius = Math.clamp(radius, 0, 2);
        return Math.abs(sourceChunkX - playerChunkX) <= safeRadius
                && Math.abs(sourceChunkZ - playerChunkZ) <= safeRadius;
    }

    public static int blockLimit(int radius, int serverMaximum) {
        int safeRadius = Math.clamp(radius, 0, 4);
        return Math.min(BLOCK_LIMITS[safeRadius], Math.clamp(serverMaximum, 0, 80));
    }
}
