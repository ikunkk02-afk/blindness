package com.ikunkk02afk.blindness.client.sound;

import com.mojang.blaze3d.systems.RenderSystem;
import com.ikunkk02afk.blindness.BlindnessMod;
import com.ikunkk02afk.blindness.awareness.SoundEchoProjectionRules;
import com.ikunkk02afk.blindness.client.BlindnessClient;
import com.ikunkk02afk.blindness.component.BlindnessComponents;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import foundry.veil.fabric.event.FabricVeilRenderLevelStageEvent;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.IdentityHashMap;
import java.util.Map;

public final class SoundEchoMarkerRenderer {
    private static Matrix4f viewMatrix;
    private static Matrix4f projectionMatrix;
    private static Vec3d cameraPosition;
    private static Vector3f cameraRight;
    private static Vector3f cameraUp;
    private static Vector3f cameraForward;
    private static final Map<SoundEchoMarker, SoundEchoProjectionRules.ProjectionState> DEBUGGED_MODES =
            new IdentityHashMap<>();

    private SoundEchoMarkerRenderer() {}

    public static void register() {
        FabricVeilRenderLevelStageEvent.EVENT.register((stage, renderer, buffers, matrices, view, projection,
                                                        renderTick, deltaTracker, camera, frustum) -> {
            if (stage == VeilRenderLevelStageEvent.Stage.AFTER_LEVEL) capture(camera, view, projection);
        });
        HudRenderCallback.EVENT.register((context, tickCounter) -> render(context));
    }

    private static void capture(Camera camera, Matrix4fc view, Matrix4fc projection) {
        cameraPosition = camera.getPos();
        viewMatrix = new Matrix4f(view);
        projectionMatrix = new Matrix4f(projection);
        cameraRight = new Vector3f(1F, 0F, 0F).rotate(camera.getRotation());
        cameraUp = new Vector3f(0F, 1F, 0F).rotate(camera.getRotation());
        cameraForward = new Vector3f(0F, 0F, -1F).rotate(camera.getRotation());
    }

