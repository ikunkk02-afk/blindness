package com.ikunkk02afk.blindness.client;

import com.ikunkk02afk.blindness.client.animation.BlindnessAnimations;
import com.ikunkk02afk.blindness.client.contact.ContactRevealManager;
import com.ikunkk02afk.blindness.network.BlindnessPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvents;
import com.ikunkk02afk.blindness.awareness.CliffRisk;
import com.ikunkk02afk.blindness.awareness.RevealSource;

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
        ClientPlayNetworking.registerGlobalReceiver(BlindnessPayloads.EntitySoundReveal.ID, (payload, context) -> context.client().execute(() -> {
            if (context.client().player == null || payload.entries().isEmpty() || payload.entries().size() > 12
                    || payload.center().getSquaredDistance(context.client().player.getPos()) > 196.0) return;
            RevealSource source = RevealSource.fromNetwork(payload.source());
            if (source == null || source == RevealSource.CANE_CENTER || source == RevealSource.CANE_ADJACENT) return;
            for (BlindnessPayloads.SoundRevealEntry entry : payload.entries()) {
                if (!entry.isValid() || entry.resolve(payload.center()).getSquaredDistance(context.client().player.getPos()) > 225.0) return;
            }
            ContactRevealManager.acceptSound(payload.center(), source, payload.entries());
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
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientBlindnessState.clear());
    }
}
