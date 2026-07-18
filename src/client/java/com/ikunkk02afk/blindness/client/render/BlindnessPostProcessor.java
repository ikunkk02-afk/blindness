package com.ikunkk02afk.blindness.client.render;

import com.ikunkk02afk.blindness.BlindnessMod;
import com.ikunkk02afk.blindness.client.BlindnessClient;
import com.ikunkk02afk.blindness.client.ClientBlindnessState;
import com.ikunkk02afk.blindness.component.BlindnessComponents;
import com.ikunkk02afk.blindness.scan.ScanWaveMath;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.compat.IrisCompat;
import foundry.veil.platform.VeilEventPlatform;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.List;

public final class BlindnessPostProcessor {
    private static final Identifier FULL_PIPELINE = BlindnessMod.id("blindness");
    private static final Identifier DEPTH_PIPELINE = BlindnessMod.id("blindness_depth");
    private static Identifier activePipeline;
    private static boolean loggedFailure;
    private static final String[] HIT_UNIFORMS = {"ScanHit0", "ScanHit1", "ScanHit2", "ScanHit3"};
    private static final int[] SELECTED_HITS = new int[HIT_UNIFORMS.length];

    private BlindnessPostProcessor() {}

    public static void register() {
        VeilEventPlatform.INSTANCE.onFreeNativeResources(BlindnessPostProcessor::cleanup);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> cleanup());
    }

    public static void tick(MinecraftClient client) {
        boolean enabled = client.player != null && client.world != null
                && !(client.currentScreen instanceof GameMenuScreen)
                && BlindnessClient.CONFIG.enableVisualPostProcessing()
                && BlindnessComponents.PLAYER.maybeGet(client.player).map(component -> component.blindnessEnabled()).orElse(true);
        if (!enabled) {
            deactivate();
            return;
        }

        Identifier desired = IrisCompat.isLoaded() && IrisCompat.INSTANCE.areShadersLoaded() ? DEPTH_PIPELINE : FULL_PIPELINE;
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
                BlindnessMod.LOGGER.warn("Veil blindness pipeline is not available yet; rendering will remain safely disabled until resources load");
            }
            return;
        }
        loggedFailure = false;
        updateUniforms(client, pipeline);
    }

    private static void updateUniforms(MinecraftClient client, PostPipeline pipeline) {
        pipeline.getUniformSafe("BaseBrightness").setFloat((float) BlindnessClient.CONFIG.baseBrightness());
        pipeline.getUniformSafe("BlurStrength").setFloat((float) BlindnessClient.CONFIG.blurStrength());
        pipeline.getUniformSafe("Saturation").setFloat((float) BlindnessClient.CONFIG.saturation());
        pipeline.getUniformSafe("OutlineThickness").setFloat((float) BlindnessClient.CONFIG.outlineThickness());
        pipeline.getUniformSafe("GlowStrength").setFloat((float) BlindnessClient.CONFIG.outlineBrightness());

        Vec3d camera = client.gameRenderer.getCamera().getPos();
        ClientBlindnessState.ScanWave wave = ClientBlindnessState.activeWave();
        if (wave != null) {
            Vec3d local = wave.origin().subtract(camera);
            float progress = wave.progress();
            float radius = Math.min(wave.maxRadius(), Math.max(0F, wave.elapsedSeconds()) * (float) BlindnessClient.CONFIG.waveSpeed());
            float fade = ScanWaveMath.fade(progress);
            pipeline.getUniformSafe("ScanOrigin").setVector((float) local.x, (float) local.y, (float) local.z);
            pipeline.getUniformSafe("ScanRadius").setFloat(radius);
            pipeline.getUniformSafe("ScanProgress").setFloat(progress);
            pipeline.getUniformSafe("FadeProgress").setFloat(Math.clamp(fade, 0F, 1F));
            pipeline.getUniformSafe("ScanMode").setInt(wave.mode());
            pipeline.getUniformSafe("ScanActive").setInt(1);
            updateAuthorizedFoci(pipeline, wave, camera, radius);
        } else {
            pipeline.getUniformSafe("ScanActive").setInt(0);
            pipeline.getUniformSafe("ScanHitCount").setInt(0);
            pipeline.getUniformSafe("FadeProgress").setFloat(0F);
        }

        ClientBlindnessState.CreaturePulse pulse = ClientBlindnessState.activeCreaturePulse();
        if (pulse != null) {
            pipeline.getUniformSafe("CreatureOrigin").setVector((float) (pulse.x() - camera.x), (float) (pulse.y() - camera.y), (float) (pulse.z() - camera.z));
            pipeline.getUniformSafe("CreatureStrength").setFloat(pulse.strength());
        } else {
            pipeline.getUniformSafe("CreatureStrength").setFloat(0F);
        }
    }

    private static void updateAuthorizedFoci(PostPipeline pipeline, ClientBlindnessState.ScanWave wave,
                                               Vec3d camera, float radius) {
        List<Vec3d> surfaces = ClientBlindnessState.authorizedSurfaces(wave.scanId());
        if (wave.mode() == 0 || surfaces.isEmpty()) {
            pipeline.getUniformSafe("ScanHitCount").setInt(0);
            return;
        }
        Arrays.fill(SELECTED_HITS, -1);
        int count = Math.min(HIT_UNIFORMS.length, surfaces.size());
        for (int slot = 0; slot < count; slot++) {
            int bestIndex = -1;
            double bestScore = Double.POSITIVE_INFINITY;
            for (int index = 0; index < surfaces.size(); index++) {
                boolean used = false;
                for (int previous = 0; previous < slot; previous++) {
                    if (SELECTED_HITS[previous] == index) { used = true; break; }
                }
                if (used) continue;
                Vec3d surface = surfaces.get(index);
                double frontierError = Math.abs(surface.distanceTo(wave.origin()) - radius);
                double score = frontierError + surface.squaredDistanceTo(camera) * 0.001;
                if (score < bestScore) {
                    bestScore = score;
                    bestIndex = index;
                }
            }
            SELECTED_HITS[slot] = bestIndex;
            Vec3d local = surfaces.get(bestIndex).subtract(camera);
            pipeline.getUniformSafe(HIT_UNIFORMS[slot]).setVector((float) local.x, (float) local.y, (float) local.z);
        }
        pipeline.getUniformSafe("ScanHitCount").setInt(count);
    }

    public static void cleanup() {
        deactivate();
        ClientBlindnessState.clear();
    }

    private static void deactivate() {
        if (activePipeline == null) return;
        try {
            VeilRenderSystem.renderer().getPostProcessingManager().remove(activePipeline);
        } catch (Throwable throwable) {
            if (!loggedFailure) BlindnessMod.LOGGER.warn("Failed to release Veil blindness pipeline cleanly", throwable);
        }
        activePipeline = null;
    }
}
