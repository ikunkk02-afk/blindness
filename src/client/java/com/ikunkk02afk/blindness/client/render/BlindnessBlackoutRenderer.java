package com.ikunkk02afk.blindness.client.render;

import com.ikunkk02afk.blindness.BlindnessMod;
import com.ikunkk02afk.blindness.client.BlindnessClient;
import com.ikunkk02afk.blindness.component.BlindnessComponents;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Renders a full-screen opaque black overlay at the HUD stage as a guaranteed
 * fallback when Veil's post-processing pipeline cannot produce a black output.
 * <p>
 * The Veil pipeline may fail when:
 * <ul>
 *   <li>Sodium's DynamicBufferManager cannot create AdvancedFbo instances
 *       (GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT)</li>
 *   <li>Iris shader packs interfere with post-process stage ordering</li>
 *   <li>The pipeline JSON references dynamic buffers that aren't available</li>
 * </ul>
 * <p>
 * This renderer is independent of Veil, Sodium, and Iris. It runs after the
 * world is drawn but before GUI elements, using vanilla DrawContext. When the
 * Veil pipeline is confirmed working, this overlay is suppressed to avoid
 * double-rendering.
 */
public final class BlindnessBlackoutRenderer {
    private static boolean veilPipelineActive;
    private static boolean lastReportedState;
    private static boolean diagnosticsReported;

    private BlindnessBlackoutRenderer() {}

    public static void register() {
        HudRenderCallback.EVENT.register(BlindnessBlackoutRenderer::render);
    }

    public static void setVeilPipelineActive(boolean active) {
        veilPipelineActive = active;
        if (active != lastReportedState) {
            lastReportedState = active;
            BlindnessMod.LOGGER.info("Blindness Veil pipeline {}",
                    active ? "active — using post-process blackout" : "inactive — using fallback blackout");
        }
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (veilPipelineActive) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        if (!BlindnessClient.CONFIG.enableVisualPostProcessing()) return;

        boolean enabled = BlindnessComponents.PLAYER.maybeGet(client.player)
                .map(component -> component.blindnessEnabled()).orElse(false);
        if (!enabled) return;

        int width = client.getWindow().getFramebufferWidth();
        int height = client.getWindow().getFramebufferHeight();
        if (width <= 0 || height <= 0) return;

        reportDiagnostics();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        context.fill(0, 0, width, height, 0xFF000000);

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static void reportDiagnostics() {
        if (diagnosticsReported) return;
        diagnosticsReported = true;

        boolean irisLoaded = FabricLoader.getInstance().isModLoaded("iris");
        boolean sodiumLoaded = FabricLoader.getInstance().isModLoaded("sodium");
        BlindnessMod.LOGGER.info(
                "Blindness rendering diagnostics: iris={} sodium={} fallback_blackout=true",
                irisLoaded, sodiumLoaded);
    }

    static void resetDiagnostics() {
        diagnosticsReported = false;
        lastReportedState = false;
    }
}