    private static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || viewMatrix == null || projectionMatrix == null
                || cameraRight == null || cameraUp == null || cameraForward == null
                || !BlindnessClient.CONFIG.entitySoundEchoEnabled()
                || !BlindnessComponents.PLAYER.maybeGet(client.player)
                .map(component -> component.blindnessEnabled()).orElse(false)) return;
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        long now = System.nanoTime();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (SoundEchoMarker marker : SoundEchoMarkerManager.snapshot()) {
            float alpha = SoundEchoMarkerManager.alpha(marker, now);
            if (alpha <= 0.01F) continue;
            Projected projected = project(marker, width, height);
            debugProjection(marker, projected);
            if (projected.mode() == SoundEchoProjectionRules.ProjectionState.REJECTED) continue;
            if (projected.mode() != SoundEchoProjectionRules.ProjectionState.ON_SCREEN
                    && !BlindnessClient.CONFIG.showOffscreenSoundEchoes()) continue;
            drawEcho(context, marker, projected, alpha, now, width, height);
        }
        RenderSystem.disableBlend();
    }

    private static Projected project(SoundEchoMarker marker, int width, int height) {
        Vec3d world = marker.position();
        Vec3d relative = world.subtract(cameraPosition);
        if (!Double.isFinite(relative.x) || !Double.isFinite(relative.y) || !Double.isFinite(relative.z)) {
            return Projected.rejected();
        }
        Vector4f clip = new Vector4f((float) relative.x, (float) relative.y, (float) relative.z, 1F);
        new Matrix4f(projectionMatrix).mul(viewMatrix).transform(clip);
        SoundEchoProjectionRules.ProjectionState mode = SoundEchoProjectionRules.classify(
                clip.x, clip.y, clip.z, clip.w);
        if (mode == SoundEchoProjectionRules.ProjectionState.REJECTED) return Projected.rejected();
        if (mode == SoundEchoProjectionRules.ProjectionState.BEHIND_CAMERA) {
            return edgeFromCameraDirection(relative, marker.seed(), width, height, mode);
        }

        // Perspective division is only legal after the positive-W check above.
        float divisor = clip.w;
        float ndcX = clip.x / divisor;
        float ndcY = clip.y / divisor;
        if (mode == SoundEchoProjectionRules.ProjectionState.ON_SCREEN) {
            float x = (ndcX * 0.5F + 0.5F) * width;
            float y = (0.5F - ndcY * 0.5F) * height;
            return finite(x, y) ? new Projected(x, y, mode) : Projected.rejected();
        }
        if (Float.isFinite(ndcX) && Float.isFinite(ndcY)
                && (Math.abs(ndcX) > 1F || Math.abs(ndcY) > 1F)) {
            return edgeFromScreenDirection(ndcX, -ndcY, width, height, mode);
        }
        return edgeFromCameraDirection(relative, marker.seed(), width, height, mode);
    }

    private static Projected edgeFromCameraDirection(Vec3d relative, int seed, int width, int height,
                                                       SoundEchoProjectionRules.ProjectionState mode) {
        Vector3f direction = new Vector3f((float) relative.x, (float) relative.y, (float) relative.z);
        if (!Float.isFinite(direction.lengthSquared()) || direction.lengthSquared() < 1.0E-8F) {
            return Projected.rejected();
        }
        direction.normalize();
        float localX = direction.dot(cameraRight);
        float localY = direction.dot(cameraUp);
        float localZ = direction.dot(cameraForward);
        if (!finite(localX, localY) || !Float.isFinite(localZ)) return Projected.rejected();
        if (mode == SoundEchoProjectionRules.ProjectionState.BEHIND_CAMERA
                && Math.abs(localX) < 0.05F && Math.abs(localY) < 0.05F) {
            localX = (seed & 1) == 0 ? -0.35F : 0.35F;
        }
        return edgeFromScreenDirection(localX, -localY, width, height, mode);
    }

    private static Projected edgeFromScreenDirection(float xDirection, float yDirection, int width, int height,
                                                       SoundEchoProjectionRules.ProjectionState mode) {
        if (!finite(xDirection, yDirection)) return Projected.rejected();
        float length = (float) Math.hypot(xDirection, yDirection);
        if (length < 1.0E-5F) return Projected.rejected();
        xDirection /= length;
        yDirection /= length;
        float margin = 28F;
        float halfWidth = Math.max(1F, width * 0.5F - margin);
        float halfHeight = Math.max(1F, height * 0.5F - margin);
        float scaleX = Math.abs(xDirection) < 1.0E-5F ? Float.POSITIVE_INFINITY : halfWidth / Math.abs(xDirection);
        float scaleY = Math.abs(yDirection) < 1.0E-5F ? Float.POSITIVE_INFINITY : halfHeight / Math.abs(yDirection);
        float scale = Math.min(scaleX, scaleY);
        float x = width * 0.5F + xDirection * scale;
        float y = height * 0.5F + yDirection * scale;
        return finite(x, y) ? new Projected(x, y, mode) : Projected.rejected();
    }

    private static boolean finite(float first, float second) {
        return Float.isFinite(first) && Float.isFinite(second);
    }

    private static void debugProjection(SoundEchoMarker marker, Projected projected) {
        if (!BlindnessClient.CONFIG.debugSoundEchoes()) return;
        SoundEchoProjectionRules.ProjectionState previous = DEBUGGED_MODES.put(marker, projected.mode());
        if (previous == projected.mode()) return;
        BlindnessMod.LOGGER.info("Sound echo projection category={} position={} clipState={} markerMode={}",
                marker.category(), marker.position(), projected.mode(), projected.mode());
    }

    public static void clearProjectionState() {
        viewMatrix = null;
        projectionMatrix = null;
        cameraPosition = null;
        cameraRight = null;
        cameraUp = null;
        cameraForward = null;
        DEBUGGED_MODES.clear();
    }

    private static void drawEcho(DrawContext context, SoundEchoMarker marker, Projected projected,
                                 float alpha, long now, int width, int height) {
        double age = (now - marker.startedNanos()) / 1_000_000_000.0;
        float baseSize = (float) BlindnessClient.CONFIG.soundEchoSize() * (11F + marker.strength() * 9F);
        float jitterScale = switch (marker.occlusion()) {
            case CLEAR -> 0.35F;
            case PARTIAL -> 1.4F;
            case OCCLUDED -> 2.8F * (float) BlindnessClient.CONFIG.occludedSoundEchoBlurStrength();
        };
        float jitterX = (float) Math.sin(age * 27.0 + marker.seed()) * jitterScale;
        float jitterY = (float) Math.cos(age * 23.0 + marker.seed() * 0.5) * jitterScale;
        int cx = Math.round(projected.x() + jitterX);
        int cy = Math.round(projected.y() + jitterY);
        int rgb = marker.hostile() ? 0xFFF0A8 : 0xB8F7FF;
        int rings = marker.hostile() ? 4 : 3;
        int brokenModulo = marker.occlusion() == com.ikunkk02afk.blindness.awareness.SoundOcclusion.OCCLUDED ? 3 : 7;
        for (int ring = 0; ring < rings; ring++) {
            float phase = (float) ((age * (marker.hostile() ? 1.8 : 1.25) + ring / (double) rings) % 1.0);
            float radius = baseSize * (0.45F + phase * 1.15F);
            float ringAlpha = alpha * (1F - phase) * (0.78F - ring * 0.08F);
            drawBrokenRing(context, cx, cy, radius, color(rgb, ringAlpha), brokenModulo, marker.seed() + ring);
        }
        int coreColor = color(rgb, Math.min(1F, alpha * (0.85F + marker.pulses() * 0.05F)));
        context.fill(cx - 1, cy - 1, cx + 2, cy + 2, coreColor);
        int waveHalf = Math.max(5, Math.round(baseSize * 0.55F));
        for (int x = -waveHalf; x < waveHalf; x++) {
            int y = Math.round((float) Math.sin((x + age * 35.0) * 0.55) * (2F + marker.strength() * 3F));
            if ((x + marker.seed()) % brokenModulo != 0) context.fill(cx + x, cy + y, cx + x + 1, cy + y + 1, coreColor);
        }
    }

    private static void drawBrokenRing(DrawContext context, int cx, int cy, float radius,
                                       int color, int brokenModulo, int seed) {
        int segments = Math.max(24, Math.round(radius * 2.5F));
        for (int i = 0; i < segments; i++) {
            if (Math.floorMod(i + seed, brokenModulo) == 0) continue;
            double angle = Math.PI * 2.0 * i / segments;
            int x = Math.round(cx + (float) Math.cos(angle) * radius);
            int y = Math.round(cy + (float) Math.sin(angle) * radius * 0.58F);
            context.fill(x, y, x + 1, y + 1, color);
        }
    }

    private static int color(int rgb, float alpha) {
        return (Math.clamp(Math.round(alpha * 255F), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    private record Projected(float x, float y, SoundEchoProjectionRules.ProjectionState mode) {
        private static Projected rejected() {
            return new Projected(0F, 0F, SoundEchoProjectionRules.ProjectionState.REJECTED);
        }
    }
}
