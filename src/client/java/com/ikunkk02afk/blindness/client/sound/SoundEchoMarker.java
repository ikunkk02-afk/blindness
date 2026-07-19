package com.ikunkk02afk.blindness.client.sound;

import com.ikunkk02afk.blindness.awareness.EntitySoundCategory;
import com.ikunkk02afk.blindness.awareness.SoundOcclusion;
import net.minecraft.util.math.Vec3d;

public final class SoundEchoMarker {
    private final Vec3d position;
    private final EntitySoundCategory category;
    private final boolean hostile;
    private final SoundOcclusion occlusion;
    private final long serverGameTime;
    private final int seed;
    private float strength;
    private long startedNanos;
    private int pulses = 1;

    SoundEchoMarker(Vec3d position, EntitySoundCategory category, float strength, boolean hostile,
                    SoundOcclusion occlusion, long serverGameTime, long now) {
        this.position = position;
        this.category = category;
        this.strength = strength;
        this.hostile = hostile;
        this.occlusion = occlusion;
        this.serverGameTime = serverGameTime;
        this.startedNanos = now;
        this.seed = position.hashCode() * 31 + Long.hashCode(serverGameTime);
    }

    void pulse(float newStrength, long now) {
        strength = Math.max(strength, newStrength);
        startedNanos = now;
        pulses = Math.min(4, pulses + 1);
    }

    public Vec3d position() { return position; }
    public EntitySoundCategory category() { return category; }
    public float strength() { return strength; }
    public boolean hostile() { return hostile; }
    public SoundOcclusion occlusion() { return occlusion; }
    public long serverGameTime() { return serverGameTime; }
    public long startedNanos() { return startedNanos; }
    public int pulses() { return pulses; }
    public int seed() { return seed; }
}
