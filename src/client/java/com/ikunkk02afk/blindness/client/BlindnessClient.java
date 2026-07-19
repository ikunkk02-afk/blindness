package com.ikunkk02afk.blindness.client;

import com.ikunkk02afk.blindness.BlindnessMod;
import com.ikunkk02afk.blindness.client.animation.BlindnessAnimations;
import com.ikunkk02afk.blindness.client.render.BlindnessPostProcessor;
import com.ikunkk02afk.blindness.client.render.ContactOutlineRenderer;
import com.ikunkk02afk.blindness.config.BlindnessClientConfig;
import io.wispforest.owo.config.ui.ConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.screen.Screen;
import com.ikunkk02afk.blindness.client.compat.BlockedInformationMods;
import com.ikunkk02afk.blindness.client.compat.BlockedInformationModsScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class BlindnessClient implements ClientModInitializer {
    public static final BlindnessClientConfig CONFIG = BlindnessClientConfig.createAndLoad();
    private static KeyBinding settingsKey;

    @Override
    public void onInitializeClient() {
        BlockedInformationMods.initializeSnapshot();
        settingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.blindness.open_settings", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "category.blindness"));
        BlindnessClientNetworking.register();
        BlindnessAnimations.register();
        BlindnessPostProcessor.register();
        ContactOutlineRenderer.register();
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (BlockedInformationMods.shouldBlockWorldEntry()) {
                client.execute(() -> client.disconnect(new BlockedInformationModsScreen()));
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (settingsKey.wasPressed()) client.setScreen(createConfigScreen(client.currentScreen));
            ClientBlindnessState.tick(client);
            BlindnessPostProcessor.tick(client);
            if (ClientBlindnessState.controlsLocked()) {
                client.options.attackKey.setPressed(false);
                client.options.useKey.setPressed(false);
                client.options.jumpKey.setPressed(false);
            }
        });
        BlindnessMod.LOGGER.info("Blindness client initialized");
    }

    public static Screen createConfigScreen(Screen parent) {
        return ConfigScreen.create(CONFIG, parent);
    }
}
