package com.ikunkk02afk.blindness.mixin;

import com.ikunkk02afk.blindness.fall.FallStateManager;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow public ServerPlayerEntity player;

    @Inject(method = "onPlayerMove", at = @At("HEAD"), cancellable = true)
    private void blindness$blockMovementWhileFallen(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        if (!packet.changesPosition() || !FallStateManager.isControlLocked(player)) return;
        ServerPlayNetworkHandler handler = (ServerPlayNetworkHandler) (Object) this;
        float yaw = packet.changesLook() ? packet.getYaw(player.getYaw()) : player.getYaw();
        float pitch = packet.changesLook() ? packet.getPitch(player.getPitch()) : player.getPitch();
        handler.requestTeleport(player.getX(), player.getY(), player.getZ(), yaw, pitch);
        ci.cancel();
    }
}
