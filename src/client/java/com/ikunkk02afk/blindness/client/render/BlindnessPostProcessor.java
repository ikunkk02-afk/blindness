package com.ikunkk02afk.blindness.client.render;

import com.ikunkk02afk.blindness.BlindnessMod;
import com.ikunkk02afk.blindness.client.BlindnessClient;
import com.ikunkk02afk.blindness.client.ClientBlindnessState;
import com.ikunkk02afk.blindness.component.BlindnessComponents;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.compat.IrisCompat;
import foundry.veil.platform.VeilEventPlatform;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.util.Identifier;

public final class BlindnessPostProcessor {
    private static final Identifier FULL_PIPELINE = BlindnessMod.id("blindness");
    private static final Identifier DEPTH_PIPELINE = BlindnessMod.id("blindness_depth");
    private static Identifier activePipeline;
    private static boolean loggedFailure;
    private static boolean ready;

    private BlindnessPostProcessor() {}

    public static void register() {
        VeilEventPlatform.INSTANCE.onFreeNativeResources(BlindnessPostProcessor::cleanup);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> cleanup());
    }

    public static void tick(MinecraftClient client) {
        boolean pauseAllowsBlack = !(client.currentScreen instanceof GameMenuScreen)
                || BlindnessClient.CONFIG.blackScreenBehindMenus();
        boolean enabled = client.player != null && client.world != null && pauseAllowsBlack
                && BlindnessClient.CONFIG.enableVisualPostProcessing()
                && BlindnessComponents.PLAYER.maybeGet(client.player)
                .map(component -> component.blindnessEnabled()).orElse(true);
        if (!enabled) {
            deactivate();
            return;
        }

        Identifier desired = IrisCompat.isLoaded() && IrisCompat.INSTANCE.areShadersLoaded()
                ? DEPTH_PIPELINE : FULL_PIPELINE;
        var manager = VeilRenderSystem.renderer().getPostProcessingManager();
        if (!desired.equals(activePipeline)) {
            deactivate();
            manager.add(2000, desired);
            activePipeline = desired;
        }
        PostPipeline pipeline = manager.getPipeline(desired);
        if (pipeline == null) {
            if (!loggedFailure) {
                loggedFailure = true;
                BlindnessMod.LOGGER.warn("Veil blindness pipeline is not available yet; keeping the effect disabled until resources load");
            }
            return;
        }
        loggedFailure = false;
        ready = true;
        pipeline.getUniformSafe("ModelMaskReady").setInt(ContactOutlineRenderer.maskFramebuffer() != null ? 1 : 0);
        pipeline.getUniformSafe("CenterOutlineThickness").setFloat((float) BlindnessClient.CONFIG.centerOutlineThickness());
        pipeline.getUniformSafe("AdjacentOutlineThickness").setFloat((float) BlindnessClient.CONFIG.adjacentOutlineThickness());
        pipeline.getUniformSafe("CenterOutlineBrightness").setFloat((float) BlindnessClient.CONFIG.centerOutlineBrightness());
        pipeline.getUniformSafe("AdjacentOutlineBrightness").setFloat((float) BlindnessClient.CONFIG.adjacentOutlineBrightness());
        pipeline.getUniformSafe("CenterGlowRadius").setFloat((float) BlindnessClient.CONFIG.centerGlowRadius());
        pipeline.getUniformSafe("AdjacentGlowRadius").setFloat((float) BlindnessClient.CONFIG.adjacentGlowRadius());
        pipeline.getUniformSafe("CenterGlowStrength").setFloat((float) BlindnessClient.CONFIG.centerGlowStrength());
        pipeline.getUniformSafe("AdjacentGlowStrength").setFloat((float) BlindnessClient.CONFIG.adjacentGlowStrength());
        ClientBlindnessState.CreaturePulse pulse = ClientBlindnessState.activeCreaturePulse();
        if (pulse == null) {
            pipeline.getUniformSafe("CreatureStrength").setFloat(0F);
        } else {
            var camera = client.gameRenderer.getCamera().getPos();
            pipeline.getUniformSafe("CreatureOrigin").setVector(
                    (float) (pulse.x() - camera.x), (float) (pulse.y() - camera.y), (float) (pulse.z() - camera.z));
            pipeline.getUniformSafe("CreatureStrength").setFloat(pulse.strength());
        }
    }

    public static void cleanup() {
        deactivate();
        ClientBlindnessState.clear();
    }

    public static boolean isReady() { return ready; }

    private static void deactivate() {
        ready = false;
        if (activePipeline == null) return;
        try {
            VeilRenderSystem.renderer().getPostProcessingManager().remove(activePipeline);
        } catch (Throwable throwable) {
            if (!loggedFailure) BlindnessMod.LOGGER.warn("Failed to release Veil blindness pipeline cleanly", throwable);
        }
        activePipeline = null;
    }
}
