package com.ikunkk02afk.blindness.client.mixin;

import com.ikunkk02afk.blindness.client.compat.BlockedInformationMods;
import com.ikunkk02afk.blindness.client.compat.BlockedInformationModsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.CookieStorage;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin {
    @Inject(method = "connect(Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/network/ServerAddress;Lnet/minecraft/client/network/ServerInfo;ZLnet/minecraft/client/network/CookieStorage;)V",
            at = @At("HEAD"), cancellable = true)
    private static void blindness$blockMultiplayerConnection(Screen parent, MinecraftClient client,
            ServerAddress address, ServerInfo info, boolean quickPlay, CookieStorage cookies, CallbackInfo ci) {
        if (BlockedInformationMods.shouldBlockWorldEntry()) {
            client.setScreen(new BlockedInformationModsScreen());
            ci.cancel();
        }
    }
}
