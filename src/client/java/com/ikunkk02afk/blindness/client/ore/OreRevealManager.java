package com.ikunkk02afk.blindness.client.ore;

import com.ikunkk02afk.blindness.accessibility.OreType;
import com.ikunkk02afk.blindness.network.BlindnessPayloads;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side manager for ore reveal data.
 * Stores which blocks are ores and their types for outline rendering.
 */
public final class OreRevealManager {
    private static final Map<BlockPos, OreRevealEntry> ORES = new HashMap<>();

    private OreRevealManager() {}

    public static void accept(BlockPos center, List<BlindnessPayloads.OreDetected> ores) {
        long now = System.nanoTime();
        for (BlindnessPayloads.OreDetected ore : ores) {
            OreType type = OreType.values()[Math.clamp(ore.oreType(), 0, OreType.values().length - 1)];
            ORES.put(ore.pos().toImmutable(), new OreRevealEntry(type, ore.faces(), now));
        }
    }

    public static OreRevealEntry get(BlockPos pos) {
        OreRevealEntry entry = ORES.get(pos);
        if (entry == null) return null;
        // Ore reveals persist for about 3 seconds (nanos).
        long age = System.nanoTime() - entry.startNanos();
        if (age > 3_000_000_000L) {
            ORES.remove(pos);
            return null;
        }
        return entry;
    }

    public static void clear() {
        ORES.clear();
    }

    public static int size() {
        return ORES.size();
    }

    public record OreRevealEntry(OreType type, int faces, long startNanos) {
        public float alpha(long now) {
            long age = now - startNanos();
            long duration = 3_000_000_000L;
            if (age <= 0) return 1F;
            if (age >= duration) return 0F;
            // Quick fade-in, long hold, quick fade-out.
            long fadeIn = 100_000_000L;  // 100ms
            long fadeOut = 400_000_000L; // 400ms
            if (age < fadeIn) return (float) age / fadeIn;
            if (age > duration - fadeOut) return (float) (duration - age) / fadeOut;
            return 1F;
        }
    }
}
