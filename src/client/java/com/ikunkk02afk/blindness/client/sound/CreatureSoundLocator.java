package com.ikunkk02afk.blindness.client.sound;

import com.ikunkk02afk.blindness.client.ClientBlindnessState;
import com.ikunkk02afk.blindness.client.BlindnessClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundInstanceListener;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class CreatureSoundLocator {
    private static final SoundInstanceListener LISTENER = CreatureSoundLocator::onSound;

    private CreatureSoundLocator() {}

    public static void register(MinecraftClient client) {
        client.getSoundManager().registerListener(LISTENER);
    }

    private static void onSound(SoundInstance sound, net.minecraft.client.sound.WeightedSoundSet set, float range) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || !isCreatureSound(sound)) return;
        Vec3d source = new Vec3d(sound.getX(), sound.getY(), sound.getZ());
        double distance = client.player.getEyePos().distanceTo(source);
        if (!Double.isFinite(distance) || distance < 1.0 || distance > Math.min(32.0, Math.max(8.0, range))) return;
        float strength = (float) Math.clamp(1.0 - distance / 32.0, 0.05, 1.0);
        var hit = client.world.raycast(new RaycastContext(client.player.getEyePos(), source,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, client.player));
        if (hit.getType() != HitResult.Type.MISS && hit.getPos().distanceTo(client.player.getEyePos()) + 0.5 < distance) strength *= 0.35F;
        int hash = sound.getId().hashCode();
        double jitterX = ((hash & 15) - 7.5) * 0.006;
        double jitterZ = (((hash >>> 4) & 15) - 7.5) * 0.006;
        ClientBlindnessState.addCreaturePulse(source.x + jitterX, source.y, source.z + jitterZ,
                strength * (float) BlindnessClient.CONFIG.soundPromptVolume());
    }

    private static boolean isCreatureSound(SoundInstance sound) {
        SoundCategory category = sound.getCategory();
        if (category != SoundCategory.HOSTILE && category != SoundCategory.NEUTRAL && category != SoundCategory.PLAYERS) return false;
        String path = sound.getId().getPath();
        return path.contains("ambient") || path.contains("step") || path.contains("hurt")
                || path.contains("attack") || path.contains("death") || path.contains("growl")
                || path.contains("hiss") || path.contains("roar");
    }
}
