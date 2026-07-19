package com.ikunkk02afk.blindness.client.render;

import com.ikunkk02afk.blindness.BlindnessMod;
import com.ikunkk02afk.blindness.client.BlindnessClient;
import com.ikunkk02afk.blindness.client.ClientBlindnessState;
import com.ikunkk02afk.blindness.component.BlindnessComponents;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.platform.VeilEventPlatform;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

public final class BlindnessPostProcessor {
    // DashLoader is intentionally marked as incompatible in fabric.mod.json.
    // Its shader cache restoration can run before Veil finishes initializing
    // shader uniforms and post-processing resources.
    private static final Identifier FULL_PIPELINE = BlindnessMod.id("blindness");
    private static final Identifier DEPTH_PIPELINE = BlindnessMod.id("blindness_depth");
    private static Identifier activePipeline;
    private static boolean loggedPipelineFailure;
    private static boolean loggedFboError;
    private static boolean ready;
    private static boolean fboDegraded;

    private BlindnessPostProcessor() {}

    public static void register() {
        VeilEventPlatform.INSTANCE.onFreeNativeResources(BlindnessPostProcessor::cleanup);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> cleanup());
    }

    public static void tick(MinecraftClient client) {
        if (!BlindnessClient.CONFIG.enableVisualPostProcessing()) {
            deactivate();
            BlindnessBlackoutRenderer.setVeilPipelineActive(false);
            return;
        }

        boolean enabled = client.player != null && client.world != null
                && BlindnessComponents.PLAYER.maybeGet(client.player)
                .map(component -> component.blindnessEnabled()).orElse(false);
        if (!enabled) {
            deactivate();
            BlindnessBlackoutRenderer.setVeilPipelineActive(false);
            return;
        }

        int width = client.getWindow().getFramebufferWidth();
        int height = client.getWindow().getFramebufferHeight();
        if (width <= 0 || height <= 0) return;

        if (fboDegraded) {
            BlindnessBlackoutRenderer.setVeilPipelineActive(false);
            return;
        }

        // Always use the depth-only pipeline. The full pipeline previously requested
        // dynamicBuffers: ["normal"] which triggers Veil's DynamicBufferManager to create
        // AdvancedFbo instances. Those fail with GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT
        // under Sodium, and the dynamic normal texture is not needed for blindness
        // blackout or contact-outline compositing. The depth pipeline uses only the
        // scene depth buffer (minecraft:main:depth) and the contact mask FBO, both of
        // which are available regardless of Iris or Sodium.
        Identifier desired = DEPTH_PIPELINE;
        var manager = VeilRenderSystem.renderer().getPostProcessingManager();

        if (!desired.equals(activePipeline)) {
            deactivate();
            try {
                manager.add(2000, desired);
                activePipeline = desired;
                loggedFboError = false;
            } catch (Exception e) {
                activePipeline = null;
                fboDegraded = true;
                if (!loggedFboError) {
                    loggedFboError = true;
                    BlindnessMod.LOGGER.warn(
                            "Veil blindness pipeline failed to activate ({}). "
                                    + "Falling back to basic black screen. "
                                    + "Sodium or Iris may be incompatible with Veil dynamic buffers. "
                                    + "Error: {}",
                            desired, e.toString());
                }
                BlindnessBlackoutRenderer.setVeilPipelineActive(false);
                return;
            }
        }

        PostPipeline pipeline = manager.getPipeline(desired);
        if (pipeline == null) {
            if (!loggedPipelineFailure) {
                loggedPipelineFailure = true;
                BlindnessMod.LOGGER.debug(
                        "Veil blindness pipeline not ready yet; waiting for resource load");
            }
            BlindnessBlackoutRenderer.setVeilPipelineActive(false);
            return;
        }

        loggedPipelineFailure = false;
        ready = true;
        BlindnessBlackoutRenderer.setVeilPipelineActive(true);

        pipeline.getUniformSafe("ModelMaskReady")
                .setInt(ContactOutlineRenderer.isMaskFboAvailable() ? 1 : 0);
        pipeline.getUniformSafe("CenterOutlineThickness")
                .setFloat((float) BlindnessClient.CONFIG.centerOutlineThickness());
        pipeline.getUniformSafe("AdjacentOutlineThickness")
                .setFloat((float) BlindnessClient.CONFIG.adjacentOutlineThickness());
        pipeline.getUniformSafe("CenterOutlineBrightness")
                .setFloat((float) BlindnessClient.CONFIG.centerOutlineBrightness());
        pipeline.getUniformSafe("AdjacentOutlineBrightness")
                .setFloat((float) BlindnessClient.CONFIG.adjacentOutlineBrightness());
        pipeline.getUniformSafe("CenterGlowRadius")
                .setFloat((float) BlindnessClient.CONFIG.centerGlowRadius());
        pipeline.getUniformSafe("AdjacentGlowRadius")
                .setFloat((float) BlindnessClient.CONFIG.adjacentGlowRadius());
        pipeline.getUniformSafe("CenterGlowStrength")
                .setFloat((float) BlindnessClient.CONFIG.centerGlowStrength());
        pipeline.getUniformSafe("AdjacentGlowStrength")
                .setFloat((float) BlindnessClient.CONFIG.adjacentGlowStrength());
    }

    public static void cleanup() {
        deactivate();
        fboDegraded = false;
        loggedFboError = false;
        loggedPipelineFailure = false;
        BlindnessBlackoutRenderer.setVeilPipelineActive(false);
        BlindnessBlackoutRenderer.resetDiagnostics();
        ClientBlindnessState.clear();
    }

    public static boolean isReady() { return ready; }

    private static void deactivate() {
        ready = false;
        if (activePipeline == null) return;
        try {
            VeilRenderSystem.renderer().getPostProcessingManager().remove(activePipeline);
        } catch (Throwable t) {
            // Pipeline may already be removed by renderer shutdown
        }
        activePipeline = null;
    }
}
