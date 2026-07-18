package com.ikunkk02afk.blindness.client;

import com.ikunkk02afk.blindness.BlindnessMod;
import com.ikunkk02afk.blindness.client.animation.BlindnessAnimations;
import com.ikunkk02afk.blindness.client.render.BlindnessPostProcessor;
import com.ikunkk02afk.blindness.client.sound.CreatureSoundLocator;
import com.ikunkk02afk.blindness.config.BlindnessClientConfig;
import io.wispforest.owo.config.ui.ConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class BlindnessClient implements ClientModInitializer {
    public static final BlindnessClientConfig CONFIG = BlindnessClientConfig.createAndLoad();
    private static KeyBinding settingsKey;

    @Override
    public void onInitializeClient() {
        settingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.blindness.open_settings", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "category.blindness"));
        BlindnessClientNetworking.register();
        BlindnessAnimations.register();
        BlindnessPostProcessor.register();
        ClientLifecycleEvents.CLIENT_STARTED.register(CreatureSoundLocator::register);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (settingsKey.wasPressed()) client.setScreen(ConfigScreen.create(CONFIG, client.currentScreen));
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
}
