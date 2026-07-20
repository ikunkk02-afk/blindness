package com.ikunkk02afk.blindness.mixin;

import com.ikunkk02afk.blindness.accessibility.EyeOfEnderEntityAccessor;
import com.ikunkk02afk.blindness.BlindnessMod;
import com.ikunkk02afk.blindness.network.BlindnessPayloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EyeOfEnderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EnderEyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * Captures the player who threw an Ender Eye and stores the thrower UUID
 * on the EyeOfEnderEntity via the accessor interface.
 * Also sends a StartEnderEyeTracking packet to the thrower's client
 * to begin the visual tracking marker.
 */
@Mixin(EnderEyeItem.class)
public abstract class EnderEyeItemMixin {

    @Inject(method = "use",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void blindness$captureThrower(World world, PlayerEntity user, Hand hand,
                                          CallbackInfoReturnable<TypedActionResult<ItemStack>> cir,
                                          ItemStack stack, BlockHitResult hit,
                                          ServerWorld serverWorld, net.minecraft.util.math.BlockPos pos,
                                          EyeOfEnderEntity eye) {
        if (!world.isClient && eye != null && user != null) {
            ((EyeOfEnderEntityAccessor) eye).blindness$setThrowerUuid(user.getUuid());

            // Send tracking start packet if the feature is enabled.
            if (BlindnessMod.serverConfig().enableEnderEyeResultHint()) {
                var uuid = user.getUuid();
                ServerPlayNetworking.send(
                        (ServerPlayerEntity) user,
                        new BlindnessPayloads.StartEnderEyeTracking(
                                eye.getId(),
                                uuid.getMostSignificantBits(),
                                uuid.getLeastSignificantBits()
                        )
                );
            }
        }
    }
}
