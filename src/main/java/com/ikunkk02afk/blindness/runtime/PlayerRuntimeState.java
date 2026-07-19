package com.ikunkk02afk.blindness.runtime;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.util.math.Vec3d;


public final class PlayerRuntimeState {
    public double balance = 100.0;
    public long fallEndsAt;
    public long controlsUnlockAt;
    public long protectedUntil;
    public long tinnitusUntil;
    public long lastContactAt;
    public long lastStableRecoveryAt;
    public long hazardFingerprint = Long.MIN_VALUE;
    public boolean caneSweepStarted;
    public boolean caneSweepFinished;
    public int nextSweepContact;
    public boolean getUpSent;
    public int nextContactSequence;
    public RegistryKey<World> worldKey;
    public Vec3d lastHorizontalVelocity = Vec3d.ZERO;
    public final BoundedExpiryMap scannedPath = new BoundedExpiryMap();

    public boolean isFalling(long tick) {
        return tick < controlsUnlockAt;
    }

    public void resetTransient(RegistryKey<World> newWorld) {
        balance = 100.0;
        fallEndsAt = 0;
        controlsUnlockAt = 0;
        protectedUntil = 0;
        tinnitusUntil = 0;
        lastContactAt = 0;
        hazardFingerprint = Long.MIN_VALUE;
        caneSweepStarted = false;
        caneSweepFinished = false;
        nextSweepContact = 0;
        getUpSent = false;
        lastHorizontalVelocity = Vec3d.ZERO;
        scannedPath.clear();
        worldKey = newWorld;
    }
}
