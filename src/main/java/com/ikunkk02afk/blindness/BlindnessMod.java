package com.ikunkk02afk.blindness;

import com.ikunkk02afk.blindness.command.BlindnessCommands;
import com.ikunkk02afk.blindness.config.BlindnessServerConfig;
import com.ikunkk02afk.blindness.fall.FallStateManager;
import com.ikunkk02afk.blindness.item.ModItems;
import com.ikunkk02afk.blindness.network.BlindnessNetworking;
import com.ikunkk02afk.blindness.contact.CaneContactService;
import com.ikunkk02afk.blindness.awareness.HostileAwarenessService;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BlindnessMod implements ModInitializer {
    public static final String MOD_ID = "blindness";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static BlindnessServerConfig serverConfig;

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        serverConfig();
        ModItems.register();
        BlindnessNetworking.registerServer();
        BlindnessCommands.register();
        CaneContactService.register();
        HostileAwarenessService.register();
        FallStateManager.register();
        LOGGER.info("Blindness 0.1.0 initialized");
    }

    public static BlindnessServerConfig serverConfig() {
        if (serverConfig == null) serverConfig = BlindnessServerConfig.createAndLoad();
        return serverConfig;
    }
}
