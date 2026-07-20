package com.ikunkk02afk.blindness.accessibility;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Unified direction calculation for accessibility hints.
 * Computes an 8-direction horizontal heading plus vertical bias
 * from the player's position and look direction to a world point.
 */
public final class DirectionHelper {
    private DirectionHelper() {}

    /**
     * Horizontal directions ordered clockwise starting from south (forward=0°).
     */
    public enum Direction8 {
        FRONT, FRONT_RIGHT, RIGHT, BACK_RIGHT,
        BACK, BACK_LEFT, LEFT, FRONT_LEFT;

        public String translationKey() {
            return "direction.blindness." + name().toLowerCase();
        }
    }

    /**
     * Calculates the horizontal direction from the player to the target,
     * biased by the player's current look yaw.
     */
    public static Direction8 horizontalDirection(PlayerEntity player, Vec3d target) {
        Vec3d playerPos = player.getPos();
        double dx = target.x - playerPos.x;
        double dz = target.z - playerPos.z;
        double horizontalDistSq = dx * dx + dz * dz;
        if (horizontalDistSq < 0.01) return Direction8.FRONT;

        // Angle of the target relative to the player's look yaw.
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float playerYaw = player.getYaw();
        float relativeYaw = ((targetYaw - playerYaw) % 360F + 360F) % 360F;

        // Map to 8-direction sectors (each sector = 45°).
        int sector = Math.round(relativeYaw / 45F) % 8;
        return switch (sector) {
            case 0 -> Direction8.FRONT;
            case 1 -> Direction8.FRONT_RIGHT;
            case 2 -> Direction8.RIGHT;
            case 3 -> Direction8.BACK_RIGHT;
            case 4 -> Direction8.BACK;
            case 5 -> Direction8.BACK_LEFT;
            case 6 -> Direction8.LEFT;
            case 7 -> Direction8.FRONT_LEFT;
            default -> Direction8.FRONT;
        };
    }

    /**
     * Returns whether the vertical offset dominates the horizontal distance,
     * in which case "above" or "below" should be shown instead of an 8-direction.
     */
    public static VerticalBias verticalBias(PlayerEntity player, Vec3d target) {
        double dy = target.y - player.getEyeY();
        double dx = target.x - player.getPos().x;
        double dz = target.z - player.getPos().z;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        double absDy = Math.abs(dy);
        if (absDy > horizontalDist * 1.5 && absDy > 1.5) {
            return dy > 0 ? VerticalBias.ABOVE : VerticalBias.BELOW;
        }
        return VerticalBias.NONE;
    }

    public enum VerticalBias {
        ABOVE, BELOW, NONE;

        public String translationKey() {
            return "direction.blindness." + name().toLowerCase();
        }
    }

    /**
     * Distance rounded to the nearest integer.
     */
    public static int roundedDistance(PlayerEntity player, Vec3d target) {
        return (int) Math.round(Math.sqrt(player.getEyePos().squaredDistanceTo(target)));
    }
}
