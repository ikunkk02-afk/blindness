package com.ikunkk02afk.blindness.client.contact;

import com.ikunkk02afk.blindness.client.BlindnessClient;
import com.ikunkk02afk.blindness.network.BlindnessPayloads;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ContactRevealManager {
    public static final int MAX_ACTIVE_REVEALS = 48;
    private static final Map<BlockPos, RevealedBlock> REVEALS = new LinkedHashMap<>();

    private ContactRevealManager() {}

    public static void accept(BlockPos center, List<BlindnessPayloads.ContactEntry> entries) {
        if (entries.isEmpty() || entries.size() > BlindnessPayloads.ContactReveal.MAX_ENTRIES) return;
        long now = System.nanoTime();
        long delay = secondsToNanos(BlindnessClient.CONFIG.adjacentRevealDelay());
        long fadeIn = secondsToNanos(BlindnessClient.CONFIG.contactFadeInTime());
        long hold = secondsToNanos(BlindnessClient.CONFIG.contactHoldTime());
        long fade = secondsToNanos(BlindnessClient.CONFIG.contactFadeOutTime());
        for (BlindnessPayloads.ContactEntry entry : entries) {
            if (!entry.isValid()) return;
            BlockPos pos = entry.resolve(center).toImmutable();
            RevealedBlock existing = REVEALS.get(pos);
            if (existing == null) {
                REVEALS.put(pos, new RevealedBlock(pos, entry.isCenter(), entry.visibleFaces(), now,
                        delay, fadeIn, hold, fade, entry.isCenter() ? 1F : 0.75F));
            } else {
                existing.refresh(entry.isCenter(), entry.visibleFaces(), now, delay, fadeIn, hold, fade,
                        entry.isCenter() ? 1F : 0.75F);
            }
        }
        while (REVEALS.size() > MAX_ACTIVE_REVEALS) {
            BlockPos earliest = REVEALS.entrySet().stream()
                    .min(Comparator.comparingLong(entry -> entry.getValue().endNanos()))
                    .map(Map.Entry::getKey).orElse(null);
            if (earliest == null) break;
            REVEALS.remove(earliest);
        }
    }

    public static void tick(long now) { REVEALS.values().removeIf(reveal -> reveal.endNanos() <= now); }
    public static List<RevealedBlock> snapshot() { return List.copyOf(new ArrayList<>(REVEALS.values())); }
    public static int size() { return REVEALS.size(); }
    public static void clear() { REVEALS.clear(); }

    private static long secondsToNanos(double seconds) { return (long) (seconds * 1_000_000_000L); }
}
