package com.ikunkk02afk.blindness.client;

import com.ikunkk02afk.blindness.client.contact.ContactRevealManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public final class ClientBlindnessState {
    private static long controlsUnlockNanos;
    private static long fallStartNanos;
    private static long fallDurationNanos;
    private static int fallSeed;
    private static RegistryKey<World> lastWorld;
    private static boolean wasDead;
    private static final List<CreaturePulse> CREATURE_PULSES = new ArrayList<>();
    private static ServerConfigSnapshot serverConfig = new ServerConfigSnapshot(10, 20, 50, 8);

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

    public static void addCreaturePulse(double x, double y, double z, float strength) {
        if (!BlindnessClient.CONFIG.showCreatureBlurredOutline() || !Double.isFinite(x + y + z)) return;
        if (CREATURE_PULSES.size() >= 8) CREATURE_PULSES.removeFirst();
        CREATURE_PULSES.add(new CreaturePulse(x, y, z, Math.clamp(strength, 0F, 1F), System.nanoTime()));
    }

    public static CreaturePulse activeCreaturePulse() {
        return CREATURE_PULSES.isEmpty() ? null : CREATURE_PULSES.getLast();
    }

    public static void tick(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            clear();
            return;
        }
        if (lastWorld == null || !lastWorld.equals(client.world.getRegistryKey())) {
            clearTransient();
            lastWorld = client.world.getRegistryKey();
        }
        boolean dead = client.player.isDead();
        if (dead && !wasDead) ContactRevealManager.clear();
        wasDead = dead;
        long now = System.nanoTime();
        ContactRevealManager.tick(now);
        CREATURE_PULSES.removeIf(pulse -> now - pulse.startNanos() > 350_000_000L);
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
        wasDead = false;
    }

    private static void clearTransient() {
        ContactRevealManager.clear();
        CREATURE_PULSES.clear();
        controlsUnlockNanos = 0;
        fallStartNanos = 0;
        fallDurationNanos = 0;
    }

    public record ServerConfigSnapshot(int tapCooldown, int sweepCooldown, int pathTtl, int maxEntries) {}
    public record CreaturePulse(double x, double y, double z, float strength, long startNanos) {}
}
