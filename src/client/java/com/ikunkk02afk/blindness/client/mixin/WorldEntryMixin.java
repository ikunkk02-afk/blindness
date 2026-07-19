package com.ikunkk02afk.blindness.client.mixin;

import com.ikunkk02afk.blindness.client.compat.BlockedInformationMods;
import com.ikunkk02afk.blindness.client.compat.BlockedInformationModsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.WorldListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldListWidget.WorldEntry.class)
public abstract class WorldEntryMixin {
    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void blindness$blockExistingWorld(CallbackInfo ci) {
        if (BlockedInformationMods.shouldBlockWorldEntry()) {
            MinecraftClient.getInstance().setScreen(new BlockedInformationModsScreen());
            ci.cancel();
        }
    }
}
