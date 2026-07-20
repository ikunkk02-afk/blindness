package com.ikunkk02afk.blindness.client;

import com.ikunkk02afk.blindness.client.animation.BlindnessAnimations;
import com.ikunkk02afk.blindness.client.contact.ContactRevealManager;
import com.ikunkk02afk.blindness.client.sound.SoundEchoMarkerManager;
import com.ikunkk02afk.blindness.client.ore.OreRevealManager;
import com.ikunkk02afk.blindness.client.ore.OreHudRenderer;
import com.ikunkk02afk.blindness.client.ender.EnderEyeResultHandler;
import com.ikunkk02afk.blindness.client.ender.EnderEyeTrackerClient;
import com.ikunkk02afk.blindness.network.BlindnessPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvents;
import com.ikunkk02afk.blindness.awareness.CliffRisk;
import com.ikunkk02afk.blindness.awareness.EntitySoundCategory;
import com.ikunkk02afk.blindness.awareness.RevealSource;
import com.ikunkk02afk.blindness.awareness.SoundOcclusion;
import com.ikunkk02afk.blindness.BlindnessMod;
import com.ikunkk02afk.blindness.awareness.SoundAwarenessRules;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

public final class BlindnessClientNetworking {
    private static int actualListeningRadius = -1;
    private static int actualBlockRadius = -1;
    private static int requestedListeningRadius = -1;
    private static int requestedBlockRadius = -1;

