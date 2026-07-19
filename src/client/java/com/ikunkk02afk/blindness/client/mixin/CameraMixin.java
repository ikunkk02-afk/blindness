package com.ikunkk02afk.blindness.client.mixin;

import com.ikunkk02afk.blindness.client.BlindnessClient;
import com.ikunkk02afk.blindness.client.ClientBlindnessState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow public abstract float getYaw();
    @Shadow public abstract float getPitch();

    @Inject(method = "update", at = @At("TAIL"))
    private void blindness$applyBoundedFallCamera(BlockView area, Entity focusedEntity, boolean thirdPerson,
                                                   boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (thirdPerson || MinecraftClient.getInstance().player == null) return;
        float cliff = ClientBlindnessState.cliffFeedback();
        if (!ClientBlindnessState.controlsLocked()) {
            if (cliff > 0F) {
                float wobble = (float) Math.sin(System.nanoTime() / 35_000_000.0) * 0.75F * cliff;
                setRotation(getYaw() + wobble, Math.clamp(getPitch() + Math.abs(wobble) * 0.35F, -89.5F, 89.5F));
            }
            return;
        }
        float progress = ClientBlindnessState.fallProgress();
        float strength = (float) BlindnessClient.CONFIG.cameraShakeStrength();
        float tilt = BlindnessClient.CONFIG.disableFirstPersonFallTilt() ? 0F : Math.min(38F, 38F * progress);
        float shake = (float) (Math.sin(progress * 22.0 + ClientBlindnessState.fallSeed() * 0.01) * 2.5 * strength * (1.0 - progress));
        setRotation(getYaw() + shake, Math.clamp(getPitch() + tilt, -89.5F, 89.5F));
    }
}
