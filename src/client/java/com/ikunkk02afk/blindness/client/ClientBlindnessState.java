package com.ikunkk02afk.blindness.client;

import com.ikunkk02afk.blindness.client.contact.ContactRevealManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.sound.SoundEvents;


public final class ClientBlindnessState {
    private static long controlsUnlockNanos;
    private static long fallStartNanos;
    private static long fallDurationNanos;
    private static int fallSeed;
    private static RegistryKey<World> lastWorld;
    private static boolean wasDead;
    private static ServerConfigSnapshot serverConfig = new ServerConfigSnapshot(10, 20, 50, 8);
    private static int cliffPulsesRemaining;
    private static long nextCliffPulseNanos;
    private static long cliffFeedbackUntilNanos;

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
        if (cliffPulsesRemaining > 0 && now >= nextCliffPulseNanos) {
            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(),
                    (float) BlindnessClient.CONFIG.soundPromptVolume(), cliffPulsesRemaining == 1 ? 1.45F : 1.15F);
            cliffPulsesRemaining--;
            nextCliffPulseNanos = now + 115_000_000L;
        }
        ContactRevealManager.tick(now);
    }

    public static boolean controlsLocked() { return System.nanoTime() < controlsUnlockNanos; }
    public static float fallProgress() {
        if (fallDurationNanos <= 0) return 0;
        return (float) Math.clamp((double) (System.nanoTime() - fallStartNanos) / fallDurationNanos, 0.0, 1.0);
    }
    public static int fallSeed() { return fallSeed; }
    public static void startCliffWarning(boolean severe) {
        cliffPulsesRemaining = severe ? 3 : 2;
        nextCliffPulseNanos = 0;
        cliffFeedbackUntilNanos = System.nanoTime() + (severe ? 420_000_000L : 280_000_000L);
    }
    public static float cliffFeedback() {
        if (!BlindnessClient.CONFIG.cliffCameraFeedback()) return 0F;
        long remaining = cliffFeedbackUntilNanos - System.nanoTime();
        return remaining <= 0 ? 0F : (float) Math.clamp(remaining / 420_000_000.0, 0.0, 1.0);
    }
    public static ServerConfigSnapshot serverConfig() { return serverConfig; }
    public static void setServerConfig(ServerConfigSnapshot snapshot) { serverConfig = snapshot; }

    public static void clear() {
        clearTransient();
        lastWorld = null;
        wasDead = false;
    }

    private static void clearTransient() {
        ContactRevealManager.clear();
        controlsUnlockNanos = 0;
        fallStartNanos = 0;
        fallDurationNanos = 0;
        cliffPulsesRemaining = 0;
        cliffFeedbackUntilNanos = 0;
    }

    public record ServerConfigSnapshot(int tapCooldown, int sweepCooldown, int pathTtl, int maxEntries) {}
}
