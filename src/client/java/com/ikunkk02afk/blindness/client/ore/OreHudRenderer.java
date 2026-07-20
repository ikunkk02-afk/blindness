package com.ikunkk02afk.blindness.client.ore;

import com.ikunkk02afk.blindness.accessibility.DirectionHelper;
import com.ikunkk02afk.blindness.accessibility.OreType;
import com.ikunkk02afk.blindness.client.BlindnessClient;
import com.ikunkk02afk.blindness.network.BlindnessPayloads;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * Renders an ore detection HUD overlay below the crosshair.
 * Merges same-type ores and shows up to 2 nearest groups.
 */
public final class OreHudRenderer {
    private static final long DISPLAY_DURATION_NANOS = 3_000_000_000L;
    private static long showUntilNanos;
    private static GroupedOreResult group1;
    private static GroupedOreResult group2;

    private OreHudRenderer() {}

    public static void show(BlockPos center, List<BlindnessPayloads.OreDetected> ores) {
        if (!BlindnessClient.CONFIG.enableOreHud()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // Group by type
        Map<OreType, GroupedOreResult> grouped = new LinkedHashMap<>();
        for (BlindnessPayloads.OreDetected ore : ores) {
            OreType type = OreType.values()[Math.clamp(ore.oreType(), 0, OreType.values().length - 1)];
            Vec3d orePos = Vec3d.ofCenter(ore.pos());
            double distSq = client.player.getEyePos().squaredDistanceTo(orePos);

            GroupedOreResult existing = grouped.get(type);
            if (existing == null) {
                grouped.put(type, new GroupedOreResult(type, 1, distSq, orePos));
            } else {
                grouped.put(type, existing.withOneMore(distSq, orePos));
            }
        }

        // Sort by nearest distance, take top 2
        List<GroupedOreResult> sorted = new ArrayList<>(grouped.values());
        sorted.sort(Comparator.comparingDouble(g -> g.nearestDistSq));

        group1 = sorted.size() > 0 ? sorted.get(0) : null;
        group2 = sorted.size() > 1 ? sorted.get(1) : null;
        showUntilNanos = System.nanoTime() + DISPLAY_DURATION_NANOS;
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        long now = System.nanoTime();
        if (now > showUntilNanos) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        float alpha = 1F;
        long remaining = showUntilNanos - now;
        if (remaining < 400_000_000L) {
            alpha = (float) remaining / 400_000_000L;
        }
        if (alpha <= 0) return;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int centerX = screenWidth / 2;
        int baseY = screenHeight / 2 + 25;

        renderGroup(context, client, group1, centerX, baseY, alpha);
        if (group2 != null) {
            renderGroup(context, client, group2, centerX, baseY + 12, alpha);
        }
    }

    private static void renderGroup(DrawContext context, MinecraftClient client,
                                    GroupedOreResult group, int centerX, int y, float alpha) {
        if (group == null || client.player == null) return;

        OreType type = group.type;
        Text displayName = OreType.displayName(type, client.world.getBlockState(
                BlockPos.ofFloored(group.nearestPos)));

        DirectionHelper.Direction8 dir = DirectionHelper.horizontalDirection(client.player, group.nearestPos);
        DirectionHelper.VerticalBias vert = DirectionHelper.verticalBias(client.player, group.nearestPos);
        int dist = DirectionHelper.roundedDistance(client.player, group.nearestPos);

        String dirKey;
        if (vert != DirectionHelper.VerticalBias.NONE) {
            dirKey = vert.translationKey();
        } else {
            dirKey = dir.translationKey();
        }

        Text line = Text.translatable("hud.blindness.ore_entry",
                displayName, group.count,
                Text.translatable(dirKey), dist);

        int color = (Math.round(alpha * 255F) << 24) | 0xFFFFFF;
        int textWidth = client.textRenderer.getWidth(line);
        context.drawText(client.textRenderer, line, centerX - textWidth / 2, y, color, true);
    }

    public static void clear() {
        showUntilNanos = 0;
        group1 = null;
        group2 = null;
    }

    private static final class GroupedOreResult {
        final OreType type;
        final int count;
        final double nearestDistSq;
        final Vec3d nearestPos;

        GroupedOreResult(OreType type, int count, double distSq, Vec3d pos) {
            this.type = type;
            this.count = count;
            this.nearestDistSq = distSq;
            this.nearestPos = pos;
        }

        GroupedOreResult withOneMore(double otherDistSq, Vec3d otherPos) {
            if (otherDistSq < nearestDistSq) {
                return new GroupedOreResult(type, count + 1, otherDistSq, otherPos);
            }
            return new GroupedOreResult(type, count + 1, nearestDistSq, nearestPos);
        }
    }
}
