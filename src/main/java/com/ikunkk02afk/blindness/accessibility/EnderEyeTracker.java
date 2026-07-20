package com.ikunkk02afk.blindness.accessibility;

import net.minecraft.entity.EyeOfEnderEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import com.ikunkk02afk.blindness.BlindnessMod;
import com.ikunkk02afk.blindness.network.BlindnessNetworking;
import com.ikunkk02afk.blindness.network.BlindnessPayloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.UUID;

/**
 * Server-side tracker for Ender Eye results.
 * Handles detecting whether the eye shattered or dropped as an item,
 * and notifies the original thrower with directional hints.
 */
public final class EnderEyeTracker {
    private EnderEyeTracker() {}

    /**
     * Called from the EyeOfEnderEntity mixin when the eye is discarded.
     *
     * @param world         the server world
     * @param eye           the eye entity (about to be removed)
     * @param throwerUuid   the UUID stored on the entity (may be null)
     * @param droppedAsItem true if the eye became an item, false if it shattered
     */
    public static void onResult(ServerWorld world, EyeOfEnderEntity eye, UUID throwerUuid, boolean droppedAsItem) {
        if (throwerUuid == null) return;
        if (!BlindnessMod.serverConfig().enableEnderEyeResultHint()) return;

        ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(throwerUuid);
        if (player == null) return; // offline

        Vec3d pos = eye.getPos();

        if (droppedAsItem) {
            // Eye dropped as an item.
            sendResult(player, 0, pos); // 0 = dropped
            world.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(),
                    SoundCategory.PLAYERS, 0.5F, 1.1F);
        } else {
            // Eye shattered.
            sendResult(player, 1, pos); // 1 = shattered
            world.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.BLOCK_GLASS_BREAK,
                    SoundCategory.PLAYERS, 0.7F, 0.9F);
        }
    }

    private static void sendResult(ServerPlayerEntity player, int resultType, Vec3d pos) {
        ServerPlayNetworking.send(player, new BlindnessPayloads.EnderEyeResult(
                resultType, pos.x, pos.y, pos.z));
    }
}
