package com.ikunkk02afk.blindness.client.contact;

import com.ikunkk02afk.blindness.client.BlindnessClient;
import com.ikunkk02afk.blindness.network.BlindnessPayloads;
import com.ikunkk02afk.blindness.awareness.RevealSource;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ContactRevealManager {
    public static final int MAX_ACTIVE_REVEALS = 128;
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
                RevealSource source = entry.isCenter() ? RevealSource.CANE_CENTER : RevealSource.CANE_ADJACENT;
                REVEALS.put(pos, new RevealedBlock(pos, source, entry.visibleFaces(), now,
                        delay, fadeIn, hold, fade, entry.isCenter() ? 1F : 0.75F));
            } else {
                existing.refresh(entry.isCenter() ? RevealSource.CANE_CENTER : RevealSource.CANE_ADJACENT,
                        entry.visibleFaces(), now, delay, fadeIn, hold, fade,
                        entry.isCenter() ? 1F : 0.75F);
            }
        }
        while (REVEALS.size() > MAX_ACTIVE_REVEALS) {
            BlockPos earliest = REVEALS.entrySet().stream()
                    .min(Comparator.<Map.Entry<BlockPos, RevealedBlock>>comparingInt(entry -> entry.getValue().priority())
                            .thenComparingLong(entry -> entry.getValue().endNanos()))
                    .map(Map.Entry::getKey).orElse(null);
            if (earliest == null) break;
            REVEALS.remove(earliest);
        }
    }

    public static void acceptSound(BlockPos center, RevealSource source, float soundStrength,
                                   List<BlindnessPayloads.SoundRevealEntry> entries) {
        if (source == null || source == RevealSource.CANE_CENTER || source == RevealSource.CANE_ADJACENT
                || !Float.isFinite(soundStrength) || soundStrength < 0F || soundStrength > 1F
                || entries.isEmpty() || entries.size() > BlindnessPayloads.EntitySoundEcho.MAX_ENTRIES) return;
        long now = System.nanoTime();
        long fadeIn = secondsToNanos(BlindnessClient.CONFIG.entitySoundOutlineFadeInTime());
        long hold = secondsToNanos(BlindnessClient.CONFIG.entitySoundOutlineHoldTime()
                * (source == RevealSource.ENTITY_FOOTSTEP ? 0.72 : 1.0));
        long fade = secondsToNanos(BlindnessClient.CONFIG.entitySoundOutlineFadeOutTime());
        float base = switch (source) {
            case ENTITY_FOOTSTEP -> 0.52F;
            case ENTITY_AMBIENT -> 0.62F;
            case ENTITY_DANGER -> 0.70F;
            default -> 0F;
        };
        float intensity = Math.min(0.75F, base * (0.65F + soundStrength * 0.35F)
                * (float) BlindnessClient.CONFIG.entitySoundOutlineBrightness() / 0.60F);
        for (BlindnessPayloads.SoundRevealEntry entry : entries) {
            if (!entry.isValid()) return;
            BlockPos pos = entry.resolve(center).toImmutable();
            RevealedBlock existing = REVEALS.get(pos);
            if (existing == null) {
                REVEALS.put(pos, new RevealedBlock(pos, source, entry.faces(), now, 0, fadeIn, hold, fade, intensity));
            } else {
                existing.refresh(source, entry.faces(), now, 0, fadeIn, hold, fade, intensity);
            }
        }
        trimToLimit();
    }

    private static void trimToLimit() {
        while (REVEALS.size() > MAX_ACTIVE_REVEALS) {
            BlockPos victim = REVEALS.entrySet().stream()
                    .min(Comparator.<Map.Entry<BlockPos, RevealedBlock>>comparingInt(entry -> entry.getValue().priority())
                            .thenComparingLong(entry -> entry.getValue().endNanos()))
                    .map(Map.Entry::getKey).orElse(null);
            if (victim == null) break;
            REVEALS.remove(victim);
        }
    }

    public static void tick(long now) { REVEALS.values().removeIf(reveal -> reveal.endNanos() <= now); }
    public static List<RevealedBlock> snapshot() { return List.copyOf(new ArrayList<>(REVEALS.values())); }
    public static int size() { return REVEALS.size(); }
    public static void clear() { REVEALS.clear(); }

    private static long secondsToNanos(double seconds) { return (long) (seconds * 1_000_000_000L); }
}
