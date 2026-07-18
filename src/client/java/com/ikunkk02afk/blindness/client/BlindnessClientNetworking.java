package com.ikunkk02afk.blindness.client;

import com.ikunkk02afk.blindness.client.animation.BlindnessAnimations;
import com.ikunkk02afk.blindness.network.BlindnessPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

public final class BlindnessClientNetworking {
    private BlindnessClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(BlindnessPayloads.FallStart.ID, (payload, context) -> context.client().execute(() -> {
            if (context.client().player != null && payload.entityId() == context.client().player.getId()
                    && (payload.direction() == 0 || payload.direction() == 1)
                    && payload.durationTicks() >= 1 && payload.durationTicks() <= 60) {
                ClientBlindnessState.startFall(payload.durationTicks(), payload.seed());
            }
        }));
        ClientPlayNetworking.registerGlobalReceiver(BlindnessPayloads.FallEnd.ID, (payload, context) -> context.client().execute(() -> {
            if (context.client().player != null && payload.entityId() == context.client().player.getId()
                    && payload.getUpTicks() >= 1 && payload.getUpTicks() <= 20) {
                ClientBlindnessState.startGetUp(payload.getUpTicks());
            }
        }));
        ClientPlayNetworking.registerGlobalReceiver(BlindnessPayloads.ScanWave.ID, (payload, context) -> context.client().execute(() -> {
            if (context.client().player == null || payload.scanId() < 0 || payload.mode() < 0 || payload.mode() > 1
                    || !Float.isFinite(payload.maxRadius()) || payload.maxRadius() < 0.5F || payload.maxRadius() > 8F
                    || payload.durationTicks() < 1 || payload.durationTicks() > 60
                    || payload.origin().getSquaredDistance(context.client().player.getPos()) > 144.0) return;
            ClientBlindnessState.addWave(payload.scanId(), payload.origin(), payload.maxRadius(), payload.durationTicks(), payload.mode());
            float volume = (float) BlindnessClient.CONFIG.soundPromptVolume() * 0.18F;
            context.client().getSoundManager().play(PositionedSoundInstance.master(
                    SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), payload.mode() == 0 ? 1.25F : 0.85F, volume));
        }));
        ClientPlayNetworking.registerGlobalReceiver(BlindnessPayloads.ScanResult.ID, (payload, context) -> context.client().execute(() -> {
            if (context.client().player == null || payload.scanId() < 0 || payload.mode() < 0 || payload.mode() > 1
                    || payload.hits().isEmpty() || payload.hits().size() > BlindnessPayloads.ScanResult.MAX_HITS
                    || payload.origin().getSquaredDistance(context.client().player.getPos()) > 144.0) return;
            for (BlindnessPayloads.ScanHit hit : payload.hits()) {
                if (!hit.isValid(8) || hit.resolve(payload.origin()).getSquaredDistance(payload.origin()) > 100.0) return;
            }
            ClientBlindnessState.acceptScanResult(payload.scanId(), payload.origin(), payload.hits());
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
                        Math.clamp(payload.tapRange(), 2, 6), Math.clamp(payload.sweepRange(), 3, 8),
                        Math.clamp(payload.tapCooldown(), 5, 40), Math.clamp(payload.sweepCooldown(), 10, 80),
                        Math.clamp(payload.pathTtl(), 20, 120), Math.clamp(payload.maxHits(), 16, 96)))));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientBlindnessState.clear());
    }
}
