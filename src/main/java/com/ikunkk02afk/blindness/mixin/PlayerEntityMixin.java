package com.ikunkk02afk.blindness.mixin;

import com.ikunkk02afk.blindness.fall.FallStateManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void blindness$blockJumpWhileFallen(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player && FallStateManager.isControlLocked(player)) ci.cancel();
    }
}
