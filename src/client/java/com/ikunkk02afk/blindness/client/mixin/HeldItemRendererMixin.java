package com.ikunkk02afk.blindness.client.mixin;

import com.ikunkk02afk.blindness.client.BlindnessClient;
import com.ikunkk02afk.blindness.item.ModItems;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void blindness$honorHeldCaneVisibility(AbstractClientPlayerEntity player, float tickDelta, float pitch,
                                                    Hand hand, float swingProgress, ItemStack item, float equipProgress,
                                                    MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                                    int light, CallbackInfo ci) {
        if (item.isOf(ModItems.GUIDANCE_CANE) && !BlindnessClient.CONFIG.keepHeldCaneVisible()) ci.cancel();
    }
}
