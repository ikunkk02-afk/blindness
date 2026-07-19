package com.ikunkk02afk.blindness.network;

import com.ikunkk02afk.blindness.BlindnessMod;
import com.ikunkk02afk.blindness.component.BlindnessComponents;
import com.ikunkk02afk.blindness.config.BlindnessServerConfig;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class BlindnessNetworking {
    private BlindnessNetworking() {}

    public static void registerServer() {
        PayloadTypeRegistry.playS2C().register(BlindnessPayloads.FallStart.ID, BlindnessPayloads.FallStart.CODEC);
        PayloadTypeRegistry.playS2C().register(BlindnessPayloads.FallEnd.ID, BlindnessPayloads.FallEnd.CODEC);
        PayloadTypeRegistry.playS2C().register(BlindnessPayloads.ContactReveal.ID, BlindnessPayloads.ContactReveal.CODEC);
        PayloadTypeRegistry.playS2C().register(BlindnessPayloads.ClearContactReveals.ID, BlindnessPayloads.ClearContactReveals.CODEC);
        PayloadTypeRegistry.playS2C().register(BlindnessPayloads.CliffWarning.ID, BlindnessPayloads.CliffWarning.CODEC);
        PayloadTypeRegistry.playS2C().register(BlindnessPayloads.EntitySoundEcho.ID, BlindnessPayloads.EntitySoundEcho.CODEC);
        PayloadTypeRegistry.playS2C().register(BlindnessPayloads.SoundAwarenessSettings.ID, BlindnessPayloads.SoundAwarenessSettings.CODEC);
        PayloadTypeRegistry.playS2C().register(BlindnessPayloads.HostileWarning.ID, BlindnessPayloads.HostileWarning.CODEC);
        PayloadTypeRegistry.playS2C().register(BlindnessPayloads.Animation.ID, BlindnessPayloads.Animation.CODEC);
        PayloadTypeRegistry.playS2C().register(BlindnessPayloads.TutorialPrompt.ID, BlindnessPayloads.TutorialPrompt.CODEC);
        PayloadTypeRegistry.playS2C().register(BlindnessPayloads.ServerConfig.ID, BlindnessPayloads.ServerConfig.CODEC);
        PayloadTypeRegistry.playC2S().register(BlindnessPayloads.TutorialAck.ID, BlindnessPayloads.TutorialAck.CODEC);
        PayloadTypeRegistry.playC2S().register(BlindnessPayloads.ConfigRequest.ID, BlindnessPayloads.ConfigRequest.CODEC);
        PayloadTypeRegistry.playC2S().register(BlindnessPayloads.UpdateSoundAwarenessSettings.ID,
                BlindnessPayloads.UpdateSoundAwarenessSettings.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(BlindnessPayloads.TutorialAck.ID, (payload, context) ->
                context.server().execute(() -> BlindnessComponents.PLAYER.get(context.player()).setTutorialCompleted(true)));
        ServerPlayNetworking.registerGlobalReceiver(BlindnessPayloads.ConfigRequest.ID, (payload, context) ->
                context.server().execute(() -> sendServerConfig(context.player())));
        ServerPlayNetworking.registerGlobalReceiver(BlindnessPayloads.UpdateSoundAwarenessSettings.ID, (payload, context) ->
                context.server().execute(() -> {
                    int listening = Math.clamp(payload.listeningChunkRadius(), 0,
                            BlindnessMod.serverConfig().maximumListeningChunkRadius());
                    int blocks = Math.clamp(payload.blockRevealRadius(), 0,
                            BlindnessMod.serverConfig().maximumEntitySoundBlockRevealRadius());
                    BlindnessComponents.PLAYER.get(context.player()).setSoundAwarenessSettings(listening, blocks);
                    sendSoundAwarenessSettings(context.player());
                }));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            BlindnessComponents.PLAYER.sync(player);
            sendServerConfig(player);
            sendSoundAwarenessSettings(player);
            if (!BlindnessComponents.PLAYER.get(player).tutorialCompleted()) {
                ServerPlayNetworking.send(player, BlindnessPayloads.TutorialPrompt.INSTANCE);
            }
        });
    }

    public static void sendServerConfig(ServerPlayerEntity player) {
        BlindnessServerConfig config = BlindnessMod.serverConfig();
        ServerPlayNetworking.send(player, new BlindnessPayloads.ServerConfig(
                config.tapCooldownTicks(), config.sweepCooldownTicks(), config.contactPathTtlTicks(),
                BlindnessPayloads.ContactReveal.MAX_ENTRIES));
    }

    public static void sendSoundAwarenessSettings(ServerPlayerEntity player) {
        var component = BlindnessComponents.PLAYER.get(player);
        ServerPlayNetworking.send(player, new BlindnessPayloads.SoundAwarenessSettings(
                component.listeningChunkRadius(), component.entitySoundBlockRevealRadius()));
    }

    public static void sendToTrackingAndSelf(ServerPlayerEntity player, net.minecraft.network.packet.CustomPayload payload) {
        ServerPlayNetworking.send(player, payload);
        for (ServerPlayerEntity watcher : PlayerLookup.tracking(player)) {
            if (watcher != player) ServerPlayNetworking.send(watcher, payload);
        }
    }
}