    private BlindnessClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(BlindnessPayloads.FallStart.ID, (payload, context) -> context.client().execute(() -> {
            if (context.client().player != null && payload.entityId() == context.client().player.getId()
                    && (payload.direction() == 0 || payload.direction() == 1)
                    && payload.durationTicks() >= 1 && payload.durationTicks() <= 100) {
                ClientBlindnessState.startFall(payload.durationTicks(), payload.seed());
            }
        }));
        ClientPlayNetworking.registerGlobalReceiver(BlindnessPayloads.FallEnd.ID, (payload, context) -> context.client().execute(() -> {
            if (context.client().player != null && payload.entityId() == context.client().player.getId()
                    && payload.getUpTicks() >= 1 && payload.getUpTicks() <= 20) {
                ClientBlindnessState.startGetUp(payload.getUpTicks());
            }
        }));
        ClientPlayNetworking.registerGlobalReceiver(BlindnessPayloads.ContactReveal.ID, (payload, context) -> context.client().execute(() -> {
            if (context.client().player == null || payload.sequence() < 0 || payload.entries().isEmpty()
                    || payload.entries().size() > BlindnessPayloads.ContactReveal.MAX_ENTRIES
                    || payload.center().getSquaredDistance(context.client().player.getPos()) > 36.0) return;
            int centers = 0;
            for (BlindnessPayloads.ContactEntry entry : payload.entries()) {
                if (!entry.isValid()
                        || entry.resolve(payload.center()).getSquaredDistance(context.client().player.getPos()) > 49.0) return;
                if (entry.isCenter()) centers++;
            }
            if (centers == 1) ContactRevealManager.accept(payload.center(), payload.entries());
        }));
        ClientPlayNetworking.registerGlobalReceiver(BlindnessPayloads.ClearContactReveals.ID,
                (payload, context) -> context.client().execute(ContactRevealManager::clear));
        ClientPlayNetworking.registerGlobalReceiver(BlindnessPayloads.CliffWarning.ID, (payload, context) -> context.client().execute(() -> {
            if (context.client().player == null || payload.risk() <= 0 || payload.risk() >= CliffRisk.values().length) return;
            CliffRisk risk = CliffRisk.values()[payload.risk()];
            Text warning = Text.translatable(switch (risk) {
                case DROP -> "warning.blindness.cliff_drop";
                case LAVA -> "warning.blindness.cliff_lava";
                case SEVERE_DROP, VOID -> "warning.blindness.cliff_severe";
                default -> "warning.blindness.cliff_drop";
            });
            if (BlindnessClient.CONFIG.cliffWarningText()) context.client().player.sendMessage(warning, true);
            if (BlindnessClient.CONFIG.cliffWarningNarration() && context.client().getNarratorManager().isActive()) {
                context.client().getNarratorManager().narrate(warning);
            }
            ClientBlindnessState.startCliffWarning(risk.isSevere());
        }));
        ClientPlayNetworking.registerGlobalReceiver(BlindnessPayloads.EntitySoundEcho.ID, (payload, context) -> context.client().execute(() -> {
            if (context.client().player == null || context.client().world == null
                    || !payload.position().isValid() || !payload.metadata().isValid()
                    || payload.entries().size() > BlindnessPayloads.EntitySoundEcho.MAX_ENTRIES) return;
            for (BlindnessPayloads.SoundRevealEntry entry : payload.entries()) {
                if (!entry.isValid()) return;
            }
            EntitySoundCategory category = EntitySoundCategory.values()[payload.metadata().category()];
            SoundOcclusion occlusion = SoundOcclusion.values()[payload.metadata().occlusion()];
            Vec3d position = new Vec3d(payload.position().x(), payload.position().y(), payload.position().z());
            BlockPos echoBlock = BlockPos.ofFloored(position);
            ChunkPos echoChunk = new ChunkPos(echoBlock);
            ChunkPos playerChunk = context.client().player.getChunkPos();
            int listeningRadius = actualListeningRadius >= 0 ? actualListeningRadius
                    : Math.clamp(BlindnessClient.CONFIG.listeningChunkRadius(), 0, 2);
            long packetAge = context.client().world.getTime() - payload.metadata().serverGameTime();
            if (!context.client().world.isInBuildLimit(echoBlock)
                    || !context.client().world.getChunkManager().isChunkLoaded(echoChunk.x, echoChunk.z)
                    || !SoundAwarenessRules.isChunkInRange(playerChunk.x, playerChunk.z,
                    echoChunk.x, echoChunk.z, listeningRadius)
                    || packetAge < -20L || packetAge > 100L) return;
            SoundEchoMarkerManager.accept(position, category, payload.metadata().strength(),
                    payload.metadata().hostile(), occlusion, payload.metadata().serverGameTime());
            RevealSource source = payload.metadata().hostile() ? RevealSource.ENTITY_DANGER : category.revealSource();
            ContactRevealManager.acceptSound(payload.blockCenter(), source, payload.metadata().strength(), payload.entries());
            if (BlindnessClient.CONFIG.debugSoundEchoes()) {
                BlindnessMod.LOGGER.info("Sound echo accepted sourceEntityType=server-filtered category={} "
                                + "echoWorldPosition={} environmentOrigin={} sourceChunk={} playerChunk={}",
                        category, position, payload.blockCenter(), echoChunk, playerChunk);
            }
        }));
        ClientPlayNetworking.registerGlobalReceiver(BlindnessPayloads.HostileWarning.ID, (payload, context) -> context.client().execute(() -> {
            if (context.client().player == null || Byte.toUnsignedInt(payload.volumePercent()) > 100) return;
            if (BlindnessClient.CONFIG.hostileWarningText()) {
                context.client().player.sendMessage(Text.translatable("warning.blindness.hostile_nearby"), true);
            }
            float volume = Byte.toUnsignedInt(payload.volumePercent()) / 100F
                    * (float) BlindnessClient.CONFIG.soundPromptVolume();
            context.client().player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), volume,
                    payload.critical() ? 0.48F : 0.62F);
        }));
        ClientPlayNetworking.registerGlobalReceiver(BlindnessPayloads.Animation.ID, (payload, context) -> context.client().execute(() -> {
            if (payload.entityId() >= 0 && payload.animation() >= 0 && payload.animation() <= 4
                    && payload.durationTicks() >= 1 && payload.durationTicks() <= 60) {
                BlindnessAnimations.play(payload.entityId(), payload.animation());
            }
        }));
        ClientPlayNetworking.registerGlobalReceiver(BlindnessPayloads.TutorialPrompt.ID, (payload, context) -> context.client().execute(() -> {
            if (context.client().player != null && BlindnessClient.CONFIG.showTutorial()) {
                context.client().player.sendMessage(Text.translatable("tutorial.blindness.welcome"), false);
                context.client().player.sendMessage(Text.translatable("tutorial.blindness.cane"), false);
                context.client().player.sendMessage(Text.translatable("tutorial.blindness.exit"), false);
            }
            ClientPlayNetworking.send(BlindnessPayloads.TutorialAck.INSTANCE);
        }));
        ClientPlayNetworking.registerGlobalReceiver(BlindnessPayloads.ServerConfig.ID, (payload, context) -> context.client().execute(() ->
                ClientBlindnessState.setServerConfig(new ClientBlindnessState.ServerConfigSnapshot(
                        Math.clamp(payload.tapCooldown(), 5, 40), Math.clamp(payload.sweepCooldown(), 10, 80),
                        Math.clamp(payload.pathTtl(), 20, 80),
                        Math.clamp(payload.maxEntries(), 1, BlindnessPayloads.ContactReveal.MAX_ENTRIES)))));
        ClientPlayNetworking.registerGlobalReceiver(BlindnessPayloads.SoundAwarenessSettings.ID,
                (payload, context) -> context.client().execute(() -> {
                    actualListeningRadius = Math.clamp(payload.listeningChunkRadius(), 0, 2);
                    actualBlockRadius = Math.clamp(payload.blockRevealRadius(), 0, 4);
                    requestedListeningRadius = actualListeningRadius;
                    requestedBlockRadius = actualBlockRadius;
                    BlindnessClient.CONFIG.listeningChunkRadius(actualListeningRadius);
                    BlindnessClient.CONFIG.entitySoundBlockRevealRadius(actualBlockRadius);
                }));
        ClientPlayNetworking.registerGlobalReceiver(BlindnessPayloads.OreContactReveal.ID, (payload, context) -> context.client().execute(() -> {
            if (context.client().player == null || payload.sequence() < 0 || payload.contactEntries().isEmpty()
                    || payload.contactEntries().size() > BlindnessPayloads.ContactReveal.MAX_ENTRIES
                    || payload.ores().size() > BlindnessPayloads.OreContactReveal.MAX_ORE_ENTRIES
                    || payload.center().getSquaredDistance(context.client().player.getPos()) > 36.0) return;
            // Validate contact entries
            int centers = 0;
            for (BlindnessPayloads.ContactEntry entry : payload.contactEntries()) {
                if (!entry.isValid()
                        || entry.resolve(payload.center()).getSquaredDistance(context.client().player.getPos()) > 49.0) return;
                if (entry.isCenter()) centers++;
            }
            if (centers != 1) return;
            // Accept reveals
            ContactRevealManager.accept(payload.center(), payload.contactEntries());
            OreRevealManager.accept(payload.center(), payload.ores());
            OreHudRenderer.show(payload.center(), payload.ores());
        }));
        ClientPlayNetworking.registerGlobalReceiver(BlindnessPayloads.EnderEyeResult.ID, (payload, context) -> context.client().execute(() -> {
            if (context.client().player == null || !payload.isValid()) return;
            EnderEyeResultHandler.handle(payload.resultType(), payload.x(), payload.y(), payload.z());
            EnderEyeTrackerClient.onResult(payload.resultType(), payload.x(), payload.y(), payload.z());
        }));
        ClientPlayNetworking.registerGlobalReceiver(BlindnessPayloads.StartEnderEyeTracking.ID, (payload, context) -> context.client().execute(() -> {
            if (context.client().player == null || !payload.isValid()) return;
            EnderEyeTrackerClient.startTracking(payload.entityId(), payload.getEntityUuid());
        }));
        ClientPlayNetworking.registerGlobalReceiver(BlindnessPayloads.StarterCaneGranted.ID, (payload, context) -> context.client().execute(() -> {
            if (context.client().player != null) {
                context.client().player.sendMessage(Text.translatable("msg.blindness.starter_cane_granted"), true);
            }
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            actualListeningRadius = actualBlockRadius = -1;
            requestedListeningRadius = requestedBlockRadius = -1;
            SoundEchoMarkerManager.clear();
            ClientBlindnessState.clear();
            EnderEyeTrackerClient.clear();
        });
    }

    public static void tickSettingsSync() {
        if (actualListeningRadius < 0 || actualBlockRadius < 0) return;
        int listening = Math.clamp(BlindnessClient.CONFIG.listeningChunkRadius(), 0, 2);
        int blocks = Math.clamp(BlindnessClient.CONFIG.entitySoundBlockRevealRadius(), 0, 4);
        if ((listening != actualListeningRadius || blocks != actualBlockRadius)
                && (listening != requestedListeningRadius || blocks != requestedBlockRadius)) {
            requestedListeningRadius = listening;
            requestedBlockRadius = blocks;
            ClientPlayNetworking.send(new BlindnessPayloads.UpdateSoundAwarenessSettings(listening, blocks));
        }
    }
}
