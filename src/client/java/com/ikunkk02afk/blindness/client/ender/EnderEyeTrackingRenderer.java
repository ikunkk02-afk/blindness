package com.ikunkk02afk.blindness.client.ender;

import com.ikunkk02afk.blindness.client.BlindnessClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

/**
 * Renders the Ender Eye tracking marker — a world-space ring + label when the entity
 * is on-screen, or a directional edge arrow when it's off-screen.
 */
public final class EnderEyeTrackingRenderer {

    // ── Visual constants ──

    /** Color for the world-space ring (green-cyan). */
    private static final int MARKER_COLOR = 0xFF_55FFAA;
    /** Color for the edge arrow. */
    private static final int ARROW_COLOR = 0xFF_55FFAA;

    /** Radius of the world-space ring in screen pixels (scaled with GUI scale). */
    private static final float RING_RADIUS = 20.0F;
    /** Safe margin from screen edges (pixels, before GUI scale). */
    private static final float EDGE_MARGIN = 28.0F;

    /** Minimum screen size for the marker (prevents it becoming invisible at distance). */
    private static final float MIN_MARKER_SCREEN_SIZE = 8.0F;

    private EnderEyeTrackingRenderer() {}

    // ── Main render entry point (called from HudRenderCallback) ──

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!EnderEyeTrackerClient.isActive()) return;
        if (!BlindnessClient.CONFIG.enableEnderEyeTrackingMarker()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        if (!BlindnessClient.CONFIG.enableEnderEyeWorldMarker()
                && !BlindnessClient.CONFIG.enableEnderEyeEdgeArrow()) return;

        float tickDelta = tickCounter.getTickDelta(true);
        Vec3d targetPos = EnderEyeTrackerClient.getInterpolatedPosition(tickDelta);
        if (targetPos == null) return;

        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();

        int windowWidth = client.getWindow().getScaledWidth();
        int windowHeight = client.getWindow().getScaledHeight();
        double guiScale = client.getWindow().getScaleFactor();

        // Build projection matrix.
        double fov = client.options.getFov().getValue();
        float aspect = (float) windowWidth / (float) windowHeight;
        Matrix4f projMatrix = new Matrix4f().perspective(
                (float) Math.toRadians(fov), aspect, 0.05F, 4096.0F);

        // Build view matrix (camera rotation).
        Quaternionf rotation = camera.getRotation();
        Matrix4f viewMatrix = new Matrix4f().rotate(rotation);
        viewMatrix.translate(
                -(float) cameraPos.x,
                -(float) cameraPos.y,
                -(float) cameraPos.z);

        // Combined view-projection.
        Matrix4f viewProj = new Matrix4f(projMatrix).mul(viewMatrix);

        // Target in clip space.
        Vector4f worldVec = new Vector4f(
                (float) targetPos.x,
                (float) targetPos.y,
                (float) targetPos.z,
                1.0F);
        worldVec.mul(viewProj);

        // Behind camera check.
        if (worldVec.w <= 0.001F) {
            renderEdgeArrow(context, client, camera, targetPos, windowWidth, windowHeight, tickCounter);
            return;
        }

        // Normalized device coordinates.
        float ndcX = worldVec.x / worldVec.w;
        float ndcY = worldVec.y / worldVec.w;
        // NDC Z is 0 at near, 1 at far (after perspective divide in OpenGL). But we
        // don't need Z beyond the w check above.

        // Convert to screen coordinates (Y-up NDC → Y-down screen).
        float screenX = (ndcX * 0.5F + 0.5F) * windowWidth;
        float screenY = (-ndcY * 0.5F + 0.5F) * windowHeight;

        // Check if on-screen with margin.
        float margin = EDGE_MARGIN * (float) guiScale;
        boolean onScreen = screenX >= margin && screenX <= windowWidth - margin
                && screenY >= margin && screenY <= windowHeight - margin;

        // Clamp NDC to [-1, 1] for distance-based sizing.
        float sizeScale = MathHelper.clamp(1.0F - (Math.abs(ndcX) + Math.abs(ndcY)) * 0.15F, 0.25F, 1.0F);
        float ringRadius = Math.max(RING_RADIUS * sizeScale, MIN_MARKER_SCREEN_SIZE);

        if (onScreen && BlindnessClient.CONFIG.enableEnderEyeWorldMarker()) {
            renderWorldMarker(context, client, screenX, screenY, ringRadius, tickCounter);
        } else if (BlindnessClient.CONFIG.enableEnderEyeEdgeArrow()) {
            renderEdgeArrow(context, client, camera, targetPos, windowWidth, windowHeight, tickCounter);
        }
    }

    // ── World-space marker (on-screen) ──

    private static void renderWorldMarker(DrawContext context, MinecraftClient client,
                                           float cx, float cy, float radius,
                                           RenderTickCounter tickCounter) {
        TextRenderer textRenderer = client.textRenderer;
        float tick = tickCounter.getTickDelta(false) + client.world.getTime();

        // Breathing animation.
        float breathe = 1.0F + 0.06F * (float) Math.sin(tick * 0.15);
        float ringR = radius * breathe;

        // Diffusion pulse ring (outer).
        float pulsePhase = tick * 0.1F;
        float pulseAlpha = 0.4F + 0.3F * (float) Math.sin(pulsePhase);
        int pulseColor = withAlpha(MARKER_COLOR, (int) (pulseAlpha * 255));
        float pulseR = ringR * (1.3F + 0.1F * (float) Math.sin(pulsePhase));

        // Draw outer pulse ring.
        drawRing(context, cx, cy, pulseR, pulseColor, 1.8F);

        // Draw main ring.
        drawRing(context, cx, cy, ringR, MARKER_COLOR, 2.5F);

        // Draw small diamond/eye icon in center.
        drawEyeIcon(context, cx, cy, ringR * 0.35F, tick);

        // Label text.
        Text label;
        if (EnderEyeTrackerClient.isDroppedPhase()) {
            label = Text.translatable("hud.blindness.dropped_ender_eye_marker");
        } else {
            label = Text.translatable("hud.blindness.ender_eye_marker");
        }

        float textY = cy + ringR + 6;
        int textWidth = textRenderer.getWidth(label);
        context.drawText(textRenderer, label,
                (int) (cx - textWidth / 2F), (int) textY, 0xFF_55FFAA, true);

        // Distance text.
        if (BlindnessClient.CONFIG.showEnderEyeDistance()) {
            int dist = EnderEyeTrackerClient.getDistance(client);
            Text distText = Text.translatable("hud.blindness.ender_eye_distance", dist);
            int distWidth = textRenderer.getWidth(distText);
            context.drawText(textRenderer, distText,
                    (int) (cx - distWidth / 2F), (int) (textY + 12), 0xFF_55FFAA, true);
        }
    }

    // ── Edge arrow (off-screen) ──

    private static void renderEdgeArrow(DrawContext context, MinecraftClient client,
                                         Camera camera, Vec3d targetPos,
                                         int windowWidth, int windowHeight,
                                         RenderTickCounter tickCounter) {
        TextRenderer textRenderer = client.textRenderer;
        float margin = EDGE_MARGIN * (float) client.getWindow().getScaleFactor();

        // Compute direction from camera to target in screen space.
        Vec3d cameraPos = camera.getPos();
        Vec3d toTarget = targetPos.subtract(cameraPos);

        double dx = toTarget.x;
        double dy = toTarget.y;
        double dz = toTarget.z;
        double hDist = Math.sqrt(dx * dx + dz * dz);

        // Angle from camera forward to target (horizontal).
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float playerYaw = camera.getYaw();
        float relYaw = MathHelper.wrapDegrees(targetYaw - playerYaw);

        // Angle from camera forward to target (vertical).
        float targetPitch = hDist > 0.001
                ? (float) Math.toDegrees(Math.atan2(dy, hDist))
                : (dy > 0 ? 90F : -90F);
        float playerPitch = camera.getPitch();
        float relPitch = MathHelper.wrapDegrees(targetPitch - playerPitch);

        // Convert relative angles to screen-space direction vector.
        // A positive relYaw means target is to the right.
        // A positive relPitch means target is above.
        float halfFovX = (float) Math.toDegrees(Math.atan(
                Math.tan(Math.toRadians(client.options.getFov().getValue() / 2.0))
                        * (windowWidth / (float) windowHeight)));

        // Map angle to normalized screen coordinate.
        float normX = MathHelper.clamp(relYaw / halfFovX, -1.0F, 1.0F);
        float halfFovY = (float) client.options.getFov().getValue() / 2.0F;
        float normY = MathHelper.clamp(-relPitch / halfFovY, -1.0F, 1.0F);

        // Clamp to screen edge rectangle.
        float arrowX, arrowY;
        float screenCenterX = windowWidth / 2F;
        float screenCenterY = windowHeight / 2F;
        float availableX = screenCenterX - margin;
        float availableY = screenCenterY - margin;

        // Intersect the direction ray with the safe rectangle boundary.
        if (Math.abs(normX) * availableY > Math.abs(normY) * availableX) {
            // Hits left/right edge.
            float signX = Math.signum(normX);
            if (signX == 0) signX = 1;
            arrowX = screenCenterX + signX * availableX;
            arrowY = screenCenterY + (normY / Math.abs(normX)) * availableX;
            arrowY = MathHelper.clamp(arrowY, margin, windowHeight - margin);
        } else if (Math.abs(normY) > 0.001F) {
            // Hits top/bottom edge.
            float signY = Math.signum(normY);
            if (signY == 0) signY = 1;
            arrowY = screenCenterY + signY * availableY;
            arrowX = screenCenterX + (normX / Math.abs(normY)) * availableY;
            arrowX = MathHelper.clamp(arrowX, margin, windowWidth - margin);
        } else {
            // Center — shouldn't happen for off-screen.
            arrowX = screenCenterX;
            arrowY = screenCenterY;
        }

        // Determine arrow direction character and height indicator.
        float tick = tickCounter.getTickDelta(false) + client.world.getTime();
        String arrowChar = getArrowChar(relYaw, toTarget.y);
        String heightSuffix = getHeightSuffix(toTarget.y, hDist);

        // Pulsing alpha.
        int alpha = 180 + (int) (50 * Math.sin(tick * 0.2));
        int arrowColor = withAlpha(ARROW_COLOR, MathHelper.clamp(alpha, 120, 255));

        // Draw arrow.
        Text arrowText = Text.literal(arrowChar + " ");
        int arrowWidth = textRenderer.getWidth(arrowText);
        context.drawText(textRenderer, arrowText,
                (int) (arrowX - arrowWidth / 2F), (int) (arrowY - 9), arrowColor, false);

        // Draw label.
        Text label;
        String labelKey = EnderEyeTrackerClient.isDroppedPhase()
                ? "hud.blindness.dropped_ender_eye_marker"
                : "hud.blindness.ender_eye_marker";
        int dist = EnderEyeTrackerClient.getDistance(client);
        String fullLabel = Text.translatable(labelKey).getString();
        if (BlindnessClient.CONFIG.showEnderEyeDistance() && !heightSuffix.isEmpty()) {
            fullLabel += " " + dist + "m";
        } else if (BlindnessClient.CONFIG.showEnderEyeDistance()) {
            fullLabel += " " + dist + "m";
        }

        if (!heightSuffix.isEmpty()) {
            fullLabel += " " + heightSuffix;
        }

        Text labelText = Text.literal(fullLabel);
        int labelWidth = textRenderer.getWidth(labelText);
        // Draw below the arrow.
        context.drawText(textRenderer, labelText,
                (int) (arrowX - labelWidth / 2F), (int) (arrowY + 4), arrowColor, false);
    }

    // ── Helpers ──

    private static String getArrowChar(float relYaw, double dy) {
        // Map relative yaw to arrow direction.
        float absYaw = Math.abs(relYaw);
        if (absYaw < 15) return "↑";
        if (absYaw < 60) return relYaw > 0 ? "↗" : "↖";
        if (absYaw < 120) return relYaw > 0 ? "→" : "←";
        if (absYaw < 165) return relYaw > 0 ? "↘" : "↙";
        return "↓";
    }

    private static String getHeightSuffix(double dy, double hDist) {
        double ratio = hDist > 0.5 ? Math.abs(dy) / hDist : Math.abs(dy);
        if (dy > 1.5 && ratio > 0.5) return "▲";
        if (dy < -1.5 && ratio > 0.5) return "▼";
        return "";
    }

    /** Draws a hollow ring at (cx, cy) with given radius and color. */
    private static void drawRing(DrawContext context, float cx, float cy,
                                  float radius, int color, float thickness) {
        int segments = 32;
        float prevX = cx + radius;
        float prevY = cy;
        for (int i = 1; i <= segments; i++) {
            float angle = (float) (i * 2.0 * Math.PI / segments);
            float x = cx + (float) Math.cos(angle) * radius;
            float y = cy + (float) Math.sin(angle) * radius;
            drawLine(context, prevX, prevY, x, y, color, thickness);
            prevX = x;
            prevY = y;
        }
    }

    /** Draws a small diamond/eye icon at (cx, cy). */
    private static void drawEyeIcon(DrawContext context, float cx, float cy,
                                     float size, float tick) {
        float alpha = 0.7F + 0.2F * (float) Math.sin(tick * 0.2);
        int color = withAlpha(MARKER_COLOR, (int) (alpha * 255));
        float half = size * 0.7F;

        // Diamond shape.
        drawLine(context, cx, cy - half, cx + half, cy, color, 2.0F);
        drawLine(context, cx + half, cy, cx, cy + half, color, 2.0F);
        drawLine(context, cx, cy + half, cx - half, cy, color, 2.0F);
        drawLine(context, cx - half, cy, cx, cy - half, color, 2.0F);

        // Center dot.
        drawLine(context, cx - 1, cy, cx + 1, cy, color, 2.0F);
    }

    /** Draws a thick line using filled rectangles (HUD-safe, no 3D state). */
    private static void drawLine(DrawContext context, float x1, float y1,
                                  float x2, float y2, int color, float thickness) {
        // Use DrawContext's fill-based approach for anti-aliased-like lines.
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.5F) {
            context.fill((int) x1, (int) y1, (int) x1 + 1, (int) y1 + 1, color);
            return;
        }

        float nx = -dy / len * thickness * 0.5F;
        float ny = dx / len * thickness * 0.5F;

        // Draw as a quad (two triangles approximated by fill calls).
        // For simplicity, draw multiple horizontal/vertical fills.
        int steps = Math.max(1, (int) len);
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            float px = x1 + dx * t;
            float py = y1 + dy * t;
            context.fill((int) (px - thickness * 0.5F), (int) (py - thickness * 0.5F),
                    (int) (px + thickness * 0.5F + 1), (int) (py + thickness * 0.5F + 1), color);
        }
    }

    /** Helper to set alpha on an ARGB color. */
    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (MathHelper.clamp(alpha, 0, 255) << 24);
    }
}
