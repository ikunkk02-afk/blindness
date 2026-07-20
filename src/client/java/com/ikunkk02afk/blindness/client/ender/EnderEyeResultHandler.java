package com.ikunkk02afk.blindness.client.ender;

import com.ikunkk02afk.blindness.accessibility.DirectionHelper;
import com.ikunkk02afk.blindness.client.BlindnessClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

/**
 * Handles client-side display of Ender Eye result notifications.
 * Shows an action bar message with direction and distance for drops,
 * or a simple message for shatters.
 */
public final class EnderEyeResultHandler {
    private static final long DISPLAY_DURATION_NANOS = 4_000_000_000L;
    private static long showUntilNanos;
    private static int resultType;
    private static Vec3d position;
    private static String directionKey;
    private static int distance;

    private EnderEyeResultHandler() {}

    /**
     * Called from the networking layer when an EnderEyeResult packet arrives.
     *
     * @param type 0 = dropped, 1 = shattered
     * @param x    world X of the event
     * @param y    world Y
     * @param z    world Z
     */
    public static void handle(int type, double x, double y, double z) {
        if (!BlindnessClient.CONFIG.enableEnderEyeResultHint()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        Vec3d pos = new Vec3d(x, y, z);
        resultType = type;
        position = pos;
        distance = DirectionHelper.roundedDistance(client.player, pos);

        DirectionHelper.Direction8 dir = DirectionHelper.horizontalDirection(client.player, pos);
        DirectionHelper.VerticalBias vert = DirectionHelper.verticalBias(client.player, pos);
        directionKey = vert != DirectionHelper.VerticalBias.NONE ? vert.translationKey() : dir.translationKey();

        showUntilNanos = System.nanoTime() + DISPLAY_DURATION_NANOS;

        // Play sound
        if (type == 0) {
            // Dropped: softer sound
            client.player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(),
                    SoundCategory.PLAYERS, 0.5F, 1.1F);
        } else {
            // Shattered: glass break
            client.player.playSoundToPlayer(SoundEvents.BLOCK_GLASS_BREAK,
                    SoundCategory.PLAYERS, 0.7F, 0.9F);
        }

        // Show action bar
        Text msg;
        if (type == 0) {
            msg = Text.translatable("msg.blindness.ender_eye_dropped",
                    Text.translatable(directionKey), distance);
        } else {
            msg = Text.translatable("msg.blindness.ender_eye_shattered");
        }
        client.player.sendMessage(msg, true);
    }

    public static void clear() {
        showUntilNanos = 0;
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        // Action bar handles text display; this method reserved for future HUD icons.
    }
}
