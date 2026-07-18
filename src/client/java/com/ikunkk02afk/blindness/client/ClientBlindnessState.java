package com.ikunkk02afk.blindness.client;

import com.ikunkk02afk.blindness.network.BlindnessPayloads;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public final class ClientBlindnessState {
    private static final List<ScanWave> WAVES = new ArrayList<>();
    private static final List<CreaturePulse> CREATURE_PULSES = new ArrayList<>();
    private static AuthorizedScan authorizedScan;
    private static long controlsUnlockNanos;
    private static long fallStartNanos;
    private static long fallDurationNanos;
    private static int fallSeed;
    private static RegistryKey<World> lastWorld;
    private static ServerConfigSnapshot serverConfig = new ServerConfigSnapshot(4, 5, 10, 20, 60, 96);

    private ClientBlindnessState() {}

    public static void startFall(int durationTicks, int seed) {
        fallStartNanos = System.nanoTime();
        fallDurationNanos = Math.clamp(durationTicks, 1, 60) * 50_000_000L;
        controlsUnlockNanos = fallStartNanos + fallDurationNanos;
        fallSeed = seed;
    }

    public static void startGetUp(int ticks) {
        controlsUnlockNanos = Math.max(controlsUnlockNanos, System.nanoTime() + Math.clamp(ticks, 1, 20) * 50_000_000L);
    }

    public static void addWave(int scanId, BlockPos origin, float radius, int durationTicks, byte mode) {
        if (!Float.isFinite(radius) || radius < 0.5F || radius > 8F || durationTicks < 1 || durationTicks > 60) return;
        if (WAVES.size() >= 8) WAVES.removeFirst();
        long clientDuration = (long) (Math.clamp(BlindnessClient.CONFIG.outlineDuration(), 0.5, 3.0) * 1_000_000_000L);
        long serverDuration = Math.clamp(durationTicks, 10, 60) * 50_000_000L;
        WAVES.add(new ScanWave(scanId, origin.toCenterPos(), radius, System.nanoTime(), Math.min(clientDuration, serverDuration), mode));
    }

    public static void acceptScanResult(int scanId, BlockPos origin, List<BlindnessPayloads.ScanHit> hits) {
        if (scanId < 0 || hits.isEmpty() || hits.size() > BlindnessPayloads.ScanResult.MAX_HITS) return;
        List<Vec3d> surfaces = new ArrayList<>(hits.size());
        for (BlindnessPayloads.ScanHit hit : hits) {
            Direction face = Direction.byId(hit.face());
            Vec3d center = hit.resolve(origin).toCenterPos();
            surfaces.add(center.add(face.getOffsetX() * 0.51, face.getOffsetY() * 0.51, face.getOffsetZ() * 0.51));
        }
        authorizedScan = new AuthorizedScan(scanId, List.copyOf(surfaces), System.nanoTime());
    }

    public static void addCreaturePulse(double x, double y, double z, float strength) {
        if (!BlindnessClient.CONFIG.showCreatureBlurredOutline() || !Double.isFinite(x + y + z)) return;
        if (CREATURE_PULSES.size() >= 8) CREATURE_PULSES.removeFirst();
        CREATURE_PULSES.add(new CreaturePulse(x, y, z, Math.clamp(strength, 0F, 1F), System.nanoTime()));
    }

    public static void tick(MinecraftClient client) {
        long now = System.nanoTime();
        WAVES.removeIf(wave -> now - wave.startNanos > wave.durationNanos);
        CREATURE_PULSES.removeIf(pulse -> now - pulse.startNanos > 350_000_000L);
        if (authorizedScan != null && now - authorizedScan.receivedNanos() > 3_000_000_000L) authorizedScan = null;
        if (client.world == null) {
            clear();
        } else if (lastWorld == null || !lastWorld.equals(client.world.getRegistryKey())) {
            clearTransient();
            lastWorld = client.world.getRegistryKey();
        }
    }

    public static ScanWave activeWave() {
        return WAVES.isEmpty() ? null : WAVES.getLast();
    }

    public static CreaturePulse activeCreaturePulse() {
        return CREATURE_PULSES.isEmpty() ? null : CREATURE_PULSES.getLast();
    }

    public static List<Vec3d> authorizedSurfaces(int scanId) {
        return authorizedScan != null && authorizedScan.scanId() == scanId ? authorizedScan.surfaces() : List.of();
    }

    public static boolean controlsLocked() { return System.nanoTime() < controlsUnlockNanos; }

    public static float fallProgress() {
        if (fallDurationNanos <= 0) return 0;
        return (float) Math.clamp((double) (System.nanoTime() - fallStartNanos) / fallDurationNanos, 0.0, 1.0);
    }

    public static int fallSeed() { return fallSeed; }
    public static ServerConfigSnapshot serverConfig() { return serverConfig; }
    public static void setServerConfig(ServerConfigSnapshot snapshot) { serverConfig = snapshot; }

    public static void clear() {
        clearTransient();
        lastWorld = null;
    }

    private static void clearTransient() {
        WAVES.clear();
        CREATURE_PULSES.clear();
        authorizedScan = null;
        controlsUnlockNanos = 0;
        fallStartNanos = 0;
        fallDurationNanos = 0;
    }

    public record ScanWave(int scanId, Vec3d origin, float maxRadius, long startNanos, long durationNanos, byte mode) {
        public float progress() { return (float) Math.clamp((double) (System.nanoTime() - startNanos) / durationNanos, 0.0, 1.0); }
        public float elapsedSeconds() { return (System.nanoTime() - startNanos) / 1_000_000_000F; }
    }
    private record AuthorizedScan(int scanId, List<Vec3d> surfaces, long receivedNanos) {}
    public record CreaturePulse(double x, double y, double z, float strength, long startNanos) {}
    public record ServerConfigSnapshot(int tapRange, int sweepRange, int tapCooldown, int sweepCooldown, int pathTtl, int maxHits) {}
}
