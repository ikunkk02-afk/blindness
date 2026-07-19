package com.ikunkk02afk.blindness.client.sound;

import com.ikunkk02afk.blindness.awareness.EntitySoundCategory;
import com.ikunkk02afk.blindness.awareness.SoundOcclusion;
import com.ikunkk02afk.blindness.client.BlindnessClient;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SoundEchoMarkerManager {
    private static final List<SoundEchoMarker> MARKERS = new ArrayList<>();

    private SoundEchoMarkerManager() {}

    public static void accept(Vec3d position, EntitySoundCategory category, float strength,
                              boolean hostile, SoundOcclusion occlusion, long serverGameTime) {
        if (!BlindnessClient.CONFIG.entitySoundEchoEnabled()) return;
        long now = System.nanoTime();
        SoundEchoMarker existing = MARKERS.stream()
                .filter(marker -> marker.hostile() == hostile && marker.category() == category
                        && marker.position().squaredDistanceTo(position) <= 1.0
                        && now - marker.startedNanos() <= 350_000_000L)
                .findFirst().orElse(null);
        if (existing != null) existing.pulse(strength, now);
        else MARKERS.add(new SoundEchoMarker(position, category, strength, hostile, occlusion, serverGameTime, now));
        trim();
    }

    public static void tick(long now) {
        MARKERS.removeIf(marker -> ageSeconds(marker, now) > totalDuration(marker));
        trim();
    }

    public static List<SoundEchoMarker> snapshot() { return List.copyOf(MARKERS); }
    public static void clear() { MARKERS.clear(); }

    public static float alpha(SoundEchoMarker marker, long now) {
        double age = ageSeconds(marker, now);
        double scale = 0.9 + marker.strength() * 0.25;
        double fadeIn = BlindnessClient.CONFIG.soundEchoFadeInTime();
        double hold = BlindnessClient.CONFIG.soundEchoHoldTime() * scale;
        double fadeOut = BlindnessClient.CONFIG.soundEchoFadeOutTime() * scale;
        double alpha;
        if (age < fadeIn) alpha = age / Math.max(0.001, fadeIn);
        else if (age < fadeIn + hold) alpha = 1.0;
        else alpha = 1.0 - (age - fadeIn - hold) / Math.max(0.001, fadeOut);
        double occlusion = switch (marker.occlusion()) {
            case CLEAR -> 1.0;
            case PARTIAL -> 0.72;
            case OCCLUDED -> 0.46;
        };
        return (float) Math.clamp(alpha * occlusion * BlindnessClient.CONFIG.soundEchoBrightness(), 0.0, 1.0);
    }

    private static double totalDuration(SoundEchoMarker marker) {
        double scale = 0.9 + marker.strength() * 0.25;
        return BlindnessClient.CONFIG.soundEchoFadeInTime()
                + (BlindnessClient.CONFIG.soundEchoHoldTime() + BlindnessClient.CONFIG.soundEchoFadeOutTime()) * scale;
    }

    private static double ageSeconds(SoundEchoMarker marker, long now) {
        return Math.max(0.0, (now - marker.startedNanos()) / 1_000_000_000.0);
    }

    private static void trim() {
        int maximum = Math.clamp(BlindnessClient.CONFIG.maximumActiveSoundEchoes(), 4, 48);
        while (MARKERS.size() > maximum) {
            SoundEchoMarker victim = MARKERS.stream().min(Comparator
                    .comparing(SoundEchoMarker::hostile)
                    .thenComparingLong(SoundEchoMarker::startedNanos)).orElse(null);
            if (victim == null) break;
            MARKERS.remove(victim);
        }
    }
}
